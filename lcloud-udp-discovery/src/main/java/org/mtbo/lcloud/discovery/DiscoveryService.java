/* (C) 2025 Vladimir E. Koltunov (mtbo.org) */

package org.mtbo.lcloud.discovery;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/** Packets pull-push loop thread */
public abstract class DiscoveryService<
    SocketType extends Closeable, PacketType extends Packet<PacketType>> {

  private final ByteBuffer prefix;
  private final byte[] sendData;

  /**
   * Construct service with config
   *
   * @param config configuration, including {@link ServiceConfig#serviceName service name}
   */
  public DiscoveryService(ServiceConfig config) {
    this.prefix =
        ByteBuffer.wrap(
                ("UDP_DISCOVERY_REQUEST " + config.serviceName).getBytes(StandardCharsets.UTF_8))
            .asReadOnlyBuffer();
    this.sendData =
        ("UDP_DISCOVERY_RESPONSE " + config.instanceName).getBytes(StandardCharsets.UTF_8);
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
                        (listenSocket) -> loop(connection, listenSocket),
                        connection::close,
                        true)
                    .publishOn(Schedulers.boundedElastic()));
  }

  private Flux<Boolean> loop(
      Connection<SocketType, PacketType> connection, SocketType listenSocket) {
    return connection
        .receive(listenSocket)
        .zipWith(Mono.just(listenSocket))
        .doOnNext(
            tuple -> {
              ByteBuffer data = tuple.getT1().data();
              logger.finer(
                  ">>> Packet received; packet: "
                      + new String(data.array(), data.arrayOffset(), data.limit()));
            })
        .repeat()
        .flatMap(
            tuple -> {
              PacketType packet = tuple.getT1();
              SocketType sendSocket = tuple.getT2();

              if (prefix.equals(packet.data().slice(0, prefix.limit()))) {
                return connection
                    .send(sendSocket, packet.copyWithData(ByteBuffer.wrap(sendData)))
                    .doOnError(
                        throwable -> {
                          logger.severe(throwable.getMessage());
                          logger.throwing(DiscoveryService.class.getSimpleName(), "run", throwable);
                        });
              } else {
                return Mono.just(false);
              }
            });
  }

  static final Logger logger = Logger.getLogger(DiscoveryService.class.getName());
}
