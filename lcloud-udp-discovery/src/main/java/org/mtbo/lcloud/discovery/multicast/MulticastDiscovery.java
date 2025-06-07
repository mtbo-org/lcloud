/* (C) 2025 Vladimir E. (PROGrand) Koltunov (mtbo.org) */

package org.mtbo.lcloud.discovery.multicast;

import java.io.IOException;
import java.net.*;
import java.time.Duration;
import java.util.HashSet;
import java.util.Objects;
import java.util.function.Function;
import org.mtbo.lcloud.logging.FileLineLogger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/** Multicast discovery */
public class MulticastDiscovery {
  final FileLineLogger logger =
      FileLineLogger.getLogger(MulticastDiscovery.class.getName(), "<<< SVC  ", 32);

  final Config config;

  final Scheduler networkManagementScheduler =
      Schedulers.newParallel("network-sched", Math.max(2, (Schedulers.DEFAULT_POOL_SIZE + 3) / 4));

  //  final Scheduler receiveScheduler = Schedulers.boundedElastic();
  final Scheduler receiveScheduler =
      Schedulers.newParallel(
          "receiver-sched", Math.max(1, (int) NetworkInterface.networkInterfaces().count()));

  //      Schedulers.newParallel("receiver-sched", Schedulers.DEFAULT_POOL_SIZE);

  /**
   * construct with config
   *
   * @param config config
   * @throws SocketException in case of any error
   */
  public MulticastDiscovery(Config config) throws SocketException {
    this.config = config;
  }

  /**
   * Collect ping requests
   *
   * @return list of instances
   */
  public Flux<HashSet<String>> receive() {
    return bindSockets(
            pair ->
                receivePacket(pair)
                    //                    .doOnNext(message -> logger.finer(message))
                    .onErrorResume(
                        throwable -> {
                          if (!(throwable.getCause() instanceof SocketTimeoutException)) {
                            logger.finer("Error on receive: " + throwable.getMessage(), throwable);
                          }
                          return Mono.delay(Duration.ofMillis(100)).thenReturn("");
                        })
                    .repeat())
        .doOnEach(stringSignal -> logger.finer(stringSignal.toString()))
        .bufferTimeout(Integer.MAX_VALUE, config.interval, HashSet::new)
        .doOnNext(message -> logger.finer(message.toString()))
        .map(
            strings -> {
              strings.remove("");
              return strings;
            });
  }

  private Mono<String> receivePacket(
      Pair<? extends MulticastSocket, ? extends NetworkInterface> pair) {

    final byte[] buf = new byte[256];
    DatagramPacket packet1 = new DatagramPacket(buf, buf.length);

    return Mono.just(packet1)
        .cache()
        .flatMap(
            datagramPacket ->
                Mono.fromCallable(
                        () -> {
                          try {

                            //                            logger.finest("Receiving packet on " +
                            // pair.t2);
                            pair.t1.receive(datagramPacket);
                            //                            logger.finest("Received packet  on " +
                            // pair.t2);

                            String[] received =
                                new String(datagramPacket.getData(), 0, datagramPacket.getLength())
                                    .split(" ");
                            if (4 == received.length
                                && Objects.equals(received[0], "LC_DISCOVERY")
                                && received[1].equals(config.serviceName)
                                && Objects.equals(received[2], "FROM")) {
                              return received[3];
                            } else {
                              return null;
                            }

                          } catch (SocketTimeoutException | SocketException e) {
                            //                            logger.finest("Receiving ERROR on " +
                            // pair.t2 + ": " + e.getMessage());
                            return null;
                          } catch (IOException e) {
                            logger.finer("Receiving exception: " + e, e);
                            throw new RuntimeException(e);
                          }
                        })
                    .timeout(config.interval)
                    .publishOn(receiveScheduler));
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
        .cache()
        .flatMapMany(
            inetAddress ->
                joinGroup(
                    pair,
                    new InetSocketAddress(inetAddress, config.multicastPort),
                    socketSupplier));
  }

  private <T> Flux<T> joinGroup(
      Pair<? extends MulticastSocket, ? extends NetworkInterface> pair,
      InetSocketAddress inetSocketAddress,
      Function<Pair<MulticastSocket, NetworkInterface>, ? extends Flux<? extends T>>
          socketSupplier) {
    return Flux.using(
            () -> {
              logger.finer("Join group: " + inetSocketAddress + " on " + pair.t2);
              pair.t1.joinGroup(inetSocketAddress, pair.t2);
              return new Pair<>(pair.t1, pair.t2);
            },
            socketSupplier,
            (p) -> {
              try {
                logger.finer("Leave group: " + inetSocketAddress + " on " + pair.t2);
                pair.t1.leaveGroup(inetSocketAddress, p.t2);
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

    return interfaces
        .flatMapMany(
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
                    .publishOn(networkManagementScheduler))
        .doOnNext(
            networkInterface -> {
              logger.info("Network interface: " + networkInterface);
            });
  }

  private <T> Flux<T> createSocket(
      NetworkInterface networkInterface,
      Function<Pair<MulticastSocket, NetworkInterface>, ? extends Flux<? extends T>>
          socketSupplier) {
    return Flux.using(
            () -> {
              logger.finer("Creating MulticastSocket for " + networkInterface);
              MulticastSocket multicastSocket = new MulticastSocket(config.multicastPort);
              multicastSocket.setSoTimeout((int) config.interval.toMillis());
              return new Pair<>(multicastSocket, networkInterface);
            },
            socketSupplier,
            pair -> {
              logger.finer("Closing MulticastSocket for " + networkInterface);
              pair.t1().close();
            },
            false)
        .publishOn(Schedulers.boundedElastic());
  }

  /**
   * Multicast discovery config
   *
   * @param serviceName service name
   * @param multicastAddr address
   * @param multicastPort port
   * @param interval lookup interval
   */
  public record Config(
      String serviceName, String multicastAddr, int multicastPort, Duration interval) {}

  /**
   * just Pair
   *
   * @param t1 first
   * @param t2 second
   * @param <T1> type of first
   * @param <T2> type of second
   */
  public record Pair<T1, T2>(T1 t1, T2 t2) {}
}
