/* (C) 2025 Vladimir E. (PROGrand) Koltunov (mtbo.org) */

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Calendar;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.LogManager;
import org.mtbo.lcloud.discovery.logging.FileLineLogger;
import org.mtbo.lcloud.discovery.multicast.MulticastDiscovery;
import org.mtbo.lcloud.discovery.multicast.MulticastDiscovery.Config;
import org.mtbo.lcloud.discovery.multicast.MulticastPublisher;
import org.mtbo.lcloud.discovery.udp.*;
import reactor.core.scheduler.Schedulers;
import sun.misc.Signal;
import sun.misc.SignalHandler;

/**
 * Demo application
 *
 * <p>Ping network in 5 seconds
 */
public class UdpDiscoveryExample {

  static FileLineLogger logger;

  static {
    var logFile =
        Optional.ofNullable(System.getProperty("java.util.logging.config.file")).orElse("");

    String skipConfig =
        Optional.ofNullable(System.getProperty("lcloud.skip.logging.config")).orElse("false");

    if (logFile.isEmpty() && !skipConfig.equals("true")) {
      try (var is =
          UdpDiscoveryExample.class.getClassLoader().getResourceAsStream("logging.properties")) {
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

    logger = FileLineLogger.getLogger(UdpDiscoveryExample.class.getName());
  }

  /** Default */
  UdpDiscoveryExample() {}

  /**
   * Main method
   *
   * @param args arguments
   * @throws InterruptedException in case of interruption
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
    var discovery = new MulticastDiscovery(config);

    var subscription =
        discovery
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
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe();

    try (ExecutorService service = Executors.newFixedThreadPool(2)) {

      service.submit(
          () -> {
            while (!Thread.currentThread().isInterrupted()) {
              System.gc();
              try {
                Thread.sleep(10000);
              } catch (InterruptedException e) {
                break;
              }
            }
          });

      var p =
          service.submit(
              new MulticastPublisher(
                  new MulticastPublisher.Config(
                      serviceName,
                      instanceName,
                      multicastAddr,
                      multicastPort,
                      Duration.ofMillis(50))));

      var latch = new CountDownLatch(1);

      SignalHandler handler =
          sig -> {
            logger.info("Shutting down...");
            subscription.dispose();
            service.shutdownNow();
            latch.countDown();
          };

      Signal.handle(new Signal("INT"), handler);

      logger.info("Program started. Press Ctrl+C to test the blocker.");

      latch.await();

      logger.info("Bye!");

      try {
        p.get();
      } catch (InterruptedException ignored) {
      } catch (ExecutionException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
