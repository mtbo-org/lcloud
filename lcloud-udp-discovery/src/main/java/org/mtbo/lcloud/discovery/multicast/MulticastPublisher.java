package org.mtbo.lcloud.discovery.multicast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class MulticastPublisher implements Runnable {

  private final String addr;
  private final int port;

  public MulticastPublisher(String addr, int port) {
    this.addr = addr;
    this.port = port;
  }

  @Override
  public void run() {
    try (DatagramSocket socket = new DatagramSocket()) {
      InetAddress group = InetAddress.getByName(addr);
      int n = 0;
      while (!Thread.currentThread().isInterrupted()) {

        var buf = ("ping" + n++).getBytes();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, group, port);
        socket.send(packet);
        Thread.sleep(1000);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
