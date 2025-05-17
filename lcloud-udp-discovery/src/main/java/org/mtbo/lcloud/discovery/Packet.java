package org.mtbo.lcloud.discovery;

import java.nio.ByteBuffer;

/**
 * Piece of data
 */

public interface Packet {

    /**
     * @return wrapped data
     */
    ByteBuffer data();

    /**
     * @param wrap new data
     * @return new packet with data replaced. Remaining field should be copied.
     */
    Packet copyWithData(ByteBuffer wrap);
}