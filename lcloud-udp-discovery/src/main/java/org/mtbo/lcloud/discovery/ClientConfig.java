/* (C) 2025 Vladimir E. Koltunov (mtbo.org) */

package org.mtbo.lcloud.discovery;

import java.util.Objects;

/** Discovery config */
public abstract class ClientConfig {
  /** unique service identifier */
  public final String serviceName;

  /** optional clients buffer size, {@link Integer#MAX_VALUE} for unlimited */
  public final int endpointsCount;

  /**
   * @param serviceName unique service identifier
   * @param endpointsCount optional clients buffer size, {@link Integer#MAX_VALUE} for unlimited
   */
  public ClientConfig(String serviceName, int endpointsCount) {
    this.serviceName = serviceName;
    this.endpointsCount = endpointsCount;
  }

  /**
   * Constructor with defaults
   *
   * @param serviceName unique service identifier
   */
  public ClientConfig(String serviceName) {
    this(serviceName, Integer.MAX_VALUE);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    var that = (ClientConfig) obj;
    return Objects.equals(this.serviceName, that.serviceName)
        && this.endpointsCount == that.endpointsCount;
  }

  @Override
  public int hashCode() {
    return Objects.hash(serviceName, endpointsCount);
  }

  @Override
  public String toString() {
    return "Config["
        + "serviceName="
        + serviceName
        + ", "
        + "port="
        + ", "
        + "clientsCount="
        + endpointsCount
        + ']';
  }
}
