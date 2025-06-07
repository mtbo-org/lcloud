/* (C) 2025 Vladimir E. (PROGrand) Koltunov (mtbo.org) */

import java.net.SocketException;
import java.time.Duration;
import java.util.Calendar;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.mtbo.lcloud.discovery.multicast.MulticastDiscovery;
import org.mtbo.lcloud.discovery.multicast.MulticastDiscovery.Config;
import org.mtbo.lcloud.discovery.multicast.MulticastPublisher;
import org.mtbo.lcloud.discovery.sql.AutoDisposable;
import org.mtbo.lcloud.logging.FileLineLogger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import sun.misc.Signal;
import sun.misc.SignalHandler;

/**
 * Demo application
 *
 * <p>Ping network in 5 seconds
 */
public class MulticastDiscoveryExample {

  static FileLineLogger logger;

  static {
    FileLineLogger.init(MulticastDiscoveryExample.class);
    logger = FileLineLogger.getLogger(MulticastDiscoveryExample.class.getName());
  }

  /** Default */
  MulticastDiscoveryExample() {}

  /**
   * Main method
   *
   * @param args arguments
   * @throws InterruptedException in case of interruption
   * @throws SocketException in case of network error
   */
  public static void main(String[] args) throws InterruptedException, SocketException {

    final String serviceName = Optional.ofNullable(System.getenv("SERVICE_NAME")).orElse("lcloud");

    final String instanceName =
        Optional.ofNullable(System.getenv("HOSTNAME")).orElse(UUID.randomUUID().toString());

    final String multicastAddr =
        Optional.ofNullable(System.getenv("MULTICAST_ADDR")).orElse("230.0.0.0");

    final var multicastPort =
        Integer.parseInt(Optional.ofNullable(System.getenv("MULTICAST_PORT")).orElse("8888"));

    Config config = new Config(serviceName, multicastAddr, multicastPort, Duration.ofMillis(250));
    @SuppressWarnings("ReactiveStreamsUnusedPublisher")
    var discovery =
        new MulticastDiscovery(config)
            .receive()
            .repeat()
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
            .subscribeOn(Schedulers.boundedElastic());

    @SuppressWarnings("ReactiveStreamsUnusedPublisher")
    final var gc =
        Flux.interval(Duration.ofSeconds(1))
            .flatMap(
                (Long aLong) -> {
                  System.gc();
                  return Mono.just(true);
                });

    try (final var ignored = new AutoDisposable(Flux.merge(discovery, gc).subscribe())) {

      try (ExecutorService service = Executors.newFixedThreadPool(2)) {

        final var p =
            service.submit(
                new MulticastPublisher(
                    new MulticastPublisher.Config(
                        serviceName,
                        instanceName,
                        multicastAddr,
                        multicastPort,
                        Duration.ofMillis(50))));

        final var latch = new CountDownLatch(1);

        SignalHandler handler =
            sig -> {
              logger.info("Shutting down...");

              service.shutdownNow();
              latch.countDown();
            };

        Signal.handle(new Signal("INT"), handler);

        logger.info("Program started. Press Ctrl+C to test the blocker.");

        latch.await();

        logger.info("Bye!");

        try {
          p.get();
        } catch (InterruptedException ignored2) {
        } catch (ExecutionException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }
}
