/* (C) 2025 Vladimir E. (PROGrand) Koltunov (mtbo.org) */

package org.mtbo.lcloud.discovery.multicast;

import java.net.*;

public class MulticastDiscovery {

  private static String getSelfIP() throws UnknownHostException {
    InetAddress inetAddress = InetAddress.getLocalHost();
    return inetAddress.getHostAddress();
  }

  public static class MulticastReceiver implements Runnable {

    private final byte[] buf = new byte[256];

    private final String addr;
    private final int port;

    public MulticastReceiver(String addr, int port) {
      this.addr = addr;
      this.port = port;
    }

    @Override
    public void run() {
      try (MulticastSocket socket = new MulticastSocket(port)) {
        String selfIp = getSelfIP();
        InetAddress group = InetAddress.getByName(addr);
        //        InetSocketAddress socketAddress = new InetSocketAddress(group, port);
        socket.joinGroup(group);

        while (!Thread.currentThread().isInterrupted()) {
          DatagramPacket packet = new DatagramPacket(buf, buf.length);
          socket.receive(packet);
          String received = new String(packet.getData(), 0, packet.getLength());

          if (!packet.getAddress().getHostAddress().equals(selfIp)) {
            System.out.printf(
                "[%s] received '%s' from %s:%d%n",
                selfIp, received, packet.getAddress(), packet.getPort());

            if ("end".equals(received)) {
              break;
            }
          }
        }

        socket.leaveGroup(group);

      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  public static class MulticastPublisher implements Runnable {

    private static final byte[] MESSAGE_BUF = "ping".getBytes();
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
        while (!Thread.currentThread().isInterrupted()) {
          DatagramPacket packet = new DatagramPacket(MESSAGE_BUF, MESSAGE_BUF.length, group, port);
          socket.send(packet);
          Thread.sleep(1000);
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
}
