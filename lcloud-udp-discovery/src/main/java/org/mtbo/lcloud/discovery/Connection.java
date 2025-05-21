/* (C) 2025 Vladimir E. (PROGrand) Koltunov (mtbo.org) */
package org.mtbo.lcloud.discovery;

import reactor.core.publisher.Mono;

/**
 * Allows to send and receive packets
 *
 * @param <SocketType> socket type, ex: {@link java.net.DatagramSocket DatagramSocket}
 * @param <PacketType> socket type, ex: {@link java.net.DatagramPacket DatagramPacket}
 */
public interface Connection<SocketType, PacketType>
    extends Listener<SocketType, PacketType>, Sender<SocketType, PacketType> {

  /**
   * Create unbounded socket
   *
   * @return created socket
   */
  Mono<SocketType> socket();

  /**
   * Close socket. Shadow exception.
   *
   * @param socket to close
   */
  void close(SocketType socket);
}
