/* (C) 2025 Vladimir E. Koltunov (mtbo.org) */

import java.net.SocketException;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.mtbo.lcloud.discovery.DiscoveryService;
import org.mtbo.lcloud.discovery.udp.DiscoveryClient;
import org.mtbo.lcloud.discovery.udp.UdpConnection;

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
    DiscoveryService discoveryService =
        new DiscoveryService(
            Optional.ofNullable(System.getenv("HOSTNAME")).orElse(UUID.randomUUID().toString()),
            "xService",
            new UdpConnection(8888));

    discoveryService.setDaemon(true);
    discoveryService.start();

    DiscoveryClient discoveryClient = new DiscoveryClient("xService", 8888);

    discoveryClient
        .lookup()
        .doOnNext(
            instances -> {
              String joined = instances.stream().sorted().collect(Collectors.joining("\n"));

              System.out.println("***********************************************");
              System.out.println("Instances Discovered:\n\n" + joined);
              System.out.println("***********************************************\n");
            })
        .doOnError(
            throwable -> {
              System.out.println("Lookup Error: " + throwable.getMessage());
                throwable.printStackTrace();
            })
        .onErrorComplete(throwable -> !(throwable instanceof InterruptedException))
        .subscribe();

    Thread.sleep(5000);

    discoveryService.shutdown();
  }

  //    static Logger logger = Logger.getLogger(ExampleService.class.getName());

  static {
    Handler handler = new ConsoleHandler();
    Level level = Level.FINE;
    handler.setLevel(level);
    Logger logger1 = Logger.getLogger("");
    logger1.setLevel(level);
    logger1.addHandler(handler);
  }
}
