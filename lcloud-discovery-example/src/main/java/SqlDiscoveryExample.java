/* (C) 2025 Vladimir E. (PROGrand) Koltunov (mtbo.org) */

import java.time.Duration;
import java.util.Calendar;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import org.mtbo.lcloud.discovery.sql.AutoDisposable;
import org.mtbo.lcloud.discovery.sql.SqlDiscovery;
import org.mtbo.lcloud.logging.FileLineLogger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import sun.misc.Signal;
import sun.misc.SignalHandler;

/**
 * Demo application
 *
 * <p>Ping network in 5 seconds
 */
public class SqlDiscoveryExample {

  static FileLineLogger logger;

  static {
    FileLineLogger.init(SqlDiscoveryExample.class);

    logger = FileLineLogger.getLogger(SqlDiscoveryExample.class.getName());
  }

  /** Default */
  SqlDiscoveryExample() {}

  /**
   * Main method
   *
   * @param args arguments
   * @throws InterruptedException in case of interruption
   */
  public static void main(String[] args) throws InterruptedException {

    final var serviceName = Optional.ofNullable(System.getenv("SERVICE_NAME")).orElse("lcloud");

    final var instanceName =
        Optional.ofNullable(System.getenv("HOSTNAME")).orElse(UUID.randomUUID().toString());

    final var connectionString =
        Optional.ofNullable(System.getenv("CONNECTION_STRING"))
            .orElse("r2dbc:postgresql://user:user@localhost:5432/demo");

    var discovery =
        new SqlDiscovery(
            new SqlDiscovery.Config(
                connectionString, serviceName, instanceName, Duration.ofMillis(500)));

    if (Boolean.FALSE.equals(
        discovery
            .initialize()
            .onErrorResume(
                (Throwable throwable) -> {
                  logger.severe("Unable to connect to db" /*, throwable*/);
                  return Mono.just(false);
                })
            .block())) {
      return;
    }

    discovery.cleanAll(Mono::error).block();

    var latch = new CountDownLatch(1);

    @SuppressWarnings("ReactiveStreamsUnusedPublisher")
    final var ping =
        discovery
            .ping(
                (Throwable throwable) -> {
                  logger.severe("Ping error: " + throwable.getMessage(), throwable);
                  return Mono.empty();
                })
            .repeat();
    @SuppressWarnings("ReactiveStreamsUnusedPublisher")
    final var lookup =
        discovery
            .lookup(
                (Throwable throwable) -> {
                  logger.severe("Lookup error: " + throwable.getMessage(), throwable);
                  return Mono.empty();
                })
            .doOnNext(
                (Set<String> instances) -> {
                  synchronized (FileLineLogger.class) {
                    logger.info("****************************************************");
                    logger.info(
                        String.format(
                            "[%1$tH:%<tM:%<tS.%<tL] %2$s instances are discovered [%3$3d]",
                            Calendar.getInstance(), serviceName, instances.size()));
                    instances.stream()
                        .sorted()
                        .forEach(
                            (String message) -> logger.info(String.format("%1$-52s", message)));
                    logger.info("****************************************************");
                  }
                });
    @SuppressWarnings("ReactiveStreamsUnusedPublisher")
    final var gc =
        Flux.interval(Duration.ofSeconds(1))
            .flatMap(
                (Long aLong) -> {
                  System.gc();
                  return Mono.just(true);
                });
    try (final var ignored = new AutoDisposable(Flux.merge(ping, lookup, gc).subscribe())) {
      // endregion

      SignalHandler handler =
          (Signal sig) -> {
            logger.info("Shutting down...");

            latch.countDown();
          };

      Signal.handle(new Signal("INT"), handler);

      logger.info("Program started. Press Ctrl+C to test the blocker.");

      latch.await();
    }

    discovery.clean().block();

    logger.info("Bye!");
  }
}
