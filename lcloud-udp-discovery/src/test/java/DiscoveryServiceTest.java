/* (C) 2025 Vladimir E. (PROGrand) Koltunov (mtbo.org) */
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.net.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mtbo.lcloud.discovery.ServiceConfig;
import org.mtbo.lcloud.discovery.udp.UdpConnection;
import org.mtbo.lcloud.discovery.udp.UdpDiscoveryService;
import org.mtbo.lcloud.discovery.udp.UdpPacket;
import org.mtbo.lcloud.discovery.udp.UdpServiceConfig;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class DiscoveryServiceTest {

  static {
    MockLogging.setupLogging();
  }

  //    static Flux<String> source;

  @BeforeAll
  public static void beforeAll() {}

  @BeforeEach
  public void beforeEach() {}

  UdpServiceConfig serviceConfig() {
    return new UdpServiceConfig("testService", "testInstance", 8888);
  }

  @SuppressWarnings("MisorderedAssertEqualsArguments")
  @Test
  public void testConfigs() {
    UdpServiceConfig config1 = new UdpServiceConfig("testService", "testInstance", 8888);
    UdpServiceConfig config2 = new UdpServiceConfig("testService", "testInstance", 8888);
    UdpServiceConfig config21 = new UdpServiceConfig("testService", "testInstance", 8889);
    UdpServiceConfig config3 = new UdpServiceConfig("testService1", "testInstance", 8881);

    assertEquals(config1, config2);
    assertNotEquals(config1, config3);
    assertNotEquals(config2, config21);

    assertEquals(config1.toString(), config2.toString());
    assertNotEquals(config1.toString(), config3.toString());

    assertEquals(config1.hashCode(), config2.hashCode());
    assertNotEquals(config1.hashCode(), config3.hashCode());

    assertNotEquals(config1, new Object());
    assertNotEquals(config1, null);

    assertNotEquals(config1, null);

    ServiceConfig config4 = new ServiceConfig("testService1", "testInstance") {};

    assertNotEquals(config4, config3);
    assertNotEquals(config4, null);
  }

  @Test
  public void testConstructor() {
    UdpConnection udpConnection = new UdpConnection(8888);
    assertNotNull(udpConnection);
  }

  @SuppressWarnings({
    "BlockingMethodInNonBlockingContext",
    "ReactiveStreamsNullableInLambdaInTransform"
  })
  @Test
  public void testConnection() {
    var connection = Mockito.mock(UdpConnection.class);

    doCallRealMethod().when(connection).allocPacket();
    doCallRealMethod().when(connection).setupLogger();
    connection.setupLogger();
    doCallRealMethod().when(connection).close(Mockito.any(DatagramSocket.class));

    assertInstanceOf(DatagramPacket.class, connection.allocPacket());

    var realConnection = new UdpConnection(8888);
    SocketAddress localSocketAddress =
        Mono.using(
                () -> realConnection.socket().block(),
                datagramSocket -> Mono.just(datagramSocket.getLocalSocketAddress()),
                realConnection::close,
                true)
            .block();

    assertInstanceOf(InetSocketAddress.class, localSocketAddress);
    assertEquals(8888, ((InetSocketAddress) localSocketAddress).getPort());

    assertThrows(NullPointerException.class, () -> realConnection.close(null));
  }

  @Test
  public void testPacketsProcessing() {
    UdpServiceConfig config = serviceConfig();
    var discoveryService = new UdpDiscoveryService(config);

    var connection = Mockito.mock(UdpConnection.class);
    doCallRealMethod().when(connection).setupLogger();
    connection.setupLogger();

    var random = new Random();

    AtomicInteger counter = new AtomicInteger(0);

    var messages =
        Stream.generate(
            () -> {
              final var integer = counter.getAndIncrement();
              if (0 == integer % 3) return "DISCOVERY_REQUEST testService FROM testInstance";
              else if (0 == integer % 5) return "DISCOVERY_REQUEST UNK FROM testInstance1";
              else
                return IntStream.range(1, 1 + random.nextInt(100))
                    .boxed()
                    .map(integer1 -> random.nextInt(256))
                    .map(aByte -> "" + ((char) aByte.intValue()))
                    .collect(Collectors.joining());
            });

    when(connection.socket())
        .thenReturn(Mono.fromCallable(() -> MockSocket.mockDatagramSocket(messages, true, true)));

    doCallRealMethod().when(connection).receive(Mockito.any(DatagramSocket.class));
    doCallRealMethod().when(connection).allocPacket();
    doCallRealMethod()
        .when(connection)
        .sendMessage(Mockito.any(DatagramSocket.class), Mockito.any(UdpPacket.class));

    //////////////////////////////

    final var total = 20;

    var al = new ArrayList<Boolean>();
    for (int n = 0; n < total; n++) {
      final var expected = 0 == n % 3;
      al.add(expected);
    }

    StepVerifier.withVirtualTime(
            () -> discoveryService.listen(connection).take(total).collect(Collectors.toList()))
        .expectSubscription()
        .expectNext(al)
        .verifyComplete();
  }
}
