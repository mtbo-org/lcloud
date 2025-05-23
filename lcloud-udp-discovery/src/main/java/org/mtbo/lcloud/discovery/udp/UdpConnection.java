/* (C) 2025 Vladimir E. (PROGrand) Koltunov (mtbo.org) */

package org.mtbo.lcloud.discovery.udp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.logging.Level;
import org.mtbo.lcloud.discovery.Connection;
import org.mtbo.lcloud.discovery.logging.FileLineLogger;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/** Allows to send and receive packets using UDP connection */
public final class UdpConnection implements Connection<DatagramSocket, UdpPacket> {

  final int port;
  private FileLineLogger logger =
      FileLineLogger.getLogger(UdpConnection.class.getName(), "<<< SVC  ", 64);

  /**
   * UDP connection
   *
   * @param port for incoming packets
   */
  public UdpConnection(int port) {
    this.port = port;
  }

  /** Utility class for coverage test. */
  public void setupLogger() {
    if (logger == null) {
      logger = FileLineLogger.getLogger(UdpConnection.class.getName(), "<<< SVC  ", 64);
    }
  }

  /** Create and bind broadcast UDP socket */
  @Override
  public Mono<DatagramSocket> socket() {
    return Mono.fromCallable(
            () -> {
              var datagramSocket = new DatagramSocket(null);
              datagramSocket.setReuseAddress(true);
              datagramSocket.setBroadcast(true);
              datagramSocket.bind(new InetSocketAddress(InetAddress.getByName("0.0.0.0"), port));
              return datagramSocket;
            })
        .publishOn(Schedulers.boundedElastic());
  }

  @Override
  public void close(DatagramSocket socket) {
    if (socket != null) {
      socket.close();
    } else {
      throw new NullPointerException("socket is null");
    }
  }

  @Override
  public Mono<UdpPacket> receive(DatagramSocket socket) {

    return Mono.fromCallable(
            () -> {
              DatagramPacket packet = allocPacket();

              FileLineLogger.pt("receive packet");
              socket.receive(packet);
              return packet;
            })
        .doOnError(
            throwable -> {
              logger.finer("Error on packet receiving", throwable);
            })
        .publishOn(Schedulers.boundedElastic())
        .map(UdpPacket::new);
  }

  /**
   * Alloc UDP packet
   *
   * @return inet datagram packet
   */
  public DatagramPacket allocPacket() {
    byte[] buffer = new byte[4096];
    return new DatagramPacket(buffer, buffer.length);
  }

  @Override
  public Mono<Boolean> sendMessage(DatagramSocket socket, UdpPacket packet) {
    return Mono.fromCallable(
            () -> {
              final var data = packet.data();
              byte[] array = data.array();
              final int responsePort = packet.packet().getPort();
              final var sendPacket =
                  new DatagramPacket(
                      array,
                      data.arrayOffset(),
                      data.limit(),
                      packet.packet().getAddress(),
                      responsePort);
              if (logger.isLoggable(Level.FINER)) {
                logger.finer("Sending packet to: " + packet.packet().getAddress().getHostAddress());
              }

              FileLineLogger.pt("send packet");
              socket.send(sendPacket);

              if (logger.isLoggable(Level.FINER)) {
                logger.finer(
                    "3 3 3  Sent packet to: " + packet.packet().getAddress().getHostAddress());
              }

              return true;
            })
        //        .publishOn(Schedulers.boundedElastic())
        .onErrorResume(
            throwable -> {
              logger.finer("Exception caught on: " + throwable, throwable);
              return Mono.just(true);
            });
  }
}
