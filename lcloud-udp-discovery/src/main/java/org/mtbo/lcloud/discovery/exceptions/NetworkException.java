/* (C) 2025 Vladimir E. Koltunov (mtbo.org) */

package org.mtbo.lcloud.discovery.exceptions;

/** Unchecked network error to throw from reactive operators */
public class NetworkException extends RuntimeException {
  /**
   * Throwable wrapping constructor
   *
   * @param cause wrapped throwable
   */
  public NetworkException(Throwable cause) {
    super(cause);
  }
}
