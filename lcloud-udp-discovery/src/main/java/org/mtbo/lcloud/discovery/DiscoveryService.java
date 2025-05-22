/* (C) 2025 Vladimir E. (PROGrand) Koltunov (mtbo.org) */

package org.mtbo.lcloud.discovery;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.mtbo.lcloud.discovery.logging.FileLineLogger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * Packets pull-push loop thread
 *
 * @param <SocketType> socket type, ex: {@link java.net.DatagramSocket DatagramSocket}
 * @param <PacketType> socket type, ex: {@link java.net.DatagramPacket DatagramPacket}
 */
public abstract class DiscoveryService<
    SocketType extends Closeable, PacketType extends Packet<PacketType>> {

  final FileLineLogger logger =
      FileLineLogger.getLogger(DiscoveryService.class.getName(), "<<< SVC  ", 64);
  private final ByteBuffer prefix;
  private final byte[] sendData;
  private final Scheduler receiveScheduler =
      Schedulers.newParallel("RECV", (Schedulers.DEFAULT_POOL_SIZE + 1) / 2);
  //  private final Scheduler logScheduler =
  //      Schedulers.newParallel("LOG", (Schedulers.DEFAULT_POOL_SIZE + 1) / 2);
  private final Scheduler logScheduler = Schedulers.newSingle("LOG");
  private final Scheduler sendScheduler =
      Schedulers.newParallel("SEND", (Schedulers.DEFAULT_POOL_SIZE + 1) / 2);

  /**
   * Construct service with config
   *
   * @param config configuration, including {@link ServiceConfig#serviceName service name}
   */
  public DiscoveryService(ServiceConfig config) {
    this.prefix =
        ByteBuffer.wrap(
                ("DISCOVERY_REQUEST " + config.serviceName + " FROM ")
                    .getBytes(StandardCharsets.UTF_8))
            .asReadOnlyBuffer();
    this.sendData =
        ("DISCOVERY_RESPONSE " + config.serviceName + " FROM " + config.instanceName)
            .getBytes(StandardCharsets.UTF_8);
  }

  /**
   * Create listen operator
   *
   * @param connection used for listen
   * @return flux with true in case of accepted packet, else false
   */
  public Flux<Boolean> listen(Connection<SocketType, PacketType> connection) {
    return Flux.from(connection.socket())
        .flatMap(
            socket ->
                Flux.using(
                        () -> socket,
                        (listenSocket) -> {
                          logger.fine("^^^^^^^^^^^^^^^^^^^ UP");
                          return loop(connection, listenSocket);
                        },
                        socket1 -> {
                          logger.fine("^^^^^^^^^^^^^^^^^^^ DOWN");
                          connection.close(socket1);
                        },
                        true)
                    .publishOn(Schedulers.boundedElastic()));
  }

  private Flux<Boolean> loop(
      Connection<SocketType, PacketType> connection, SocketType listenSocket) {
    return Flux.just(true)
        .repeat()
        .publishOn(receiveScheduler)
        .flatMap(
            (b) ->
                connection
                    .receive(listenSocket)
                    .zipWith(Mono.just(listenSocket))
                    .transformDeferred(
                        tuple2Mono ->
                            tuple2Mono
                                .publishOn(logScheduler)
                                .map(
                                    tuple -> {
                                      PacketType packet = tuple.getT1();
                                      ByteBuffer data = packet.data();
                                      FileLineLogger.pt("log packet");
                                      logger.finer(
                                          "2 2 2 Packet received; packet: "
                                              + new String(
                                                  data.array(), data.arrayOffset(), data.limit())
                                              + " ADDR "
                                              + packet);

                                      return tuple;
                                    }))
                    .onErrorResume(
                        throwable -> {
                          logger.finer("Exception caught on: " + throwable, throwable);
                          return Mono.empty();
                        }))
        .publishOn(sendScheduler)
        .flatMap(
            tuple -> {
              PacketType packet = tuple.getT1();
              SocketType sendSocket = tuple.getT2();

              if (canAcceptMessage(packet)) {
                return connection.sendMessage(
                    sendSocket, packet.copyWithData(ByteBuffer.wrap(sendData)))

                /*.doOnError(
                throwable -> {
                  logger.severe(throwable.toString(), throwable);
                })*/ ;
              } else {
                return Mono.just(false);
              }
            });
  }

  private boolean canAcceptMessage(PacketType packet) {
    return prefix.equals(packet.data().slice(0, Math.min(prefix.limit(), packet.data().limit())));
  }
}
