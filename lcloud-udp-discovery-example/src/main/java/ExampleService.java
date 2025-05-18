/* (C) 2025 Vladimir E. Koltunov (mtbo.org) */

import java.net.SocketException;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.mtbo.lcloud.discovery.udp.*;

/**
 * Demo application
 *
 * <p>Ping network in 5 seconds
 */
public class ExampleService {

  /**
   * Main method
   *
   * @param args arguments
   * @throws InterruptedException in case of interruption
   * @throws SocketException in case of fatal network error
   */
  public static void main(String[] args) throws InterruptedException, SocketException {

    final int port = 8888;
    var serviceConfig =
        new UdpServiceConfig(
            "xService",
            Optional.ofNullable(System.getenv("HOSTNAME")).orElse(UUID.randomUUID().toString()),
            port);
    var discoveryService = new UdpDiscoveryService(serviceConfig);

    var clientConfig = new UdpClientConfig("xService", port);
    var discoveryClient = new UdpDiscoveryClient(clientConfig);

    var serviceListener = discoveryService.listen(new UdpConnection(port)).subscribe();

    var clientListener =
        discoveryClient
            .startLookup(Duration.ofSeconds(2))
            .doOnNext(
                instances -> {
                  String joined = instances.stream().sorted().collect(Collectors.joining("\n"));

                  System.out.println("***********************************************");
                  System.out.println(
                      clientConfig.serviceName + " instances are discovered:\n\n" + joined);
                  System.out.println("***********************************************\n");
                })
            .doOnError(
                throwable -> {
                  logger.severe("Lookup Error: " + throwable.getMessage());
                  logger.throwing(ExampleService.class.getName(), "main", throwable);
                })
            .onErrorComplete(throwable -> !(throwable instanceof InterruptedException))
            .subscribe();

    Thread.sleep(10000);
    clientListener.dispose();
    serviceListener.dispose();
  }

  static Logger logger = Logger.getLogger(ExampleService.class.getName());

  static {
    Handler handler = new ConsoleHandler();
    Level level = Level.FINE;
    handler.setLevel(level);
    Logger logger1 = Logger.getLogger("");
    logger1.setLevel(level);
    logger1.addHandler(handler);
  }
}
