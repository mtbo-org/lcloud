/* (C) 2025 Vladimir E. (PROGrand) Koltunov (mtbo.org) */
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.Duration;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.mtbo.lcloud.discovery.udp.UdpClientConfig;
import org.mtbo.lcloud.discovery.udp.UdpDiscoveryClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class MockClient extends UdpDiscoveryClient {

  private final boolean socketCloseShouldFail;
  private final boolean socketSendShouldFail;
  private final boolean socketReceiveShouldFail;

  public MockClient() {
    this(false, false, false);
  }

  public MockClient(boolean failOnClose, boolean failOnSend, boolean failOnReceive) {
    super(new UdpClientConfig("testService", "testInstance", 100, 8888));
    this.socketCloseShouldFail = failOnClose;
    this.socketSendShouldFail = failOnSend;
    this.socketReceiveShouldFail = failOnReceive;
  }

  @Override
  protected Mono<Stream<InetAddress>> getAdditionalAddresses() {
    return Mono.just(Stream.of(InetAddress.getLoopbackAddress()))
        .publishOn(Schedulers.boundedElastic());
  }

  @Override
  public Mono<DatagramSocket> createSocket() {
    var random = new Random();

    AtomicInteger counter = new AtomicInteger(0);

    var messages =
        Stream.generate(
            () -> {
              final var integer = counter.getAndIncrement();
              if (0 == integer % 2) return "DISCOVERY_RESPONSE testService FROM 1";
              else if (0 == integer % 3) return "DISCOVERY_RESPONSE testService FROM 2";
              else
                return IntStream.range(1, 1 + random.nextInt(100))
                    .boxed()
                    .map(integer1 -> random.nextInt(256))
                    .map(aByte -> "" + ((char) aByte.intValue()))
                    .collect(Collectors.joining());
            });

    return Mono.fromCallable(
            () ->
                MockSocket.mockDatagramSocket(
                    messages, socketSendShouldFail, socketCloseShouldFail, socketReceiveShouldFail))
        .publishOn(Schedulers.boundedElastic());
  }

  @Override
  protected Mono<DatagramPacket> receivePacket(
      DatagramSocket socket, DatagramPacket receivePacket) {
    return Mono.delay(Duration.ofMillis(50))
        .flatMap(aLong -> super.receivePacket(socket, receivePacket));
  }
}
