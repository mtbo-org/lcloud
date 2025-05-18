/* (C) 2025 Vladimir E. Koltunov (mtbo.org) */

package org.mtbo.lcloud.discovery;

import reactor.core.publisher.Mono;

/** Allows to send and receive packets */
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
