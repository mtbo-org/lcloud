/* (C) 2025 Vladimir E. Koltunov (mtbo.org) */

package org.mtbo.lcloud.discovery;

import java.time.Duration;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Allows to request network services named by some {@link ClientConfig#serviceName serice name} */
public abstract class DiscoveryClient<AddressType, SocketType extends AutoCloseable, PacketType> {

  /** Configuration parameters */
  protected final ClientConfig config;

  /**
   * Construct client with config
   *
   * @param config configuration, including {@link ClientConfig#serviceName service name}
   */
  public DiscoveryClient(ClientConfig config) {
    this.config = config;
  }

  /**
   * Periodically look for network services named by {@link ClientConfig#serviceName}
   * receiveInterval is calculated with minus 100 millis
   *
   * @param interval delay between requests
   * @return {@link Publisher} of service instances id's set
   */
  public final Flux<Set<String>> startLookup(Duration interval) {
    Duration receiveTimeout = interval.minus(Duration.ofMillis(100));

    if (receiveTimeout.isNegative()) {
      receiveTimeout = interval;
    }

    return startLookup(interval, receiveTimeout);
  }

  /**
   * Periodically look for network services named by {@link ClientConfig#serviceName}
   * receiveInterval is calculated with minus 100 millis
   *
   * @param interval delay between requests
   * @param receiveTimeout time interval to check responses from instances
   * @return {@link Publisher} of service instances id's set
   */
  public Flux<Set<String>> startLookup(Duration interval, Duration receiveTimeout) {
    Flux<AddressType> mainAddresses = getMainBroadcastAddresses();

    return Flux.interval(interval)
        .flatMap(unused -> lookupOnceInternal(receiveTimeout, mainAddresses))
        .repeat();
  }

  /**
   * Lookup services once
   *
   * @param receiveTimeout time interval to check responses from instances
   * @return Single or zero instance {@link Publisher} of service instances id's set
   */
  @SuppressWarnings("unused")
  public Mono<Set<String>> lookupOnce(Duration receiveTimeout) {
    Flux<AddressType> mainAddresses = getMainBroadcastAddresses();

    return Mono.from(lookupOnceInternal(receiveTimeout, mainAddresses));
  }

  /**
   * Internal method reuses cached mainAddresses
   *
   * @param receiveTimeout time interval to check responses from instances
   * @param mainAddresses cached local broadcast addresses
   * @return Single or zero instance {@link Publisher} of service instances id's set
   */
  private Flux<Set<String>> lookupOnceInternal(
      Duration receiveTimeout, Flux<AddressType> mainAddresses) {
    return Flux.from(send(mainAddresses))
        .flatMap(
            socket ->
                Flux.using(
                        () -> socket,
                        this::receive,
                        (s) -> {
                          try {
                            s.close();
                            logger.finer("Closed socket");
                          } catch (Exception ignored) {
                            logger.warning("Failed to close socket");
                          }
                        },
                        true)
                    .timeout(receiveTimeout, Flux.empty()))
        .bufferTimeout(config.endpointsCount, receiveTimeout)
        .map(strings -> strings.stream().filter(s -> !s.isEmpty()).collect(Collectors.toSet()));
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
   * @param mainAddresses precached addresses. May be appended with additional ones.
   * @return sockets on which sending was performed. Can be used to receive responses.
   */
  private Flux<SocketType> send(Flux<AddressType> mainAddresses) {

    // Try the main first
    Flux<AddressType> addresses = mainAddresses.concatWith(getAdditionalAddresses());

    Flux<AddressSocket<AddressType, SocketType>> v =
        addresses.zipWith(createSocket().repeat(), AddressSocket::new);

    var sendData = ("UDP_DISCOVERY_REQUEST " + config.serviceName).getBytes();

    return v.flatMap(
        addressWithSocket -> {
          AddressType address = addressWithSocket.address();
          SocketType socket = addressWithSocket.socket();

          var sendPacket = createSendPacket(sendData, address);

          return Flux.from(
              sendPacket(socket, sendPacket)
                  .doOnError(throwable -> logger.warning("send error: " + throwable))
                  .onErrorResume((throwable) -> Mono.empty())
                  .doOnNext(o -> logger.finer(">>> Request packet sent to: " + address)));
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

    return receivePacket(socket, receivePacket)
        .onErrorContinue((throwable, o) -> logger.warning("receive error: " + throwable));
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

              logger.fine(
                  ">>> Broadcast response from: "
                      + packetAddress(receivePacket)
                      + ", ["
                      + message
                      + "]");

              if (message.startsWith("UDP_DISCOVERY_RESPONSE ")) {
                return Mono.just(message.substring("UDP_DISCOVERY_RESPONSE ".length()));
              } else {
                return Mono.empty();
              }
            });
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
  protected abstract Flux<AddressType> getMainBroadcastAddresses();

  /**
   * Enumerate network interfaces broadcast addresses able to transmit and receive packets
   *
   * @return addresses Reactive Streams {@link Publisher}
   */
  protected abstract Flux<AddressType> getAdditionalAddresses();

  static final Logger logger = Logger.getLogger(DiscoveryClient.class.getName());
}
