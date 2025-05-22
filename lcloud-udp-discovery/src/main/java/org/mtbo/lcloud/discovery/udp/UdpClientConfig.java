/* (C) 2025 Vladimir E. (PROGrand) Koltunov (mtbo.org) */

package org.mtbo.lcloud.discovery.udp;

import java.util.Objects;
import org.mtbo.lcloud.discovery.ClientConfig;

/** {@link UdpDiscoveryClient UDP discovery client's} config */
public final class UdpClientConfig extends ClientConfig {
  /** UDP server port */
  public final int serverPort;

  /** UDP client port */
  public final int clientPort;

  /**
   * Constructor
   *
   * @param serviceName unique service identifier
   * @param instanceName unique instance identifier
   * @param clientsCount max instances list size (default to {@link Integer#MAX_VALUE})
   * @param serverPort UDP server port
   * @param clientPort UDP client Port
   */
  @SuppressWarnings("unused")
  public UdpClientConfig(
      String serviceName, String instanceName, int clientsCount, int serverPort, int clientPort) {
    super(serviceName, instanceName, clientsCount);
    this.serverPort = serverPort;
    this.clientPort = clientPort;
  }

  /**
   * Constructor with defaults
   *
   * @param serviceName unique service identifier
   * @param instanceName unique instance identifier
   * @param serverPort UDP server port
   * @param clientPort UDP client port
   */
  public UdpClientConfig(String serviceName, String instanceName, int serverPort, int clientPort) {
    super(serviceName, instanceName);
    this.serverPort = serverPort;
    this.clientPort = clientPort;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    UdpClientConfig udpConfig = (UdpClientConfig) o;
    return serverPort == udpConfig.serverPort;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), serverPort, clientPort);
  }

  @Override
  public String toString() {
    return "UdpConfig{" + "serverPort=" + serverPort + " clientPort=" + clientPort + '}';
  }
}
