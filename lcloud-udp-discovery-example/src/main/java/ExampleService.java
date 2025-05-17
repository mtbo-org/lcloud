import org.mtbo.lcloud.discovery.udp.DiscoveryClient;
import org.mtbo.lcloud.discovery.udp.DiscoveryService;
import org.mtbo.lcloud.discovery.udp.UdpConnection;

import java.net.SocketException;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.*;
import java.util.stream.Collectors;


public class ExampleService {
    public static void main(String[] args) throws InterruptedException, SocketException {
        DiscoveryService discoveryService = new DiscoveryService(Optional.ofNullable(System.getenv("HOSTNAME")).orElse(UUID.randomUUID().toString()),
                "xService", new UdpConnection(8888));
        discoveryService.setDaemon(true);
        discoveryService.start();

        DiscoveryClient discoveryClient = new DiscoveryClient("xService", 8888);

        var th = getThread(discoveryClient);

        th.join();
    }

    private static Thread getThread(DiscoveryClient discoveryClient) {
        var th = new Thread(() -> {
            while (true) {
                var instances = discoveryClient.lookup();

                String joined = instances.stream().sorted().collect(Collectors.joining("\n"));

                System.out.println("***********************************************");
                System.out.println("Instances Discovered:\n\n" + joined);
                System.out.println("***********************************************\n");
            }
        });

        th.start();
        return th;
    }

    static Logger logger = Logger.getLogger(ExampleService.class.getName());

    static {
        Handler handler = new ConsoleHandler();
        handler.setLevel(Level.FINE);
        Logger logger1 = Logger.getLogger("");
        logger1.setLevel(Level.FINE);
        logger1.addHandler(handler);
    }
}
