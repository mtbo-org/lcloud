/* (C) 2025 Vladimir E. (PROGrand) Koltunov (mtbo.org) */

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Calendar;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.logging.LogManager;
import org.mtbo.lcloud.discovery.logging.FileLineLogger;
import org.mtbo.lcloud.discovery.sql.SqlDiscovery;
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
    var logFile =
        Optional.ofNullable(System.getProperty("java.util.logging.config.file")).orElse("");

    var skipConfig =
        Optional.ofNullable(System.getProperty("lcloud.skip.logging.config")).orElse("false");

    if (logFile.isEmpty() && !skipConfig.equals("true")) {
      try (var is =
          SqlDiscoveryExample.class.getClassLoader().getResourceAsStream("logging.properties")) {
        LogManager.getLogManager().readConfiguration(is);

      } catch (Throwable e) {
        e.printStackTrace();
      }
    }

    var level = Optional.ofNullable(System.getProperty("org.mtbo.lcloud.discovery.level"));

    level.ifPresent(
        s -> {
          try (var stream =
              new ByteArrayInputStream(
                  ("org.mtbo.lcloud.discovery.level=" + level.get())
                      .getBytes(StandardCharsets.UTF_8))) {
            LogManager.getLogManager()
                .updateConfiguration(
                    stream,
                    s1 -> {
                      return (o, n) -> {
                        return n != null ? n : o;
                      };
                    });
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });

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
            .orElse("r2dbc:postgresql://user:user@localhost:5444/demo");

    var discovery =
        new SqlDiscovery(
            new SqlDiscovery.Config(
                connectionString, serviceName, instanceName, Duration.ofMillis(250)));

    if (Boolean.FALSE.equals(
        discovery
            .initialize()
            .onErrorResume(
                throwable -> {
                  logger.severe("Unable to connect to db" /*, throwable*/);
                  return Mono.just(false);
                })
            .block())) {
      return;
    }

    discovery.cleanAll(Mono::error).block();

    var pingSubscription =
        discovery
            .ping(
                throwable -> {
                  logger.severe("Ping error: " + throwable.getMessage(), throwable);
                  return Mono.empty();
                })
            //            .doOnNext(System.out::println)
            .subscribe();

    var lookupSubscription =
        discovery
            .lookup(
                throwable -> {
                  logger.severe("Lookup error: " + throwable.getMessage(), throwable);
                  return Mono.empty();
                })
            .doOnNext(
                instances -> {
                  synchronized (FileLineLogger.class) {
                    logger.info("****************************************************");
                    logger.info(
                        String.format(
                            "[%1$tH:%<tM:%<tS.%<tL] %2$s instances are discovered [%3$3d]",
                            Calendar.getInstance(), serviceName, instances.size()));
                    instances.stream()
                        .sorted()
                        .forEach(message -> logger.info(String.format("%1$-52s", message)));
                    logger.info("****************************************************");
                  }
                })
            .subscribe();

    var latch = new CountDownLatch(1);

    // region Garbage collector forcing

    var gcSubscription =
        Flux.interval(Duration.ofSeconds(1))
            .flatMap(
                aLong -> {
                  System.gc();
                  return Mono.just(true);
                })
            .subscribe();
    // endregion

    SignalHandler handler =
        sig -> {
          logger.info("Shutting down...");

          latch.countDown();
        };

    Signal.handle(new Signal("INT"), handler);

    logger.info("Program started. Press Ctrl+C to test the blocker.");

    latch.await();

    lookupSubscription.dispose();
    pingSubscription.dispose();
    gcSubscription.dispose();

    discovery.clean(Mono::error).block();

    logger.info("Bye!");
  }
}
