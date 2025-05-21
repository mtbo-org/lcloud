/* (C) 2025 Vladimir E. (PROGrand) Koltunov (mtbo.org) */
package org.mtbo.lcloud.discovery.udp;

import java.util.Objects;
import org.mtbo.lcloud.discovery.ClientConfig;

/** {@link UdpDiscoveryClient UDP discovery client's} config */
public final class UdpClientConfig extends ClientConfig {
  /** UDP port */
  public final int port;

  /**
   * Constructor
   *
   * @param serviceName unique service identifier
   * @param instanceName unique instance identifier
   * @param clientsCount max instances list size (default to {@link Integer#MAX_VALUE})
   * @param port UDP port
   */
  @SuppressWarnings("unused")
  public UdpClientConfig(String serviceName, String instanceName, int clientsCount, int port) {
    super(serviceName, instanceName, clientsCount);
    this.port = port;
  }

  /**
   * Constructor with defaults
   *
   * @param serviceName unique service identifier
   * @param instanceName unique instance identifier
   * @param port UDP port
   */
  public UdpClientConfig(String serviceName, String instanceName, int port) {
    super(serviceName, instanceName);
    this.port = port;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    UdpClientConfig udpConfig = (UdpClientConfig) o;
    return port == udpConfig.port;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), port);
  }

  @Override
  public String toString() {
    return "UdpConfig{" + "port=" + port + '}';
  }
}
