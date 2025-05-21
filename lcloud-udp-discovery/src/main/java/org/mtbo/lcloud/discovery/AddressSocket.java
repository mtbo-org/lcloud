/* (C) 2025 Vladimir E. (PROGrand) Koltunov (mtbo.org) */
package org.mtbo.lcloud.discovery;

/**
 * Wrap address to socket pair.
 *
 * @param address address from which send
 * @param socket socket through which send
 * @param <AddressType> address type, ex. {@link java.net.InetAddress InetAddress}
 * @param <SocketType> socket type, ex: {@link java.net.DatagramSocket DatagramSocket}
 */
public record AddressSocket<AddressType, SocketType>(AddressType address, SocketType socket) {}
