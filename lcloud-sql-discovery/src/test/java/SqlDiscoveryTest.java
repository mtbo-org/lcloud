/* (C) 2025 Vladimir E. (PROGrand) Koltunov (mtbo.org) */

import static org.junit.jupiter.api.Assertions.*;

import io.r2dbc.spi.ConnectionFactories;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mtbo.lcloud.discovery.sql.SqlDiscovery;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

public class SqlDiscoveryTest {

  public static final String SERVICE_NAME = "service";
  public static final String INSTANCE_NAME = "first";
  static final String connectionString =
      "r2dbc:h2:mem:///test?options=DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
  static SqlDiscovery.Config config;

  @BeforeAll
  static void setUpBeforeClass() {

    config = createConfig(SERVICE_NAME, INSTANCE_NAME);
  }

  private static SqlDiscovery.Config createConfig(String serviceName, String instanceName) {
    return new SqlDiscovery.Config(
        connectionString, serviceName, instanceName, Duration.ofMillis(50));
  }

  @BeforeEach
  void setUp() {
    var connectionFactory = ConnectionFactories.get(connectionString);

    R2dbcEntityTemplate template = new R2dbcEntityTemplate(connectionFactory);

    template
        .getDatabaseClient()
        .sql("drop table if exists instances")
        .fetch()
        .rowsUpdated()
        .as(StepVerifier::create)
        .expectNextCount(1)
        .verifyComplete();
  }

  @Test
  public void testCanInitialize() {

    var discovery = new SqlDiscovery(config);

    StepVerifier.withVirtualTime(discovery::initialize).expectNext(true).verifyComplete();
  }

  @Test
  public void testCanPing() {

    var discovery = new SqlDiscovery(config);

    discovery.initialize().block();

    assertEquals(Boolean.TRUE, discovery.ping(Mono::error).take(1).blockLast());
  }

  @Test
  public void testCanLookupSelf() {

    var discovery = new SqlDiscovery(config);

    discovery.initialize().block();

    var pinger = discovery.ping(Mono::error).subscribe();

    assertEquals(Set.of("first"), discovery.lookup(Mono::error).take(1).blockLast());

    pinger.dispose();
  }

  @Test
  public void testCanClean() {

    var discovery = new SqlDiscovery(config);

    discovery.initialize().block();

    discovery.ping(Mono::error).take(1).blockLast();

    assertEquals(0, discovery.clean(Mono::error).block());

    assertEquals(
        1, Mono.delay(Duration.ofMillis(100)).flatMap(x -> discovery.clean(Mono::error)).block());

    assertEquals(Set.of(), discovery.lookup(Mono::error).take(1).blockLast());
  }

  @Test
  public void testCanCleanAll() {

    var configOther = createConfig(SERVICE_NAME, "other");

    var discovery1 = new SqlDiscovery(config);
    var discovery2 = new SqlDiscovery(configOther);

    Flux.concat(
            discovery1.initialize().publishOn(Schedulers.boundedElastic()),
            discovery2.initialize().publishOn(Schedulers.boundedElastic()))
        .blockLast();

    Flux.concat(
            discovery1.ping(Mono::error).take(1).publishOn(Schedulers.boundedElastic()),
            discovery2.ping(Mono::error).take(1).publishOn(Schedulers.boundedElastic()))
        .blockLast();

    assertEquals(0, discovery1.cleanAll(Mono::error).block());

    assertEquals(
        2,
        Mono.delay(Duration.ofMillis(100)).flatMap(x -> discovery1.cleanAll(Mono::error)).block());

    assertEquals(Set.of(), discovery1.lookup(Mono::error).take(1).blockLast());
    assertEquals(Set.of(), discovery2.lookup(Mono::error).take(1).blockLast());
  }

  @Test
  public void testCanLookupOther() {

    var discovery = new SqlDiscovery(config);

    var configOther = createConfig(SERVICE_NAME, "other");

    var discoveryOther = new SqlDiscovery(configOther);

    discovery.initialize().block();
    discoveryOther.initialize().block();

    var pinger = discovery.ping(Mono::error).subscribe();
    var pingerOther = discoveryOther.ping(Mono::error).subscribe();

    assertEquals(Set.of(INSTANCE_NAME, "other"), discovery.lookup(Mono::error).take(1).blockLast());

    pinger.dispose();
    pingerOther.dispose();
  }

  @Test
  public void testDoesNotInterfere() {

    var discovery = new SqlDiscovery(config);

    var configOther = createConfig("other", INSTANCE_NAME);

    var discoveryOther = new SqlDiscovery(configOther);

    discovery.initialize().block();

    var pinger = discovery.ping(Mono::error).subscribe();
    var pingerOther = discoveryOther.ping(Mono::error).subscribe();

    assertEquals(Set.of(INSTANCE_NAME), discovery.lookup(Mono::error).take(1).blockLast());

    pinger.dispose();
    pingerOther.dispose();
  }
}
