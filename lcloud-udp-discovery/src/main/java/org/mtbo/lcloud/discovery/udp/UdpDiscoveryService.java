/* (C) 2025 Vladimir E. Koltunov (mtbo.org) */

package org.mtbo.lcloud.discovery.udp;

import java.net.DatagramSocket;
import org.mtbo.lcloud.discovery.DiscoveryService;
import org.mtbo.lcloud.discovery.ServiceConfig;

/** UDP implementation of {@link DiscoveryService} */
public class UdpDiscoveryService extends DiscoveryService<DatagramSocket, UdpPacket> {

  /**
   * Construct service with config
   *
   * @param config configuration, including {@link ServiceConfig#serviceName service name}
   */
  public UdpDiscoveryService(UdpServiceConfig config) {
    super(config);
  }
}
