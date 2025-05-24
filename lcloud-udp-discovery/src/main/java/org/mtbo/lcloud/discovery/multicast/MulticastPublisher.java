/* (C) 2025 Vladimir E. (PROGrand) Koltunov (mtbo.org) */

package org.mtbo.lcloud.discovery.multicast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.Duration;
import org.mtbo.lcloud.discovery.logging.FileLineLogger;

public class MulticastPublisher implements Runnable {

  final FileLineLogger logger =
      FileLineLogger.getLogger(MulticastPublisher.class.getName(), "CLI >>>  ");

  private final Config config;

  public MulticastPublisher(Config config) {
    this.config = config;
  }

  @Override
  public void run() {
    try (var socket = new DatagramSocket()) {
      var group = InetAddress.getByName(config.addr);

      final var message =
          "LC_DISCOVERY "
              + config.serviceName.replace(" ", "_")
              + " FROM "
              + config.instanceName.replace(" ", "_");

      while (!Thread.currentThread().isInterrupted()) {
        var buf = message.getBytes();
        var packet = new DatagramPacket(buf, buf.length, group, config.port);
        logger.finer(message);
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
