/* (C) 2025 Vladimir E. Koltunov (mtbo.org) */

package org.mtbo.lcloud.discovery;

import java.util.Objects;

/** Discovery config */
public abstract class ServiceConfig {
  /** unique service identifier */
  public final String serviceName;

  /** this endpoint name */
  public final String instanceName;

  /**
   * @param serviceName unique service identifier
   * @param instanceName this endpoint name
   */
  public ServiceConfig(String serviceName, String instanceName) {
    this.serviceName = serviceName;
    this.instanceName = instanceName;
  }

  @Override
  public String toString() {
    return "ServiceConfig{"
        + "serviceName='"
        + serviceName
        + '\''
        + ", instanceName='"
        + instanceName
        + '\''
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    ServiceConfig that = (ServiceConfig) o;
    return Objects.equals(serviceName, that.serviceName)
        && Objects.equals(instanceName, that.instanceName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(serviceName, instanceName);
  }
}
