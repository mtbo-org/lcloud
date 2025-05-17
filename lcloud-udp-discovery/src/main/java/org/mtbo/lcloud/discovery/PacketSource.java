/* (C) 2025 Vladimir E. Koltunov (mtbo.org) */

package org.mtbo.lcloud.discovery;

import org.mtbo.lcloud.discovery.exceptions.NetworkException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.SubmissionPublisher;
import java.util.logging.Logger;

public final class PacketSource extends SubmissionPublisher<Packet> {

  private final Listener listener;

  public PacketSource(Listener listener) {
    this.listener = listener;
  }

  public CompletableFuture<Void> process() {
    return listener
        .receive()
        .handleAsync(
            (packet, throwable) -> {
              if (throwable instanceof RuntimeException) {
                throw (RuntimeException) throwable;
              }
              return offer(packet, (subscriber, packetUnused) -> false);
            })
        .exceptionallyAsync(
            throwable -> {

                throwable.printStackTrace();
              logger.severe("Unable to receive: " + throwable.getMessage());
              if (throwable instanceof NetworkException
                  || !(throwable instanceof RuntimeException)) {
                return Integer.MAX_VALUE;
              } else {
                throw (RuntimeException) throwable;
              }
            })
        .thenAcceptAsync(lagOrDrop -> logger.finer("LAG OR DROP: " + lagOrDrop));
  }

  static final Logger logger = Logger.getLogger(PacketSource.class.getName());
}
