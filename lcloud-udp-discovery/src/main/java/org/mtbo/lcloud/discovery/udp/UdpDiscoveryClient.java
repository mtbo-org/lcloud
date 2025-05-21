/* (C) 2025 Vladimir E. (PROGrand) Koltunov (mtbo.org) */
package org.mtbo.lcloud.discovery.udp;

import java.net.*;
import java.util.Objects;
import java.util.logging.Level;
import java.util.stream.Stream;
import org.mtbo.lcloud.discovery.DiscoveryClient;
import org.mtbo.lcloud.discovery.logging.FileLineLogger;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/** UDP DiscoveryClient implementation */
public class UdpDiscoveryClient
    extends DiscoveryClient<InetAddress, DatagramSocket, DatagramPacket> {

  final FileLineLogger logger = FileLineLogger.getLogger(UdpDiscoveryClient.class.getName());

  /**
   * Construct client with config
   *
   * @param config configuration, including {@link UdpClientConfig#serviceName service name} and
   *     {@link UdpClientConfig#serverPort}
   */
  public UdpDiscoveryClient(UdpClientConfig config) {
    super(config);
  }

  @Override
  protected Mono<DatagramSocket> createSocket() {
    return Mono.fromCallable(
            () -> {
              var datagramSocket = new DatagramSocket(null);
              datagramSocket.setReuseAddress(true);
              //              datagramSocket.setBroadcast(true);
              datagramSocket.bind(
                  new InetSocketAddress(
                      InetAddress.getByName("0.0.0.0"), ((UdpClientConfig) config).clientPort));
              return datagramSocket;
            })
        .publishOn(Schedulers.boundedElastic());
  }

  @Override
  protected Mono<DatagramSocket> sendPacket(DatagramSocket socket, DatagramPacket sendPacket) {
    return Mono.fromCallable(
            () -> {
              try {
                if (logger.isLoggable(Level.FINEST)) {
                  logger.finest(
                      "client send broadcast ["
                          + new String(
                              sendPacket.getData(), sendPacket.getOffset(), sendPacket.getLength())
                          + "]");
                }
                socket.send(sendPacket);
              } catch (Throwable e) {
                try {
                  socket.close();
                  logger.fine("Socket Closed error: " + e);
                } catch (Throwable ex) {
                  logger.severe("Socket Closed FATAL error: " + ex + "\n\t>>>" + e);
                }

                return null;
              }
              return socket;
            })
        .publishOn(Schedulers.boundedElastic());
  }

  @Override
  protected DatagramPacket createSendPacket(byte[] sendData, InetAddress address) {
    return new DatagramPacket(
        sendData, sendData.length, address, ((UdpClientConfig) config).serverPort);
  }

  @Override
  protected Mono<DatagramPacket> receivePacket(
      DatagramSocket socket, DatagramPacket receivePacket) {
    return Mono.fromCallable(
            () -> {
              try {
                socket.receive(receivePacket);
              } catch (Throwable e) {
                if (e instanceof SocketException
                    && (e.getMessage().equals("Socket closed")
                        || e.getMessage().equals("Closed by interrupt"))) {
                  logger.finer("CLIENT SOCKET ERROR: " + e, e);
                } else {
                  logger.severe(
                      "CLIENT SOCKET ERROR: >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n" + e, e);
                }

                return null;
              }
              return receivePacket;
            })
        .publishOn(Schedulers.boundedElastic());
  }

  @Override
  protected DatagramPacket createReceivePacket() {
    var buffer = new byte[4096];
    return new DatagramPacket(buffer, buffer.length);
  }

  @Override
  protected String packetMessage(DatagramPacket receivedPacket) {
    return new String(
        receivedPacket.getData(), receivedPacket.getOffset(), receivedPacket.getLength());
  }

  @Override
  protected String packetAddress(DatagramPacket packet) {
    return packet.getAddress().getHostAddress();
  }

  /**
   * Get IPv4 network broadcast addresses
   *
   * @return cached local broadcast address
   */
  @Override
  protected Mono<InetAddress> getMainBroadcastAddresses() {
    return Mono.fromCallable(() -> InetAddress.getByName("255.255.255.255"))
        .publishOn(Schedulers.boundedElastic())
        .cache();
  }

  @Override
  protected Mono<Stream<InetAddress>> getAdditionalAddresses() {
    return getNetworkInterfaces()
        .map(
            networkInterfaceStream ->
                networkInterfaceStream
                    .filter(this::canSend)
                    .map(NetworkInterface::getInterfaceAddresses)
                    .flatMap(
                        interfaceAddresses ->
                            interfaceAddresses.stream()
                                .map(InterfaceAddress::getBroadcast)
                                .filter(Objects::nonNull)));
  }

  /**
   * Network interfaces
   *
   * @return network interfaces as {@link Stream}
   */
  private Mono<Stream<NetworkInterface>> getNetworkInterfaces() {
    return Mono.fromCallable(NetworkInterface::networkInterfaces)
        .publishOn(Schedulers.boundedElastic())
        .onErrorResume(ignored -> Mono.just(Stream.empty()));
  }

  /**
   * Is network interface able to send packets
   *
   * @param networkInterface to be checked
   * @return able to send
   */
  protected boolean canSend(NetworkInterface networkInterface) {
    try {
      return !networkInterface.isLoopback() && networkInterface.isUp();
    } catch (Throwable ignored) {
      logger.finer("interface is not allowed to send packets: " + networkInterface);
      return false;
    }
  }
}
