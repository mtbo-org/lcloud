/* (C) 2025 Vladimir E. (PROGrand) Koltunov (mtbo.org) */

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.stream.Stream;
import org.mtbo.lcloud.discovery.udp.UdpClientConfig;
import org.mtbo.lcloud.discovery.udp.UdpDiscoveryClient;
import reactor.core.publisher.Mono;

public class ProtectedClient extends UdpDiscoveryClient {

  public ProtectedClient(UdpClientConfig config) {
    super(config);
  }

  @Override
  protected Mono<DatagramSocket> createSocket() {
    return super.createSocket();
  }

  @Override
  protected Mono<Stream<InetAddress>> getAdditionalAddresses() {
    return super.getAdditionalAddresses();
  }

  @Override
  protected boolean canSend(NetworkInterface networkInterface) {
    return super.canSend(networkInterface);
  }
}
