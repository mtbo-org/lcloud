/* (C) 2025 Vladimir E. (PROGrand) Koltunov (mtbo.org) */

package org.mtbo.lcloud.discovery.udp;

import java.util.Objects;
import org.mtbo.lcloud.discovery.ServiceConfig;

/** UDP implementation of {@link ServiceConfig} */
public class UdpServiceConfig extends ServiceConfig {
  /** UDP port */
  public final int port;

  /**
   * Constructor
   *
   * @param serviceName unique service identifier
   * @param instanceName this endpoint name
   * @param port UDP port
   */
  public UdpServiceConfig(String serviceName, String instanceName, int port) {
    super(serviceName, instanceName);
    this.port = port;
  }

  @Override
  public String toString() {
    return "UdpServiceConfig{" + "port=" + port + '}';
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || getClass() != obj.getClass()) return false;
    if (!super.equals(obj)) return false;
    UdpServiceConfig that = (UdpServiceConfig) obj;
    return port == that.port;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), port);
  }
}
