/* (C) 2025 Vladimir E. Koltunov (mtbo.org) */

package org.mtbo.lcloud.discovery;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Flow;
import java.util.logging.Logger;

/** Process {@link Packet packets} and send responses to {@link #sender} */
public class PacketProcessor implements Flow.Subscriber<Packet> {
  private final Sender sender;

  private final ByteBuffer prefix;
  private final byte[] sendData;

  private Flow.Subscription subscription;

  /**
   * Constructor
   *
   * @param instanceName service instance name
   * @param serviceName service name
   * @param sender downstream
   */
  public PacketProcessor(String instanceName, String serviceName, Sender sender) {
    this.sender = sender;

    this.prefix =
        ByteBuffer.wrap(("UDP_DISCOVERY_REQUEST " + serviceName).getBytes(StandardCharsets.UTF_8))
            .asReadOnlyBuffer();
    this.sendData = ("UDP_DISCOVERY_RESPONSE " + instanceName).getBytes(StandardCharsets.UTF_8);
  }

  /** Shutdown {@link #subscription} */
  public void shutdown() {
    subscription.cancel();
  }

  @Override
  public void onSubscribe(Flow.Subscription subscription) {
    this.subscription = subscription;
    subscription.request(1);
  }

  @Override
  public void onNext(Packet item) {
    ByteBuffer data = item.data();
    logger.finer(
        ">>> Packet received; packet: "
            + new String(data.array(), data.arrayOffset(), data.limit()));

    if (prefix.equals(data.slice(0, prefix.limit()))) {
      // Send a response
      sender
          .send(item.copyWithData(ByteBuffer.wrap(sendData)))
          .exceptionallyAsync(
              throwable -> {
                logger.severe(throwable.getMessage());
                logger.throwing(PacketProcessor.class.getSimpleName(), "run", throwable);
                return null;
              });
    }

    // Enqueue next packet processing
    subscription.request(1);
  }

  @Override
  public void onError(Throwable throwable) {
    logger.severe(throwable.getMessage());
    logger.throwing(PacketProcessor.class.getSimpleName(), "onError", throwable);
  }

  @Override
  public void onComplete() {
    logger.severe("Stop response");
  }

  static final Logger logger = Logger.getLogger(PacketProcessor.class.getName());
}
