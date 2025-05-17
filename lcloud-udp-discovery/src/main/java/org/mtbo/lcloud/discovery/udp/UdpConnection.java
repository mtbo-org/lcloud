/* (C) 2025 Vladimir E. Koltunov (mtbo.org) */

package org.mtbo.lcloud.discovery.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.mtbo.lcloud.discovery.Connection;
import org.mtbo.lcloud.discovery.Packet;
import org.mtbo.lcloud.discovery.exceptions.NetworkException;

/** Allows to send and receive packets using UDP connection */
public final class UdpConnection implements Connection {

  private final DatagramSocket socket;

  /**
   * Create and bind broadcast UDP socket
   *
   * @param port for incoming packets
   * @throws SocketException in case of bind error
   */
  public UdpConnection(final int port) throws SocketException {
    socket = new DatagramSocket(null);
    socket.setReuseAddress(true);
    socket.setBroadcast(true);
    socket.bind(new InetSocketAddress(port));
  }

  @Override
  public CompletableFuture<Packet> receive() {
    return CompletableFuture.supplyAsync(
        () -> {
          byte[] buffer = new byte[4096];

          DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

          try {
            socket.receive(packet);
          } catch (IOException e) {
            throw new NetworkException(e);
          }

          logger.finest(
              ">>> Discovery packet received from: " + packet.getAddress().getHostAddress());

          return new UdpPacket(packet);
        });
  }

  @Override
  public CompletableFuture<Void> send(Packet packet) {
    return CompletableFuture.runAsync(
        () -> {
          assert packet instanceof UdpPacket;
          final var udpPacket = (UdpPacket) packet;
          final var data = udpPacket.data();
          byte[] array = data.array();
          final var sendPacket =
              new DatagramPacket(
                  array,
                  data.arrayOffset(),
                  data.limit(),
                  udpPacket.packet.getAddress(),
                  udpPacket.packet.getPort());
          try {
            socket.send(sendPacket);
          } catch (IOException e) {
            logger.severe("Failed to send UDP packet: " + e);
            throw new RuntimeException(e);
          }
          logger.finer(
              ">>> Sent packet to: "
                  + sendPacket.getAddress().getHostAddress()
                  + ":"
                  + udpPacket.packet.getPort());
        });
  }

  static final Logger logger = Logger.getLogger(UdpConnection.class.getName());

  private record UdpPacket(DatagramPacket packet) implements Packet {

    @Override
    public ByteBuffer data() {
      return ByteBuffer.wrap(packet.getData(), packet.getOffset(), packet.getLength());
    }

    @Override
    public Packet copyWithData(ByteBuffer wrap) {
      return new UdpPacket(
          new DatagramPacket(
              wrap.array(),
              wrap.arrayOffset(),
              wrap.limit(),
              packet.getAddress(),
              packet.getPort()));
    }
  }
}
