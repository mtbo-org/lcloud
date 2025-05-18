/* (C) 2025 Vladimir E. Koltunov (mtbo.org) */

package org.mtbo.lcloud.discovery.udp;

import java.net.*;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.mtbo.lcloud.discovery.DiscoveryClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/** UDP DiscoveryClient implementation */
public class UdpDiscoveryClient
    extends DiscoveryClient<InetAddress, DatagramSocket, DatagramPacket> {

  /**
   * Construct client with config
   *
   * @param config configuration, including {@link Config#serviceName service name} and {@link
   *     UdpConfig#port}
   */
  public UdpDiscoveryClient(UdpConfig config) {
    super(config);
  }

  @Override
  protected Mono<DatagramSocket> createSocket() {
    return Mono.fromCallable(DatagramSocket::new).publishOn(Schedulers.boundedElastic());
  }

  @Override
  protected Mono<DatagramSocket> sendPacket(DatagramSocket socket, DatagramPacket sendPacket) {
    return Mono.fromCallable(
            () -> {
              socket.send(sendPacket);
              return socket;
            })
        .publishOn(Schedulers.boundedElastic());
  }

  @Override
  protected DatagramPacket createSendPacket(byte[] sendData, InetAddress address) {
    return new DatagramPacket(sendData, sendData.length, address, ((UdpConfig) config).port);
  }

  @Override
  protected Mono<DatagramPacket> receivePacket(
      DatagramSocket socket, DatagramPacket receivePacket) {
    return Mono.fromCallable(
            () -> {
              socket.receive(receivePacket);
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
  protected Flux<InetAddress> getMainBroadcastAddresses() {
    return Flux.from(
            Mono.fromCallable(() -> InetAddress.getByName("255.255.255.255"))
                .publishOn(Schedulers.boundedElastic()))
        .doOnNext(inetAddress -> logger.fine("get main address (REAL): " + inetAddress.toString()))
        .cache()
        .doOnNext(
            inetAddress -> logger.fine("get main address (CACHED): " + inetAddress.toString()));
  }

  @Override
  protected Flux<InetAddress> getAdditionalAddresses() {
    return getNetworkInterfaces()
        .flatMap(Flux::fromStream)
        .filter(this::canSend)
        .flatMapIterable(NetworkInterface::getInterfaceAddresses)
        .mapNotNull(InterfaceAddress::getBroadcast);
  }

  /**
   * Network interfaces
   *
   * @return network interfaces as {@link Stream}
   */
  private Flux<Stream<NetworkInterface>> getNetworkInterfaces() {
    return Flux.from(
        Mono.fromCallable(NetworkInterface::networkInterfaces)
            .publishOn(Schedulers.boundedElastic())
            .onErrorResume(e -> Mono.just(Stream.empty())));
  }

  /**
   * Is network interface able to send packets
   *
   * @return able to send
   */
  private boolean canSend(NetworkInterface networkInterface) {
    try {
      return !networkInterface.isLoopback() && networkInterface.isUp();
    } catch (SocketException e) {
      logger.warning("interface is not allowed to send packets: " + networkInterface.getName());
      return false;
    }
  }

  /** {@link UdpDiscoveryClient UDP discovery client's} config */
  public static final class UdpConfig extends Config {
    private final int port;

    /**
     * Constructor
     *
     * @param serviceName unique service identifier
     * @param clientsCount max instances list size (default to {@link Integer#MAX_VALUE})
     * @param port UDP port
     */
    @SuppressWarnings("unused")
    public UdpConfig(String serviceName, int clientsCount, int port) {
      super(serviceName, clientsCount);
      this.port = port;
    }

    /**
     * Constructor with defaults
     *
     * @param serviceName unique service identifier
     * @param port UDP port
     */
    public UdpConfig(String serviceName, int port) {
      super(serviceName);
      this.port = port;
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;
      UdpConfig udpConfig = (UdpConfig) o;
      return port == udpConfig.port;
    }

    @Override
    public int hashCode() {
      return Objects.hash(super.hashCode(), port);
    }

    @Override
    public String toString() {
      return "UdpConfig{"
          + "port="
          + port
          + ", serviceName='"
          + serviceName
          + '\''
          + ", clientsCount="
          + clientsCount
          + '}';
    }
  }

  static final Logger logger = Logger.getLogger(UdpDiscoveryClient.class.getName());
}
