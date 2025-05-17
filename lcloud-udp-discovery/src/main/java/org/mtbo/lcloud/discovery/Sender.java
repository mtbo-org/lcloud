/* (C) 2025 Vladimir E. Koltunov (mtbo.org) */

package org.mtbo.lcloud.discovery;

import java.util.concurrent.CompletableFuture;

/** Allows to send packets */
public interface Sender {
  /**
   * Send packet
   *
   * @param packet packet with data and connection parameters
   * @return void future
   */
  CompletableFuture<Void> send(Packet packet);
}
