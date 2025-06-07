/* (C) 2025 Vladimir E. (PROGrand) Koltunov (mtbo.org) */

import static org.junit.jupiter.api.Assertions.*;

import io.r2dbc.spi.ConnectionFactories;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mtbo.lcloud.discovery.sql.AutoDisposable;
import org.mtbo.lcloud.discovery.sql.SqlDiscovery;
import org.mtbo.lcloud.logging.FileLineLogger;
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
  private static final Duration quant = Duration.ofMillis(50);
  static SqlDiscovery.Config config;

  static {
    FileLineLogger.init(SqlDiscoveryTest.class);
  }

  @BeforeAll
  static void setUpBeforeClass() {

    config = createConfig(SERVICE_NAME, INSTANCE_NAME);
  }

  private static SqlDiscovery.Config createConfig(String serviceName, String instanceName) {
    return new SqlDiscovery.Config(connectionString, serviceName, instanceName, quant);
  }

  @BeforeEach
  void setUp() {
    final var connectionFactory = ConnectionFactories.get(connectionString);

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

    final var discovery = new SqlDiscovery(config);

    StepVerifier.withVirtualTime(discovery::initialize).expectNext(true).verifyComplete();
  }

  @Test
  public void testCanPing() {

    final var discovery = new SqlDiscovery(config);

    discovery.initialize().block();

    assertEquals(Boolean.TRUE, discovery.ping(Mono::error).block());
  }

  @Test
  public void testCanLookupSelf() {

    final var discovery = new SqlDiscovery(config);

    discovery.initialize().block();

    discovery.ping(Mono::error).block();

    assertEquals(Set.of("first"), discovery.lookup(Mono::error).take(1).blockLast());
  }

  @Test
  public void testCanClean() {

    final var discovery = new SqlDiscovery(config);

    discovery.initialize().block();

    try (final var ignored = new AutoDisposable(discovery.ping(Mono::error).repeat().subscribe())) {

      Mono.delay(quant.dividedBy(2)).block();

      assertEquals(0, discovery.clean().block());
    }

    Mono.delay(quant.plus(quant.dividedBy(2))).block();

    assertEquals(1, discovery.clean().block());

    assertEquals(Set.of(), discovery.lookup(Mono::error).take(1).blockLast());
  }

  @Test
  public void testCanCleanAll() {

    final var configOther = createConfig(SERVICE_NAME, "other");

    final var discovery1 = new SqlDiscovery(config);
    final var discovery2 = new SqlDiscovery(configOther);

    Flux.merge(
            discovery1.initialize().publishOn(Schedulers.boundedElastic()),
            discovery2.initialize().publishOn(Schedulers.boundedElastic()))
        .blockLast();

    @SuppressWarnings("ReactiveStreamsUnusedPublisher")
    Flux<Boolean> pinger =
        Flux.merge(discovery1.ping(Mono::error).repeat(), discovery2.ping(Mono::error).repeat())
            .publishOn(Schedulers.boundedElastic());

    try (final var ignored = new AutoDisposable(pinger.subscribe())) {

      Mono.delay(quant.dividedBy(2)).block();

      assertEquals(0, discovery1.cleanAll(Mono::error).block());
    }

    Mono.delay(quant.plus(quant.dividedBy(2))).block();

    assertEquals(2, discovery1.cleanAll(Mono::error).block());
    assertEquals(Set.of(), discovery1.lookup(Mono::error).blockFirst());
  }

  @Test
  public void testCanLookupOther() {

    final var discovery = new SqlDiscovery(config);

    final var configOther = createConfig(SERVICE_NAME, "other");

    final var discoveryOther = new SqlDiscovery(configOther);

    discovery.initialize().block();
    discoveryOther.initialize().block();

    try (final var ignored =
        new AutoDisposable(
            Flux.merge(discovery.ping(Mono::error), discoveryOther.ping(Mono::error))
                .subscribe())) {

      Mono.delay(quant.dividedBy(2)).block();

      assertEquals(
          Set.of(INSTANCE_NAME, "other"), discovery.lookup(Mono::error).take(1).blockLast());
    }
  }

  @Test
  public void testDoesNotInterfere() {

    final var discovery = new SqlDiscovery(config);

    final var configOther = createConfig("other", INSTANCE_NAME);

    final var discoveryOther = new SqlDiscovery(configOther);

    discovery.initialize().block();

    try (final var ignored =
        new AutoDisposable(
            Flux.merge(discovery.ping(Mono::error), discoveryOther.ping(Mono::error))
                .subscribe())) {

      Mono.delay(quant.dividedBy(2)).block();
      assertEquals(Set.of(INSTANCE_NAME), discovery.lookup(Mono::error).take(1).blockLast());
    }
  }
}
