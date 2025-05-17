/* (C) 2025 Vladimir E. Koltunov (mtbo.org) */

package org.mtbo.lcloud.discovery;

import java.util.concurrent.SubmissionPublisher;
import java.util.stream.Stream;

public final class PacketSource extends SubmissionPublisher<Packet> {

  private final Listener listener;

  public PacketSource(Listener listener) {
    this.listener = listener;
  }

  public void process() {
    Stream.iterate(listener.receive(), o -> !isClosed(), o -> listener.receive())
        .forEach(
            packetFuture ->
                packetFuture.handleAsync(
                    (packet, throwable) -> offer(packet, (subscriber, packetUnused) -> false)));
  }
}
