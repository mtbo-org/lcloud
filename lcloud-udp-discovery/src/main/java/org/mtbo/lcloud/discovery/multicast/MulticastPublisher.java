/* (C) 2025 Vladimir E. (PROGrand) Koltunov (mtbo.org) */

package org.mtbo.lcloud.discovery.multicast;

import java.io.IOException;
import java.net.*;
import java.time.Duration;
import java.util.logging.Level;
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

    try {
      //      var interfaces =
      //          NetworkInterface.networkInterfaces()
      //              .filter(
      //                  networkInterface -> {
      //                    try {
      //                      return networkInterface.supportsMulticast()
      //                          && networkInterface.isUp()
      //                          && !networkInterface.isVirtual()
      //                          && !networkInterface.isLoopback()
      //                          && networkInterface.supportsMulticast()
      //                          && !networkInterface.isPointToPoint()
      //                          && networkInterface.inetAddresses().findAny().isPresent();
      //                    } catch (SocketException e) {
      //                      return false;
      //                    }
      //                  })
      //              .toList();
      //
      //      try (ExecutorService service = Executors.newFixedThreadPool(interfaces.size())) {
      //
      //        for (var intf : interfaces) {
      final var message =
          "LC_DISCOVERY "
              + config.serviceName.replace(" ", "_")
              + " FROM "
              + config.instanceName.replace(" ", "_");
      var buf = message.getBytes();
      //
      //          intf.getInterfaceAddresses().stream()
      //              .map(InterfaceAddress::getAddress)
      //              .forEach(
      //                  inetAddress -> {
      //                    var group = new InetSocketAddress(inetAddress, config.port);
      InetAddress group = InetAddress.getByName(config.addr);
      var packet = new DatagramPacket(buf, buf.length, group, config.port);

      //                    service.submit(
      //                        () -> {
      try (var socket = new DatagramSocket(null)) {
        while (!Thread.currentThread().isInterrupted()) {
          if (logger.isLoggable(Level.FINEST)) {
            logger.finest(message /* + " on " + intf*/);
          }

          try {
            socket.send(packet);
          } catch (IOException e) {
            logger.finer("send error: " + e, e);
          }

          try {
            Thread.sleep(config.interval);
          } catch (InterruptedException e) {
            break;
          }
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      //                        });
      //                  });
      //        }
      //      }

    } catch (UnknownHostException e) {
      throw new RuntimeException(e);
    }
  }

  public record Config(
      String serviceName, String instanceName, String addr, int port, Duration interval) {}
}
