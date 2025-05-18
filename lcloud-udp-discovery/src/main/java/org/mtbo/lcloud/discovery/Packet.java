/* (C) 2025 Vladimir E. Koltunov (mtbo.org) */

package org.mtbo.lcloud.discovery;

import java.nio.ByteBuffer;

/** Piece of data */
public interface Packet<PacketType> {

  /**
   * @return wrapped data
   */
  ByteBuffer data();

  /**
   * @param wrap new data
   * @return new packet with data replaced. Remaining field should be copied.
   */
  PacketType copyWithData(ByteBuffer wrap);
}
