/* (C) 2025 Vladimir E. Koltunov (mtbo.org) */

package org.mtbo.lcloud.discovery.udp;

import java.io.IOException;
import java.net.*;
import java.time.Duration;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Stream;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class DiscoveryClient {

  private final String serviceName;

  private final int port;

  public DiscoveryClient(String serviceName, int port) {
    this.serviceName = serviceName;
    this.port = port;
  }

  private static boolean canSend(NetworkInterface networkInterface) {
    try {
      return !networkInterface.isLoopback() && networkInterface.isUp();
    } catch (SocketException e) {
      // TODO
      return false;
    }
  }

  public Flux<Set<String>> lookup() {
    Flux<InetAddress> mainAddresses = getMainAddresses();
    return Flux.interval(Duration.ofMillis(1000))
        .flatMap(aLong -> Flux.from(send(mainAddresses)).doOnEach(
                signal -> {
                  System.out.println("-----------------------");
                  System.out.println("[" + signal + "]");
                  System.out.println("-----------------------");
                }).flatMap(count -> Flux.from(receive())));
  }

  /**
   * Wrap address to socket pair.
   *
   * @param inetAddress
   * @param socket
   */
  public record AddressSocket(InetAddress inetAddress, DatagramSocket socket) {}

  private Mono<Long> send(Flux<InetAddress> mainAddresses) {

    // Try the main first
     Flux<InetAddress> addresses = mainAddresses.concatWith(getAdditionalAddresses());

      Flux<AddressSocket> v =
        addresses.zipWith(
            Mono.fromCallable(DatagramSocket::new).publishOn(Schedulers.boundedElastic()).repeat(),
            AddressSocket::new);


    var sendData = ("UDP_DISCOVERY_REQUEST " + serviceName).getBytes();

    return v.flatMap(
            addressWithSocket -> {
              InetAddress address = addressWithSocket.inetAddress;
              DatagramSocket socket = addressWithSocket.socket;

              logger.fine("Try to send: " + address.getHostAddress() + " " + socket.isClosed());

              var sendPacket = new DatagramPacket(sendData, sendData.length, address, port);

              return Flux.from(
                  Mono.fromCallable(
                          () -> {
                            socket.send(sendPacket);
                            return 0;
                          })
                      .publishOn(Schedulers.boundedElastic())
                      .doOnError(throwable -> logger.warning("send error: " + throwable))
                      .onErrorResume((throwable) -> Mono.empty())
                      .doOnNext(
                          o ->
                              logger.finer(
                                  ">>> Request packet sent to: " + address.getHostAddress())));
            })
        .count();
  }

  private Mono<Set<String>> receive() {
    return Mono.just(Set.of());
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

  private Thread getThread(DatagramSocket socket, Set<String> result) {
    result.clear();

    var th =
        new Thread(
            () -> {
              var buffer = new byte[15000];

              while (!Thread.interrupted()) {
                var receivePacket = new DatagramPacket(buffer, buffer.length);

                try {
                  socket.receive(receivePacket);
                } catch (IOException e) {
                  break;
                }

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
                  result.add(message.substring("UDP_DISCOVERY_RESPONSE ".length()));
                }
              }
            });

    th.start();
    return th;
  }

  static final Logger logger = Logger.getLogger(DiscoveryClient.class.getName());
}
