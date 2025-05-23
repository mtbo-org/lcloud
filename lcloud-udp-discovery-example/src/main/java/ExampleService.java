/* (C) 2025 Vladimir E. (PROGrand) Koltunov (mtbo.org) */

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.LogManager;
import org.mtbo.lcloud.discovery.logging.FileLineLogger;
import org.mtbo.lcloud.discovery.multicast.MulticastDiscovery;
import org.mtbo.lcloud.discovery.multicast.MulticastDiscovery.Config;
import org.mtbo.lcloud.discovery.multicast.MulticastPublisher;
import org.mtbo.lcloud.discovery.udp.*;

/**
 * Demo application
 *
 * <p>Ping network in 5 seconds
 */
public class ExampleService {

  static FileLineLogger logger;

  static {
    var logFile =
        Optional.ofNullable(System.getProperty("java.util.logging.config.file")).orElse("");

    String skipConfig =
        Optional.ofNullable(System.getProperty("lcloud.skip.logging.config")).orElse("false");

    if (logFile.isEmpty() && !skipConfig.equals("true")) {
      try (var is =
          ExampleService.class.getClassLoader().getResourceAsStream("logging.properties")) {
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

    logger = FileLineLogger.getLogger(ExampleService.class.getName());
  }

  /** Default */
  ExampleService() {}

  /**
   * Main method
   *
   * @param args arguments
   * @throws InterruptedException in case of interruption
   */
  public static void main(String[] args) throws InterruptedException, ExecutionException {

    final String serviceName = Optional.ofNullable(System.getenv("SERVICE_NAME")).orElse("lcloud");

    final String instanceName =
        Optional.ofNullable(System.getenv("HOSTNAME")).orElse(UUID.randomUUID().toString());

    final String multicastAddr =
        Optional.ofNullable(System.getenv("MULTICAST_ADDR")).orElse("230.0.0.0");

    final var multicastPort =
        Integer.parseInt(Optional.ofNullable(System.getenv("MULTICAST_PORT")).orElse("8888"));

    var discovery =
        new MulticastDiscovery(
            new Config(serviceName, multicastAddr, multicastPort, Duration.ofSeconds(2)));

    var subscription =
        discovery
            .receive()
            .repeat()
            .doOnEach(
                x -> {
                  System.out.println(
                      String.join(
                          "\n",
                          "*****************************************",
                          x.toString(),
                          "n*****************************************"));
                })
            .subscribe();

    try (ExecutorService service = Executors.newFixedThreadPool(1)) {
      var p =
          service.submit(
              new MulticastPublisher(
                  new MulticastPublisher.Config(
                      serviceName,
                      instanceName,
                      multicastAddr,
                      multicastPort,
                      Duration.ofMillis(500))));

      try {
        System.in.read();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      subscription.dispose();

      service.shutdownNow();
      try {
        p.get();
      } catch (InterruptedException ignored) {
      } catch (ExecutionException e) {
        throw new RuntimeException(e);
      }
    }

    //    final var serverPort =
    //        Integer.parseInt(Optional.ofNullable(System.getenv("SERVICE_PORT")).orElse("8888"));
    //
    //    final var clientPort =
    //        Integer.parseInt(Optional.ofNullable(System.getenv("CLIENT_PORT")).orElse("8889"));
    //
    //    final var serviceName =
    // Optional.ofNullable(System.getenv("SERVICE_NAME")).orElse("lcloud");
    //    final var instanceName =
    //        Optional.ofNullable(System.getenv("HOSTNAME")).orElse(UUID.randomUUID().toString());
    //    final var serviceConfig = new UdpServiceConfig(serviceName, instanceName, serverPort);
    //    final var discoveryService = new UdpDiscoveryService(serviceConfig);
    //
    //    var clientConfig = new UdpClientConfig(serviceName, instanceName, serverPort, clientPort);
    //    var discoveryClient = new UdpDiscoveryClient(clientConfig);
    //
    //    var serviceListener = discoveryService.listen(new UdpConnection(serverPort)).subscribe();
    //
    //    var clientListener =
    //        discoveryClient
    //            .startLookup(Duration.ofMillis(150))
    //            .doOnNext(
    //                instances -> {
    //                  synchronized (FileLineLogger.class) {
    //                    logger.info("****************************************************");
    //                    logger.info(
    //                        String.format(
    //                            "[%1$tH:%<tM:%<tS.%<tL] %2$s instances are discovered [%3$3d]",
    //                            Calendar.getInstance(), clientConfig.serviceName,
    // instances.size()));
    //                    instances.forEach(message -> logger.info(String.format("%1$-52s",
    // message)));
    //                    logger.info("****************************************************");
    //                  }
    //                })
    //            .doOnError(
    //                throwable -> {
    //                  logger.severe("Lookup Error: " + throwable, throwable);
    //                })
    //            .onErrorComplete(throwable -> !(throwable instanceof InterruptedException))
    //            .delayUntil(strings -> Mono.delay(Duration.ofMillis(50)))
    //            .subscribe();
    //
    //    Runtime.getRuntime()
    //        .addShutdownHook(
    //            new Thread(
    //                () -> {
    //                  logger.info("Shutting down...");
    //
    //                  clientListener.dispose();
    //                  serviceListener.dispose();
    //                }));
    //
    //    var latch = new CountDownLatch(1);
    //
    //    SignalHandler handler =
    //        sig -> {
    //          logger.info("Shutting down...");
    //          clientListener.dispose();
    //          serviceListener.dispose();
    //          latch.countDown();
    //        };
    //
    //    Signal.handle(new Signal("INT"), handler);
    //
    //    logger.info("Program started. Press Ctrl+C to test the blocker.");
    //
    //    latch.await();
    //
    //    logger.info("Bye!");
  }
}
