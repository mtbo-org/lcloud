/* (C) 2025 Vladimir E. (PROGrand) Koltunov (mtbo.org) */

package org.mtbo.lcloud.discovery;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.stream.Stream;
import org.mtbo.lcloud.discovery.logging.FileLineLogger;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Allows to request network services named by some {@link ClientConfig#serviceName serice name}
 *
 * @param <AddressType> address type, ex. {@link java.net.InetAddress InetAddress}
 * @param <SocketType> socket type, ex: {@link java.net.DatagramSocket DatagramSocket}
 * @param <PacketType> socket type, ex: {@link java.net.DatagramPacket DatagramPacket}
 */
public abstract class DiscoveryClient<AddressType, SocketType extends AutoCloseable, PacketType> {

  /** Socket reference counter */
  protected static AtomicInteger clientSocketsCounter = new AtomicInteger(0);

  /** Configuration parameters */
  protected final ClientConfig config;

  final FileLineLogger logger =
      FileLineLogger.getLogger(DiscoveryClient.class.getName(), "CLI >>>  ");
  private final String prefix;

  /**
   * Construct client with config
   *
   * @param config configuration, including {@link ClientConfig#serviceName service name}
   */
  public DiscoveryClient(ClientConfig config) {
    this.config = config;
    this.prefix = "DISCOVERY_RESPONSE " + config.serviceName + " FROM ";
  }

  /**
   * Periodically look for network services named by {@link ClientConfig#serviceName}
   *
   * @param receiveTimeout time interval to check responses from instances
   * @return {@link Publisher} of service instances id's set
   */
  public Flux<HashSet<String>> startLookup(Duration receiveTimeout) {
    Mono<AddressType> mainAddresses = getMainBroadcastAddresses();
    return lookupInternal(receiveTimeout, mainAddresses).repeat();
  }

  /**
   * Lookup services once
   *
   * @param receiveTimeout time interval to check responses from instances
   * @return Single or zero instance {@link Publisher} of service instances id's set
   */
  public Mono<Set<String>> lookupOnce(Duration receiveTimeout) {
    Mono<AddressType> mainAddresses = getMainBroadcastAddresses();

    return Mono.from(lookupInternal(receiveTimeout, mainAddresses));
  }

  /**
   * Internal method reuses cached mainAddresses
   *
   * @param receiveTimeout time interval to check responses from instances
   * @param address cached local broadcast addresses
   * @return Single or zero instance {@link Publisher} of service instances id's set
   */
  private Flux<HashSet<String>> lookupInternal(Duration receiveTimeout, Mono<AddressType> address) {
    return send(address)
        .flatMap(socketType -> receiveOnInterface(receiveTimeout, socketType))
        .bufferTimeout(
            config.endpointsCount,
            receiveTimeout,
            HashSet::new) // Collect distinct names during time-windows
        .map(
            strings -> {
              strings.remove("");
              logger.finer("Discovered " + strings.size() + " endpoints");
              return strings;
            })
        .onErrorResume(
            throwable -> {
              logger.finer("Error on loop", throwable);
              return Mono.empty();
            });
  }

  private Flux<String> receiveOnInterface(Duration receiveTimeout, SocketType socketType) {
    return Flux.using(
            () -> socketType,
            (s) -> receive(s).onErrorResume(throwable -> Mono.empty()),
            (s) -> {
              try {
                s.close();

                if (logger.isLoggable(Level.FINER)) {
                  logger.finer(
                      "CLIENT SOCKET DESTROYED: " + clientSocketsCounter.decrementAndGet());
                }
              } catch (Throwable e) {
                throw new RuntimeException(e);
              }
            })
        .timeout(receiveTimeout)
        .onErrorResume(throwable -> Mono.just("")); // Return empty in case of no reply.
  }

  /**
   * Create socket operator
   *
   * @return socket
   */
  protected abstract Mono<SocketType> createSocket();

  /**
   * Send broadcast via addresses operator
   *
   * @param mainAddress precached addresses. May be appended with additional ones.
   * @return sockets on which sending was performed. Can be used to receive responses.
   */
  private Flux<SocketType> send(Mono<AddressType> mainAddress) {

    Mono<ArrayList<AddressType>> addressesMono =
        mainAddress
            .zipWith(getAdditionalAddresses())
            .map(
                objects -> {
                  var arr = new ArrayList<AddressType>();
                  arr.add(objects.getT1());
                  arr.addAll(objects.getT2().toList());
                  return arr;
                });

    Flux<AddressSocket<AddressType, SocketType>> addressSocketFlux =
        addressesMono
            .doOnNext(
                addresses -> {
                  if (logger.isLoggable(Level.FINER)) {
                    synchronized (FileLineLogger.class) {
                      logger.finer("BROADCAST ADDRESSES (" + addresses.size() + "):");
                      for (var address : addresses) {
                        logger.finer(address.toString());
                      }
                    }
                  }
                })
            .flatMapIterable(addressTypes -> addressTypes)
            .flatMap(
                addressType ->
                    createSocket().map(socketType -> new AddressSocket<>(addressType, socketType)));

    if (logger.isLoggable(Level.FINER)) {
      addressSocketFlux =
          addressSocketFlux.doOnNext(
              socket ->
                  logger.finer(
                      "CLIENT SOCKET CREATED: "
                          + clientSocketsCounter.incrementAndGet()
                          + " - "
                          + socket.address()));
    }

    var sendData =
        ("DISCOVERY_REQUEST " + config.serviceName + " FROM " + config.instanceName).getBytes();

    return addressSocketFlux.flatMap(
        (pair) -> {
          AddressType address = pair.address();
          SocketType socket = pair.socket();

          logger.finer("SEND BROADCAST on " + address);

          var packet = createSendPacket(sendData, address);

          return sendPacket(socket, packet)
              .doOnNext(ignored -> logger.finer("1 1 1 Request packet sent to: " + address))
              .onErrorResume(
                  throwable -> {
                    logger.finer("sendPacket error", throwable);
                    return Mono.empty();
                  });
        });
  }

  /**
   * Send packet operator
   *
   * @param socket on which to send
   * @param sendPacket packet to send
   * @return mono operator
   */
  protected abstract Mono<SocketType> sendPacket(SocketType socket, PacketType sendPacket);

  /**
   * Create packet for send to address
   *
   * @param sendData data bytes
   * @param address address to send
   * @return packet
   */
  protected abstract PacketType createSendPacket(byte[] sendData, AddressType address);

  private Mono<PacketType> receiveResponse(SocketType socket) {

    var receivePacket = createReceivePacket();

    return receivePacket(socket, receivePacket);
  }

  /**
   * Process packer receiving operator
   *
   * @param socket from which packet will be received
   * @param receivePacket packet r/w
   * @return mono operator
   */
  protected abstract Mono<PacketType> receivePacket(SocketType socket, PacketType receivePacket);

  /**
   * Create packet for receive
   *
   * @return packet
   */
  protected abstract PacketType createReceivePacket();

  private Flux<String> receive(SocketType socket) {
    return Flux.from(receiveResponse(socket))
        .repeat()
        .flatMap(
            receivePacket -> {
              var message = packetMessage(receivePacket);

              if (logger.isLoggable(Level.FINER)) {
                logger.finer(
                    "4 4 4 Broadcast response from: "
                        + packetAddress(receivePacket)
                        + ", ["
                        + message
                        + "]");
              }

              if (message.startsWith(prefix)) {
                return Mono.just(message.substring(prefix.length()));
              } else {
                return Mono.empty();
              }
            })
        .onErrorComplete();
  }

  /**
   * Human's readable address name
   *
   * @param packet contains address
   * @return display name
   */
  protected abstract String packetAddress(PacketType packet);

  /**
   * Parse packet for message
   *
   * @param receivedPacket received packet
   * @return parsed message
   */
  protected abstract String packetMessage(PacketType receivedPacket);

  /**
   * Get IPv4 network broadcast address
   *
   * @return cached local broadcast addresses
   */
  protected abstract Mono<AddressType> getMainBroadcastAddresses();

  /**
   * Enumerate network interfaces broadcast addresses able to transmit and receive packets
   *
   * @return addresses Reactive Streams {@link Publisher}
   */
  protected abstract Mono<Stream<AddressType>> getAdditionalAddresses();
}
