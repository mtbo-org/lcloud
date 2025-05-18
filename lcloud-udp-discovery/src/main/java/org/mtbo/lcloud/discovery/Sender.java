/* (C) 2025 Vladimir E. Koltunov (mtbo.org) */

package org.mtbo.lcloud.discovery;

import reactor.core.publisher.Mono;

/** Allows to send packets */
public interface Sender<SocketType, PacketType> {
  /**
   * Send packet operator
   *
   * @param socket on which to send packet
   * @param packet packet with data and connection parameters
   * @return void mono
   */
  Mono<Boolean> send(SocketType socket, PacketType packet);
}
