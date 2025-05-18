/* (C) 2025 Vladimir E. Koltunov (mtbo.org) */

package org.mtbo.lcloud.discovery;

import reactor.core.publisher.Mono;

/** Allows to receive packets */
public interface Listener<SocketType, PacketType> {
  /**
   * Receive packets
   *
   * @param socket on which to receive packet
   * @return packet from incoming connection
   */
  Mono<PacketType> receive(SocketType socket);
}
