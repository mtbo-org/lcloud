/* (C) 2025 Vladimir E. Koltunov (mtbo.org) */

package org.mtbo.lcloud.discovery;

import java.util.concurrent.CompletableFuture;

/** Allows to receive packets */
public interface Listener {
  /**
   * Receive packets
   *
   * @return packet from incoming connection
   */
  CompletableFuture<Packet> receive();
}
