/* (C) 2025 Vladimir E. (PROGrand) Koltunov (mtbo.org) */
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.net.*;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mtbo.lcloud.discovery.ClientConfig;
import org.mtbo.lcloud.discovery.udp.UdpClientConfig;
import org.mtbo.lcloud.discovery.udp.UdpDiscoveryClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class DiscoveryClientTest {
  static {
    MockLogging.setupLogging();
  }

  @BeforeAll
  public static void beforeAll() {
    UdpClientConfig config = new UdpClientConfig("testService", "testInstance", 100, 8888);
    var client = new UdpDiscoveryClient(config);
    assertNotNull(client);
  }

  private static boolean isSetMatch(Set<String> strings) {
    return strings.equals(Set.of("2"))
        || strings.equals(Set.of("1"))
        || strings.equals(Set.of("1", "2"));
  }

  @BeforeEach
  public void beforeEach() {}

  @SuppressWarnings("MisorderedAssertEqualsArguments")
  @Test
  public void testConfigs() {
    UdpClientConfig config1 = new UdpClientConfig("testService", "testInstance", 100, 8888);
    UdpClientConfig config2 = new UdpClientConfig("testService", "testInstance", 100, 8888);
    UdpClientConfig config21 = new UdpClientConfig("testService", "testInstance", 8889);
    UdpClientConfig config3 = new UdpClientConfig("testService1", "testInstance", 8881);

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

    ClientConfig config4 = new ClientConfig("testService1", "testInstance", 100) {};

    assertNotEquals(config4, config3);
    assertNotEquals(config4, null);
  }

  @Test
  public void testBufferTimeout() {

    StepVerifier.withVirtualTime(
            () ->
                Mono.just("Hello")
                    .delayElement(Duration.ofMillis(10))
                    .repeat()
                    .bufferTimeout(100, Duration.ofMillis(100))
                    .map(Set::copyOf)
                    .take(Duration.ofMillis(150)))
        .expectSubscription()
        .thenAwait(Duration.ofMillis(250))
        .expectNext(Set.of("Hello"))
        .verifyComplete();
  }

  @Test()
  public void testCreateSocket() {
    var client = new ProtectedClient(new UdpClientConfig("testService", "testInstance", 8888));

    assertInstanceOf(DatagramSocket.class, client.createSocket().block());
  }

  @Test
  public void testGetAdditionalAddresses() {
    var client = new ProtectedClient(new UdpClientConfig("testService", "testInstance", 8888));

    assertDoesNotThrow(client::getAdditionalAddresses);
  }

  @Test
  public void testCanCheckBadInterfaces() throws SocketException {
    var client = new ProtectedClient(new UdpClientConfig("testService", "testInstance", 8888));

    var badDownLoopback = Mockito.mock(NetworkInterface.class);
    doReturn(true).when(badDownLoopback).isLoopback();
    doReturn(false).when(badDownLoopback).isUp();

    assertFalse(() -> client.canSend(badDownLoopback));

    var badUpLoopback = Mockito.mock(NetworkInterface.class);
    doReturn(true).when(badUpLoopback).isLoopback();
    doReturn(true).when(badUpLoopback).isUp();

    assertFalse(() -> client.canSend(badUpLoopback));

    var badDownNoLoopback = Mockito.mock(NetworkInterface.class);
    doReturn(true).when(badDownNoLoopback).isLoopback();
    doReturn(true).when(badDownNoLoopback).isUp();

    assertFalse(() -> client.canSend(badDownNoLoopback));

    var throwing = Mockito.mock(NetworkInterface.class);
    doThrow(new SocketException("test")).when(throwing).isLoopback();
    doThrow(new SocketException("test")).when(throwing).isUp();

    assertFalse(() -> client.canSend(throwing));
  }

  @Test
  public void testLookupOnce() {
    var client = spy(MockClient.class);

    assertNotNull(client);

    StepVerifier.withVirtualTime(() -> client.lookupOnce(Duration.ofSeconds(1)))
        .expectSubscription()
        .thenAwait(Duration.ofSeconds(2))
        .expectNextMatches(DiscoveryClientTest::isSetMatch)
        .verifyComplete();
  }

  @Test
  public void testSocketFailingOnClose() {
    var client = spy(MockClientFailOnClose.class);

    assertNotNull(client);

    StepVerifier.withVirtualTime(() -> client.lookupOnce(Duration.ofSeconds(1)))
        .expectSubscription()
        .thenAwait(Duration.ofSeconds(2))
        .expectNextMatches(DiscoveryClientTest::isSetMatch)
        .verifyComplete();
  }

  @Test
  public void testSocketFailingOnSend() {
    var client = spy(MockClientFailOnSend.class);

    assertNotNull(client);

    StepVerifier.withVirtualTime(
            () ->
                client
                    .startLookup(Duration.ofMillis(900))
                    .delayUntil(strings -> Mono.delay(Duration.ofMillis(100)).thenReturn(strings))
                    .take(3))
        .expectSubscription()
        .thenAwait(Duration.ofMillis(3900))
        .expectNextMatches(DiscoveryClientTest::isSetMatch)
        .expectNextMatches(DiscoveryClientTest::isSetMatch)
        .expectNextMatches(DiscoveryClientTest::isSetMatch)
        .verifyComplete();
  }

  @Test
  public void testSocketFailingOnReceive() {
    var client = spy(MockClientFailOnReceive.class);

    assertNotNull(client);

    StepVerifier.withVirtualTime(() -> client.startLookup(Duration.ofMillis(900)).take(3))
        .expectSubscription()
        .thenAwait(Duration.ofMillis(3900))
        .expectNextMatches(DiscoveryClientTest::isSetMatch)
        .expectNextMatches(DiscoveryClientTest::isSetMatch)
        .expectNextMatches(DiscoveryClientTest::isSetMatch)
        .verifyComplete();
  }

  @Test
  public void testLookup() {
    var client = spy(MockClient.class);

    assertNotNull(client);

    StepVerifier.withVirtualTime(() -> client.startLookup(Duration.ofMillis(900)).take(3))
        .expectSubscription()
        .thenAwait(Duration.ofMillis(3900))
        .expectNextMatches(DiscoveryClientTest::isSetMatch)
        .expectNextMatches(DiscoveryClientTest::isSetMatch)
        .expectNextMatches(DiscoveryClientTest::isSetMatch)
        .verifyComplete();
  }
}
