/* (C) 2025 Vladimir E. (PROGrand) Koltunov (mtbo.org) */

package org.mtbo.lcloud.discovery.multicast;

import java.io.IOException;
import java.net.*;
import java.time.Duration;
import java.util.HashSet;
import java.util.function.Function;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

public class MulticastDiscovery {
  final Config config;

  final Scheduler networkManagementScheduler =
      Schedulers.newParallel("network-sched", Schedulers.DEFAULT_POOL_SIZE / 4);

  final Scheduler receiveScheduler =
      Schedulers.newParallel("receiving-sched", Schedulers.DEFAULT_POOL_SIZE);

  public MulticastDiscovery(Config config) {
    this.config = config;
  }

  public Flux<HashSet<String>> receive() {
    return bindSockets(
            pair ->
                receivePacket(pair)
                    .doOnNext(System.out::println)
                    .onErrorResume(
                        throwable -> {
                          if (!(throwable.getCause() instanceof SocketTimeoutException)) {
                            System.out.println("Error on receive: " + throwable.getMessage());
                          }
                          return Mono.delay(Duration.ofMillis(100)).thenReturn("");
                        })
                    .repeat())
        .bufferTimeout(Integer.MAX_VALUE, config.interval, HashSet::new)
        .map(
            strings -> {
              strings.remove("");
              return strings;
            });
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

                //                System.out.println(
                //                    "Received packet: "
                //                        + received
                //                        + " on "
                //                        + Thread.currentThread().getName()
                //                        + ": "
                //                        + pair.t2.getName());
                return received;
              } catch (SocketTimeoutException | SocketException e) {
                return "";
              } catch (IOException e) {
                System.out.println("Receiving exception: " + e);
                throw new RuntimeException(e);
              }
            })
        .timeout(config.interval)
        .publishOn(receiveScheduler);
  }

  private <T> Flux<T> bindSockets(
      Function<Pair<MulticastSocket, NetworkInterface>, ? extends Flux<? extends T>>
          socketSupplier) {
    return createSockets((pair) -> bindSocket(pair, socketSupplier));
  }

  private <T> Flux<T> bindSocket(
      Pair<MulticastSocket, NetworkInterface> pair,
      Function<Pair<MulticastSocket, NetworkInterface>, ? extends Flux<? extends T>>
          socketSupplier) {
    return Mono.fromCallable(() -> InetAddress.getByName(config.multicastAddr))
        .publishOn(Schedulers.boundedElastic())
        //        .cache()
        .flatMapMany(
            inetAddress ->
                joinGroup(
                    pair.t1,
                    new InetSocketAddress(inetAddress, config.multicastPort),
                    pair.t2,
                    socketSupplier));
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
            })
        .publishOn(networkManagementScheduler);
  }

  private <T> Flux<T> createSockets(
      Function<Pair<MulticastSocket, NetworkInterface>, ? extends Flux<? extends T>>
          socketSupplier) {
    return networkInterfaces()
        .flatMap(networkInterface -> createSocket(networkInterface, socketSupplier));
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
                .publishOn(networkManagementScheduler));
  }

  private <T> Flux<T> createSocket(
      NetworkInterface networkInterface,
      Function<Pair<MulticastSocket, NetworkInterface>, ? extends Flux<? extends T>>
          socketSupplier) {
    return Flux.using(
            () -> {
              System.out.println("Creating MulticastSocket for " + networkInterface);
              MulticastSocket multicastSocket = new MulticastSocket(config.multicastPort);
              multicastSocket.setSoTimeout((int) config.interval.toMillis());
              return new Pair<>(multicastSocket, networkInterface);
            },
            socketSupplier,
            pair -> {
              System.out.println("Closing MulticastSocket for " + networkInterface);
              pair.t1().close();
            },
            false)
        .publishOn(networkManagementScheduler);
  }

  public record Config(
      String serviceName, String multicastAddr, int multicastPort, Duration interval) {}

  public record Pair<T1, T2>(T1 t1, T2 t2) {}
}
