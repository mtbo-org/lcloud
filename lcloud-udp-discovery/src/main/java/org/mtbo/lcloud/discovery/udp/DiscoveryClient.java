/* (C) 2025 Vladimir E. Koltunov (mtbo.org) */

package org.mtbo.lcloud.discovery.udp;

import java.net.*;
import java.time.Duration;
import java.util.HashSet;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/** Allows to request network services named by some {@link Config#serviceName serice name} */
public class DiscoveryClient {

  private final Config config;

  /**
   * Construct client with config
   *
   * @param config configuration, including {@link Config#serviceName service name} and {@link
   *     Config#port port}
   */
  public DiscoveryClient(Config config) {
    this.config = config;
  }

  /**
   * {@link DiscoveryClient discovery client's} config
   *
   * @param serviceName unique service identifier
   * @param port UDP port
   * @param clientsCount optional clients buffer size, {@link Integer#MAX_VALUE} for unlimited
   */
  public record Config(String serviceName, int port, int clientsCount) {
    /**
     * Constructor with defaults
     *
     * @param serviceName unique service identifier
     * @param port UDP port
     */
    public Config(String serviceName, int port) {
      this(serviceName, port, Integer.MAX_VALUE);
    }
  }

  /**
   * Periodically look for network services named by {@link Config#serviceName} receiveInterval is
   * calculated with minus 100 millis
   *
   * @param interval delay between requests
   * @return {@link Publisher} of service instances id's set
   */
  public Flux<HashSet<String>> startLookup(Duration interval) {
    Duration receiveTimeout = interval.minus(Duration.ofMillis(100));

    if (receiveTimeout.isNegative()) {
      receiveTimeout = interval;
    }

    return startLookup(interval, receiveTimeout);
  }

  /**
   * Periodically look for network services named by {@link Config#serviceName} receiveInterval is
   * calculated with minus 100 millis
   *
   * @param interval delay between requests
   * @param receiveTimeout time interval to check responses from instances
   * @return {@link Publisher} of service instances id's set
   */
  public Flux<HashSet<String>> startLookup(Duration interval, Duration receiveTimeout) {
    Flux<InetAddress> mainAddresses = getMainAddresses();

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
  public Mono<HashSet<String>> lookupOnce(Duration receiveTimeout) {
    Flux<InetAddress> mainAddresses = getMainAddresses();

    return Mono.from(lookupOnceInternal(receiveTimeout, mainAddresses));
  }

  /**
   * Internal method reuses cached mainAddresses
   *
   * @param receiveTimeout
   * @param mainAddresses
   * @return
   */
  private Flux<HashSet<String>> lookupOnceInternal(
      Duration receiveTimeout, Flux<InetAddress> mainAddresses) {
    return Flux.from(send(mainAddresses))
        .flatMap(this::receive)
        .bufferTimeout(128, receiveTimeout)
        .map(HashSet::new);
  }

  /**
   * Wrap address to socket pair.
   *
   * @param inetAddress internet address
   * @param socket UDP socket
   */
  private record AddressSocket(InetAddress inetAddress, DatagramSocket socket) {}

  private Flux<DatagramSocket> send(Flux<InetAddress> mainAddresses) {

    // Try the main first
    Flux<InetAddress> addresses = mainAddresses.concatWith(getAdditionalAddresses());

    Flux<AddressSocket> v =
        addresses.zipWith(
            Mono.fromCallable(DatagramSocket::new).publishOn(Schedulers.boundedElastic()).repeat(),
            AddressSocket::new);

    var sendData = ("UDP_DISCOVERY_REQUEST " + config.serviceName).getBytes();

    return v.flatMap(
        addressWithSocket -> {
          InetAddress address = addressWithSocket.inetAddress;
          DatagramSocket socket = addressWithSocket.socket;

          logger.fine("Try to send: " + address.getHostAddress() + " " + socket.isClosed());

          var sendPacket = new DatagramPacket(sendData, sendData.length, address, config.port);

          return Flux.from(
              Mono.fromCallable(
                      () -> {
                        socket.send(sendPacket);
                        return socket;
                      })
                  .publishOn(Schedulers.boundedElastic())
                  .doOnError(throwable -> logger.warning("send error: " + throwable))
                  .onErrorResume((throwable) -> Mono.empty())
                  .doOnNext(
                      o ->
                          logger.finer(">>> Request packet sent to: " + address.getHostAddress())));
        });
  }

  private Mono<DatagramPacket> receiveResponse(DatagramSocket socket) {
    var buffer = new byte[15000];

    var receivePacket = new DatagramPacket(buffer, buffer.length);

    return Mono.fromCallable(
            () -> {
              socket.receive(receivePacket);
              return receivePacket;
            })
        .publishOn(Schedulers.boundedElastic())
        .onErrorContinue((throwable, o) -> logger.warning("receive error: " + throwable));
  }

  private Flux<String> receive(DatagramSocket socket) {
    return getResponses(socket);
  }

  private Flux<String> getResponses(DatagramSocket socket) {
    return Flux.from(receiveResponse(socket))
        .repeat(1)
        .flatMap(
            receivePacket -> {
              var message =
                  new String(
                      receivePacket.getData(),
                      receivePacket.getOffset(),
                      receivePacket.getLength());

              logger.fine(
                  ">>> Broadcast response from: "
                      + receivePacket.getAddress().getHostAddress()
                      + ", ["
                      + message
                      + "]");

              if (message.startsWith("UDP_DISCOVERY_RESPONSE ")) {
                return Mono.just(message.substring("UDP_DISCOVERY_RESPONSE ".length()));
              } else {
                return Mono.empty();
              }
            })
        .doOnEach(stringSignal -> logger.fine("XXX: " + stringSignal));
  }

  /**
   * Get IPv4 network broadcast address
   *
   * @return cached local broadcast address
   */
  public static Flux<InetAddress> getMainAddresses() {
    return Flux.from(
            Mono.fromCallable(() -> InetAddress.getByName("255.255.255.255"))
                .publishOn(Schedulers.boundedElastic()))
        .doOnNext(inetAddress -> logger.fine("get main address (REAL): " + inetAddress.toString()))
        .cache()
        .doOnNext(
            inetAddress -> logger.fine("get main address (CACHED): " + inetAddress.toString()));
  }

  /**
   * Enumerate network interfaces broadcast addresses able to transmit and receive packets
   *
   * @return addresses Reactive Streams {@link Publisher}
   */
  public static Flux<InetAddress> getAdditionalAddresses() {
    return getNetworkInterfaces()
        .flatMap(Flux::fromStream)
        .filter(DiscoveryClient::canSend)
        .flatMapIterable(NetworkInterface::getInterfaceAddresses)
        .mapNotNull(InterfaceAddress::getBroadcast);
  }

  private static Flux<Stream<NetworkInterface>> getNetworkInterfaces() {
    return Flux.from(
        Mono.fromCallable(NetworkInterface::networkInterfaces)
            .publishOn(Schedulers.boundedElastic())
            .onErrorResume(e -> Mono.just(Stream.empty())));
  }

  private static boolean canSend(NetworkInterface networkInterface) {
    try {
      return !networkInterface.isLoopback() && networkInterface.isUp();
    } catch (SocketException e) {
      logger.warning("interface is not allowed to send packets: " + networkInterface.getName());
      return false;
    }
  }

  static final Logger logger = Logger.getLogger(DiscoveryClient.class.getName());
}
