package org.mtbo.lcloud.discovery;

import java.io.IOException;

/**
 * Allows to send packets
 */
public interface Sender {
    /**
     * Send packet
     * @param packet packet with data and connection parameters
     * @throws IOException on connection failure
     */
    void send(Packet packet) throws IOException;
}
