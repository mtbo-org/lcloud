/* (C) 2025 Vladimir E. Koltunov (mtbo.org) */

package org.mtbo.lcloud.discovery;

import java.util.concurrent.ExecutionException;

/** Packets pull-push loop thread */
public final class DiscoveryService extends Thread {
  private final PacketSource source;
  private final PacketProcessor processor;

  /**
   * Construct loop
   *
   * @param instanceName instance name
   * @param serviceName instance namespace
   * @param connection to push packets
   */
  public DiscoveryService(String instanceName, String serviceName, Connection connection) {
    source = new PacketSource(connection);
    processor = new PacketProcessor(instanceName, serviceName, connection);
    source.subscribe(processor);
  }

  @Override
  public void run() {

    while (!interrupted()) {
      try {
        source.process().get();
      } catch (InterruptedException e) {
        break;
      } catch (ExecutionException e) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * Break loop
   *
   */
  public void shutdown() {
    source.close();
    processor.shutdown();
    interrupt();
    if (!interrupted()) {
        try {
            join();
        } catch (InterruptedException ignored) {

        }
    }
  }
}
