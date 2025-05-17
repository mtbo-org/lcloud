/* (C) 2025 Vladimir E. Koltunov (mtbo.org) */

package org.mtbo.lcloud.discovery;

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
    source.process();
  }

  /**
   * Break loop
   *
   * @throws InterruptedException
   */
  public void shutdown() throws InterruptedException {
    source.close();
    processor.shutdown();
    join();
  }
}
