/* (C) 2025 Vladimir E. (PROGrand) Koltunov (mtbo.org) */
import java.time.Duration;
import java.util.Calendar;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.logging.LogManager;
import org.mtbo.lcloud.discovery.logging.FileLineLogger;
import org.mtbo.lcloud.discovery.udp.*;
import reactor.core.publisher.Mono;
import sun.misc.Signal;
import sun.misc.SignalHandler;

/**
 * Demo application
 *
 * <p>Ping network in 5 seconds
 */
public class ExampleService {

  static FileLineLogger logger = FileLineLogger.getLogger(ExampleService.class.getName());

  static {
    try (var is = ExampleService.class.getClassLoader().getResourceAsStream("logging.properties")) {
      LogManager.getLogManager().readConfiguration(is);
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  /** Default */
  ExampleService() {}

  /**
   * Main method
   *
   * @param args arguments
   * @throws InterruptedException in case of interruption
   */
  public static void main(String[] args) throws InterruptedException {

    final var port = 8888;
    final var serviceName = Optional.ofNullable(System.getenv("SERVICE_NAME")).orElse("lcloud");
    final var instanceName =
        Optional.ofNullable(System.getenv("HOSTNAME")).orElse(UUID.randomUUID().toString());
    final var serviceConfig = new UdpServiceConfig(serviceName, instanceName, port);
    final var discoveryService = new UdpDiscoveryService(serviceConfig);

    var clientConfig = new UdpClientConfig(serviceName, instanceName, port);
    var discoveryClient = new UdpDiscoveryClient(clientConfig);

    var serviceListener = discoveryService.listen(new UdpConnection(port)).subscribe();

    var clientListener =
        discoveryClient
            .startLookup(Duration.ofMillis(150))
            .doOnNext(
                instances -> {
                  logger.info("****************************************************");
                  logger.info(
                      String.format(
                          "[%1$tH:%<tM:%<tS.%<tL] %2$s instances are discovered [%3$3d]",
                          Calendar.getInstance(), clientConfig.serviceName, instances.size()));
                  instances.forEach(message -> logger.info(String.format("%1$-52s", message)));
                  logger.info("****************************************************");
                })
            .doOnError(
                throwable -> {
                  logger.severe("Lookup Error: " + throwable, throwable);
                })
            .onErrorComplete(throwable -> !(throwable instanceof InterruptedException))
            .delayUntil(strings -> Mono.delay(Duration.ofMillis(50)))
            .subscribe();

    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  logger.info("Shutting down...");

                  clientListener.dispose();
                  serviceListener.dispose();
                }));

    var latch = new CountDownLatch(1);

    SignalHandler handler =
        sig -> {
          logger.info("Shutting down...");
          clientListener.dispose();
          serviceListener.dispose();
          latch.countDown();
        };

    Signal.handle(new Signal("INT"), handler);

    logger.info("Program started. Press Ctrl+C to test the blocker.");

    latch.await();

    logger.info("Bye!");
  }
}
