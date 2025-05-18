/* (C) 2025 Vladimir E. Koltunov (mtbo.org) */

package org.mtbo.lcloud.discovery;

import java.util.concurrent.SubmissionPublisher;
import java.util.logging.Logger;
import org.mtbo.lcloud.discovery.exceptions.NetworkException;

/** Incoming packet {@link java.util.concurrent.Flow.Publisher publisher} */
public final class PacketSource extends SubmissionPublisher<Packet> {

  private final Listener listener;

  /**
   * Constructor
   *
   * @param listener incoming packets source
   */
  public PacketSource(Listener listener) {
    this.listener = listener;
  }

  /** Network receive/send step. */
  public void process() {
    listener
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
              logger.severe("Unable to receive: " + throwable.getMessage());
              throwable.printStackTrace();
              logger.throwing(PacketSource.class.getName(), "process", throwable);
              if (throwable instanceof NetworkException
                  || !(throwable instanceof RuntimeException)) {
                return Integer.MAX_VALUE;
              } else {
                throw (RuntimeException) throwable;
              }
            })
        .thenAcceptAsync(lagOrDrop -> logger.finer("LAG OR DROP: " + lagOrDrop))
        .join();
  }

  static final Logger logger = Logger.getLogger(PacketSource.class.getName());
}
