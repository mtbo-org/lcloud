/* (C) 2025 Vladimir E. Koltunov (mtbo.org) */

package org.mtbo.lcloud.discovery;

/**
 * Wrap address to socket pair.
 *
 * @param address address from which send
 * @param socket socket through which send
 */
public record AddressSocket<AddressType, SocketType>(AddressType address, SocketType socket) {}
