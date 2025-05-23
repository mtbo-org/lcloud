package org.mtbo.lcloud.discovery.multicast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.Duration;

public class MulticastPublisher implements Runnable {

  private final Config config;

  public MulticastPublisher(Config config) {
    this.config = config;
  }

  @Override
  public void run() {
    try (DatagramSocket socket = new DatagramSocket()) {
      InetAddress group = InetAddress.getByName(config.addr);
      int n = 0;
      while (!Thread.currentThread().isInterrupted()) {

        var buf = ("ping" + n++).getBytes();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, group, config.port);
        socket.send(packet);
        Thread.sleep(config.interval);
      }
    } catch (InterruptedException ignored) {
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public record Config(
      String serviceName, String instanceName, String addr, int port, Duration interval) {}
}
