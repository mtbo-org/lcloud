/* (C) 2025 Vladimir E. (PROGrand) Koltunov (mtbo.org) */

package org.mtbo.lcloud.discovery;

import reactor.core.publisher.Mono;

/**
 * Allows to receive packets
 *
 * @param <SocketType> socket type, ex: {@link java.net.DatagramSocket DatagramSocket}
 * @param <PacketType> socket type, ex: {@link java.net.DatagramPacket DatagramPacket}
 */
public interface Listener<SocketType, PacketType> {
  /**
   * Receive packets
   *
   * @param socket on which to receive packet
   * @return packet from incoming connection
   */
  Mono<PacketType> receive(SocketType socket);
}
