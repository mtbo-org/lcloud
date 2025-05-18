/* (C) 2025 Vladimir E. Koltunov (mtbo.org) */

package org.mtbo.lcloud.discovery.udp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.logging.Logger;
import org.mtbo.lcloud.discovery.Connection;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/** Allows to send and receive packets using UDP connection */
public final class UdpConnection implements Connection<DatagramSocket, UdpPacket> {

  final int port;

  /**
   * UDP connection
   *
   * @param port for incoming packets
   */
  public UdpConnection(int port) {
    this.port = port;
  }

  /** Create and bind broadcast UDP socket */
  @Override
  public Mono<DatagramSocket> socket() {
    return Mono.fromCallable(
            () -> {
              var datagramSocket = new DatagramSocket(null);
              datagramSocket.setReuseAddress(true);
              datagramSocket.setBroadcast(true);
              datagramSocket.bind(new InetSocketAddress(port));
              return datagramSocket;
            })
        .publishOn(Schedulers.boundedElastic());
  }

  @Override
  public void close(DatagramSocket socket) {
    socket.close();
  }

  @Override
  public Mono<UdpPacket> receive(DatagramSocket socket) {

    return Mono.fromCallable(
            () -> {
              byte[] buffer = new byte[4096];

              DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

              socket.receive(packet);
              return packet;
            })
        .publishOn(Schedulers.boundedElastic())
        .map(UdpPacket::new);
  }

  @Override
  public Mono<Boolean> send(DatagramSocket socket, UdpPacket packet) {
    return Mono.fromCallable(
            () -> {
              final var data = packet.data();
              byte[] array = data.array();
              final var sendPacket =
                  new DatagramPacket(
                      array,
                      data.arrayOffset(),
                      data.limit(),
                      packet.packet().getAddress(),
                      packet.packet().getPort());
              socket.send(sendPacket);
              logger.finer(
                  ">>> Sent packet to: "
                      + sendPacket.getAddress().getHostAddress()
                      + ":"
                      + packet.packet().getPort());

              return true;
            })
        .publishOn(Schedulers.boundedElastic());
  }

  static final Logger logger = Logger.getLogger(UdpConnection.class.getName());
}
