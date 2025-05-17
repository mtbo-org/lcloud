package org.mtbo.lcloud.discovery.udp;

import org.mtbo.lcloud.discovery.Connection;
import org.mtbo.lcloud.discovery.Listener;
import org.mtbo.lcloud.discovery.Sender;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public class DiscoveryService extends Thread {
    private final String instanceName;
    private final String serviceName;
    private final Listener listener;
    private final Sender sender;


    public DiscoveryService(String instanceName, String serviceName, Listener listener, Sender sender) {
        this.instanceName = instanceName;
        this.serviceName = serviceName;
        this.listener = listener;
        this.sender = sender;
    }

    public DiscoveryService(String instanceName, String serviceName, Connection connection) {
        this(instanceName, serviceName, connection, connection);
    }


    @Override
    public void run() {
        try {
            final var prefix = ByteBuffer.wrap(("UDP_DISCOVERY_REQUEST " + serviceName).getBytes(StandardCharsets.UTF_8)).asReadOnlyBuffer();
            final byte[] sendData = ("UDP_DISCOVERY_RESPONSE " + instanceName).getBytes(StandardCharsets.UTF_8);

            while (!isInterrupted()) {
                logger.finer(">>> Ready to receive broadcast packets!");

                var packet = listener.receive();

                ByteBuffer data = packet.data();
                logger.finer(">>> Packet received; packet: " + new String(data.array(),
                        data.arrayOffset(), data.limit()));

                if (prefix.equals(data.slice(0, prefix.limit()))) {
                    //Send a response
                    sender.send(packet.copyWithData(ByteBuffer.wrap(sendData)));
                }
            }
        } catch (IOException ex) {
            logger.severe(ex.getMessage());
            logger.throwing(DiscoveryService.class.getSimpleName(), "run", ex);
        }

    }

    static final Logger logger = Logger.getLogger(DiscoveryService.class.getName());
}
