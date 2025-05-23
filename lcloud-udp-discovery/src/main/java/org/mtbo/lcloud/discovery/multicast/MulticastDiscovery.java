/* (C) 2025 Vladimir E. (PROGrand) Koltunov (mtbo.org) */

package org.mtbo.lcloud.discovery.multicast;

import java.io.IOException;
import java.net.*;
import java.time.Duration;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class MulticastDiscovery {

  AtomicInteger recvCount = new AtomicInteger(0);

  public Flux<HashSet<String>> receive(String multicastAddr, int multicastPort) {
    return bindSockets(
        multicastAddr,
        multicastPort,
        pair ->
            receivePacket(pair)
                .doOnError(
                    throwable -> System.out.println("Error on receive: " + throwable.getMessage()))
                .onErrorResume(ignored -> Mono.delay(Duration.ofMillis(100)).thenReturn(""))
                .repeat()
                .bufferTimeout(1024, Duration.ofMillis(5000), HashSet::new)
                .map(
                    strings -> {
                      strings.remove("");
                      return strings;
                    }));
  }

  private Mono<String> receivePacket(
      Pair<? extends MulticastSocket, ? extends NetworkInterface> pair) {

    return Mono.fromCallable(
        () -> {
          final byte[] buf = new byte[256];
          DatagramPacket packet = new DatagramPacket(buf, buf.length);
          try {
            pair.t1.receive(packet);
            String received = new String(packet.getData(), 0, packet.getLength());

            System.out.println("Received packet: " + received);
            return received;
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
  }

  private <T> Flux<T> bindSockets(
      String multicastAddr,
      int multicastPort,
      Function<Pair<MulticastSocket, NetworkInterface>, ? extends Flux<? extends T>>
          socketSupplier) {
    return createSockets(
        multicastPort, (pair) -> bindSocket(pair, multicastAddr, multicastPort, socketSupplier));
  }

  private <T> Flux<T> bindSocket(
      Pair<MulticastSocket, NetworkInterface> pair,
      String address,
      int port,
      Function<Pair<MulticastSocket, NetworkInterface>, ? extends Flux<? extends T>>
          socketSupplier) {
    return Mono.fromCallable(() -> InetAddress.getByName(address))
        .publishOn(Schedulers.boundedElastic())
        //        .cache()
        .flatMapMany(
            inetAddress ->
                joinGroup(
                    pair.t1, new InetSocketAddress(inetAddress, port), pair.t2, socketSupplier));
  }

  private <T> Flux<T> joinGroup(
      MulticastSocket socket,
      InetSocketAddress inetSocketAddress,
      NetworkInterface networkInterface,
      Function<Pair<MulticastSocket, NetworkInterface>, ? extends Flux<? extends T>>
          socketSupplier) {
    return Flux.using(
        () -> {
          System.out.println("Join group: " + inetSocketAddress + " on " + networkInterface);
          socket.joinGroup(inetSocketAddress, networkInterface);
          return new Pair<>(socket, networkInterface);
        },
        socketSupplier,
        (pair) -> {
          try {
            System.out.println("Leave group: " + inetSocketAddress + " on " + networkInterface);
            pair.t1.leaveGroup(inetSocketAddress, pair.t2);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
  }

  private <T> Flux<T> createSockets(
      int multicastPort,
      Function<Pair<MulticastSocket, NetworkInterface>, ? extends Flux<? extends T>>
          socketSupplier) {
    return networkInterfaces()
        .flatMap(networkInterface -> createSocket(multicastPort, networkInterface, socketSupplier));
  }

  private Flux<NetworkInterface> networkInterfaces() {
    var interfaces =
        Mono.fromCallable(NetworkInterface::networkInterfaces).publishOn(Schedulers.single());

    return interfaces.flatMapMany(
        networkInterfaceStream ->
            Flux.fromStream(networkInterfaceStream)
                .filter(
                    networkInterface -> {
                      try {
                        return networkInterface.supportsMulticast()
                            && networkInterface.isUp()
                            && !networkInterface.isLoopback()
                            && networkInterface.supportsMulticast()
                            && !networkInterface.isPointToPoint()
                            && networkInterface.inetAddresses().findAny().isPresent();
                      } catch (SocketException e) {
                        return false;
                      }
                    })
                .publishOn(Schedulers.boundedElastic()));
  }

  private <T> Flux<T> createSocket(
      int port,
      NetworkInterface networkInterface,
      Function<Pair<MulticastSocket, NetworkInterface>, ? extends Flux<? extends T>>
          socketSupplier) {
    return Flux.using(
        () -> {
          System.out.println("Creating MulticastSocket for " + networkInterface);
          return new Pair<>(new MulticastSocket(port), networkInterface);
        },
        socketSupplier,
        pair -> {
          System.out.println("Closing MulticastSocket for " + networkInterface);
          pair.t1().close();
        },
        false);
  }

  public record Pair<T1, T2>(T1 t1, T2 t2) {}
}
