/* (C) 2025 Vladimir E. (PROGrand) Koltunov (mtbo.org) */

package org.mtbo.lcloud.discovery.udp;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import org.mtbo.lcloud.discovery.Packet;

/**
 * UDP packet implementation of {@link Packet}
 *
 * @param packet wrapped
 */
public record UdpPacket(DatagramPacket packet) implements Packet<UdpPacket> {

  @Override
  public ByteBuffer data() {
    return ByteBuffer.wrap(packet.getData(), packet.getOffset(), packet.getLength());
  }

  @Override
  public UdpPacket copyWithData(ByteBuffer wrap) {
    return new UdpPacket(
        new DatagramPacket(
            wrap.array(), wrap.arrayOffset(), wrap.limit(), packet.getAddress(), packet.getPort()));
  }

  @Override
  public String toString() {
    return packet.getAddress().getHostAddress();
  }
}
