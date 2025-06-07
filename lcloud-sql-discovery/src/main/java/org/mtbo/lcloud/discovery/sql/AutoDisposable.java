/* (C) 2025 Vladimir E. (PROGrand) Koltunov (mtbo.org) */

package org.mtbo.lcloud.discovery.sql;

import reactor.core.Disposable;

/** Try with resource wrapper for disposable */
public final class AutoDisposable implements AutoCloseable {

  private final Disposable disposable;

  /**
   * Wrap disposable
   *
   * @param disposable wrapped object
   */
  public AutoDisposable(Disposable disposable) {
    this.disposable = disposable;
  }

  @Override
  public void close() {
    disposable.dispose();
  }
}
