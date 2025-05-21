/* (C) 2025 Vladimir E. (PROGrand) Koltunov (mtbo.org) */
package org.mtbo.lcloud.discovery;

import java.util.Objects;

/** Discovery config */
public abstract class ClientConfig {
  /** unique service identifier */
  public final String serviceName;

  /** unique instance identifier */
  public final String instanceName;

  /** optional clients buffer size, {@link Integer#MAX_VALUE} for unlimited */
  public final int endpointsCount;

  /**
   * Constructor
   *
   * @param serviceName unique service identifier
   * @param instanceName unique instance identifier
   * @param endpointsCount optional clients buffer size, {@link Integer#MAX_VALUE} for unlimited
   */
  public ClientConfig(String serviceName, String instanceName, int endpointsCount) {
    this.serviceName = serviceName.replace(" ", "_");
    this.instanceName = instanceName.replace(" ", "_");
    this.endpointsCount = endpointsCount;
    assert Objects.equals(serviceName, this.serviceName);
    assert Objects.equals(instanceName, this.instanceName);
  }

  /**
   * Constructor with defaults
   *
   * @param serviceName unique service identifier
   * @param instanceName unique instance identifier
   */
  public ClientConfig(String serviceName, String instanceName) {
    this(serviceName, instanceName, Integer.MAX_VALUE);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    } else if (getClass() != obj.getClass()) {
      return false;
    }
    var that = (ClientConfig) obj;
    return Objects.equals(this.serviceName, that.serviceName)
        && this.endpointsCount == that.endpointsCount;
  }

  @Override
  public int hashCode() {
    return Objects.hash(serviceName, endpointsCount);
  }
}
