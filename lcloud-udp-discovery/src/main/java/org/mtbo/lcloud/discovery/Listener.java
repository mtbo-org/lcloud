package org.mtbo.lcloud.discovery;

import java.io.IOException;

/**
 * Allows to receive packets
 */
public interface Listener {
    /**
     * Receive packets
     *
     * @return packet from incoming connection
     * @throws IOException in case of incoming connection failure
     */
    Packet receive() throws IOException;
}
