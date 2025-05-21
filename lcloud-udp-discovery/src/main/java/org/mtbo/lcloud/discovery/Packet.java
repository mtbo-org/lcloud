/* (C) 2025 Vladimir E. (PROGrand) Koltunov (mtbo.org) */
package org.mtbo.lcloud.discovery;

import java.nio.ByteBuffer;

/**
 * Piece of data
 *
 * @param <PacketType> socket type, ex: {@link java.net.DatagramPacket DatagramPacket}
 */
public interface Packet<PacketType> {

  /**
   * Wrapped byte buffer
   *
   * @return wrapped data
   */
  ByteBuffer data();

  /**
   * Create duplicate
   *
   * @param wrap new data
   * @return new packet with data replaced. Remaining field should be copied.
   */
  PacketType copyWithData(ByteBuffer wrap);
}
