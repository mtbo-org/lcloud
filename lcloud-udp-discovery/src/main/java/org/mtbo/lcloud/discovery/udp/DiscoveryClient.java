/* (C) 2025 Vladimir E. Koltunov (mtbo.org) */

package org.mtbo.lcloud.discovery.udp;

import java.io.IOException;
import java.net.*;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public class DiscoveryClient {

  private final String serviceName;

  private final int port;

  public DiscoveryClient(String serviceName, int port) {
    this.serviceName = serviceName;
    this.port = port;
  }

  public Set<String> lookup() throws InterruptedException, SocketException {
    try (DatagramSocket socket = new DatagramSocket()) {
      // Open a random port to send the package
      socket.setBroadcast(true);

      var sendData = ("UDP_DISCOVERY_REQUEST " + serviceName).getBytes();

      // Try the 255.255.255.255 first
      try {
        var sendPacket =
            new DatagramPacket(
                sendData, sendData.length, InetAddress.getByName("255.255.255.255"), port);
        socket.send(sendPacket);
        logger.finer(">>> Request packet sent to: 255.255.255.255 (DEFAULT)");
      } catch (Exception ignored) {
      }

      // Broadcast the message over all the network interfaces
      var interfaces = NetworkInterface.getNetworkInterfaces();

      while (interfaces.hasMoreElements()) {
        var networkInterface = interfaces.nextElement();

        if (networkInterface.isLoopback() || !networkInterface.isUp()) {
          continue; // Don't want to broadcast to the loopback interface
        }

        for (var interfaceAddress : networkInterface.getInterfaceAddresses()) {
          var broadcast = interfaceAddress.getBroadcast();
          if (broadcast == null) {
            continue;
          }

          // Send the broadcast package!
          try {
            var sendPacket = new DatagramPacket(sendData, sendData.length, broadcast, port);
            socket.send(sendPacket);
          } catch (Exception ignored) {
          }

          logger.finer(
              ">>> Request packet sent to: "
                  + broadcast.getHostAddress()
                  + "; Interface: "
                  + networkInterface.getDisplayName());
        }
      }

      logger.finer(">>> Done looping over all network interfaces. Now waiting for a reply!");

      var result = new HashSet<String>();

      var th = getThread(socket, result);
      th.join(1000);

      return result;
    }
  }

  private Thread getThread(DatagramSocket socket, Set<String> result) {
    result.clear();

    var th =
        new Thread(
            () -> {
              var buffer = new byte[15000];

              while (!Thread.interrupted()) {
                var receivePacket = new DatagramPacket(buffer, buffer.length);

                try {
                  socket.receive(receivePacket);
                } catch (IOException e) {
                  break;
                }

                var message =
                    new String(
                        receivePacket.getData(),
                        receivePacket.getOffset(),
                        receivePacket.getLength());

                logger.fine(
                    ">>> Broadcast response from: "
                        + receivePacket.getAddress().getHostAddress()
                        + ", ["
                        + message
                        + "]");

                if (message.startsWith("UDP_DISCOVERY_RESPONSE ")) {
                  result.add(message.substring("UDP_DISCOVERY_RESPONSE ".length()));
                }
              }
            });

    th.start();
    return th;
  }

  static final Logger logger = Logger.getLogger(DiscoveryClient.class.getName());
}
