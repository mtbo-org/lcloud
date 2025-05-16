import org.mtbo.lcloud.discovery.udp.DiscoveryClient;
import org.mtbo.lcloud.discovery.udp.DiscoveryService;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;


public class ExampleService {

    public static void main(String[] args) throws InterruptedException {
        DiscoveryService discoveryService = new DiscoveryService(Optional.ofNullable(System.getenv("HOSTNAME")).orElse(UUID.randomUUID().toString()), "xService", 8888);
        discoveryService.setDaemon(true);
        discoveryService.start();

        DiscoveryClient discoveryClient = new DiscoveryClient("xService", 8888);

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

        th.join();
    }
}
