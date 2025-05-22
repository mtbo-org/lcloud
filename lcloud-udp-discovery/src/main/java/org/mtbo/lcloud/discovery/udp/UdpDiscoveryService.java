/* (C) 2025 Vladimir E. (PROGrand) Koltunov (mtbo.org) */

package org.mtbo.lcloud.discovery.udp;

import java.net.DatagramSocket;
import org.mtbo.lcloud.discovery.DiscoveryService;
import org.mtbo.lcloud.discovery.ServiceConfig;
import reactor.util.annotation.NonNull;

/** UDP implementation of {@link DiscoveryService} */
public class UdpDiscoveryService extends DiscoveryService<DatagramSocket, UdpPacket> {

  /**
   * Construct service with config
   *
   * @param config configuration, including {@link ServiceConfig#serviceName service name}
   */
  public UdpDiscoveryService(@NonNull UdpServiceConfig config) {
    super(config);
  }
}
