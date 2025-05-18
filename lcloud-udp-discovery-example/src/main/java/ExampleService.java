/* (C) 2025 Vladimir E. Koltunov (mtbo.org) */

import java.net.SocketException;
import java.util.Optional;
import java.util.Set;
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

    var th = getThread(discoveryClient);

    th.join(5000);

    th.interrupt();

    discoveryService.shutdown();
  }

  private static Thread getThread(DiscoveryClient discoveryClient) {
    var th =
        new Thread(
            () -> {
              while (!Thread.currentThread().isInterrupted()) {
                Set<String> instances;
                try {
                  instances = discoveryClient.lookup();
                } catch (InterruptedException | SocketException e) {
                  break;
                }

                String joined = instances.stream().sorted().collect(Collectors.joining("\n"));

                System.out.println("***********************************************");
                System.out.println("Instances Discovered:\n\n" + joined);
                System.out.println("***********************************************\n");
              }
            });

    th.start();
    return th;
  }

  //    static Logger logger = Logger.getLogger(ExampleService.class.getName());

  static {
    Handler handler = new ConsoleHandler();
    handler.setLevel(Level.FINE);
    Logger logger1 = Logger.getLogger("");
    logger1.setLevel(Level.FINE);
    logger1.addHandler(handler);
  }
}
