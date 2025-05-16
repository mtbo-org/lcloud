package org.mtbo.lcloud.discovery.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class DiscoveryService extends Thread {
    private final String instanceName;
    private final String serviceName;

    private final int port;

    public DiscoveryService(String instanceName, String serviceName, int port) {
        this.instanceName = instanceName;
        this.serviceName = serviceName;
        this.port = port;
    }

    DatagramSocket socket;

    @Override
    public void run() {
        try {
            //Keep a socket open to listen to all the UDP trafic that is destined for this port
            socket = new DatagramSocket(null);
            socket.setReuseAddress(true);
            socket.setBroadcast(true);
            socket.bind(new InetSocketAddress(port));

            while (!isInterrupted()) {
                logInfo(">>>Ready to receive broadcast packets!");

                //Receive a packet
                byte[] recvBuf = new byte[15000];
                DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                socket.receive(packet);

                String message = new String(packet.getData(), packet.getOffset(), packet.getLength());

                //Packet received
                logInfo(">>>Discovery packet received from: " + packet.getAddress().getHostAddress());
                logInfo(">>>Packet received; data: " + message);

                if (message.equals("UDP_DISCOVERY_REQUEST " + serviceName)) {
                    byte[] sendData = ("UDP_DISCOVERY_RESPONSE " + instanceName).getBytes(StandardCharsets.UTF_8);

                    //Send a response
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, packet.getAddress(), packet.getPort());
                    socket.send(sendPacket);

                    logInfo(">>>Sent packet to: " + sendPacket.getAddress().getHostAddress());
                }
            }
        } catch (IOException ex) {
            System.err.println(ex.getLocalizedMessage());
            ex.printStackTrace();
        }

    }

    private void logInfo(String s) {
        //System.out.println(getClass().getName() + s);
    }
}
