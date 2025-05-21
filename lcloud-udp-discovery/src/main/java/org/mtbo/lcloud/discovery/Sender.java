/* (C) 2025 Vladimir E. (PROGrand) Koltunov (mtbo.org) */
package org.mtbo.lcloud.discovery;

import reactor.core.publisher.Mono;

/**
 * Allows to send packets
 *
 * @param <SocketType> socket type, ex: {@link java.net.DatagramSocket DatagramSocket}
 * @param <PacketType> socket type, ex: {@link java.net.DatagramPacket DatagramPacket}
 */
public interface Sender<SocketType, PacketType> {
  /**
   * Send packet operator
   *
   * @param socket on which to send packet
   * @param packet packet with data and connection parameters
   * @return void mono
   */
  Mono<Boolean> sendMessage(SocketType socket, PacketType packet);
}
