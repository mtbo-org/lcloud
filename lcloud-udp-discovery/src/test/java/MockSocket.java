/* (C) 2025 Vladimir E. (PROGrand) Koltunov (mtbo.org) */
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;

public class MockSocket {

  static AtomicInteger sendFailCounter = new AtomicInteger();

  static {
    MockLogging.setupLogging();
  }

  public static DatagramSocket mockDatagramSocket(
      Stream<String> packets, boolean randomSendError, boolean randomReceiveError)
      throws IOException {
    return mockDatagramSocket(packets, randomSendError, false, randomReceiveError);
  }

  public static DatagramSocket mockDatagramSocket(
      Stream<String> packets, boolean failOnSend, boolean failOnClose, boolean failOnReceive)
      throws IOException {

    DatagramSocket socket = Mockito.mock(DatagramSocket.class);

    if (failOnClose) {
      doThrow(new IOException()).when(socket).close();
    }

    AtomicInteger receiveFailCounter = new AtomicInteger(0);

    doAnswer(
            invocation -> {
              int i = sendFailCounter.getAndIncrement() % 5;
              if (failOnSend && 4 == i) {
                throw new IOException("FAIL emitted by test");
              }
              return null;
            })
        .when(socket)
        .send(Mockito.any(DatagramPacket.class));

    var it = packets.iterator();

    doAnswer(
            invocation -> {
              int i = receiveFailCounter.getAndIncrement() % 5;
              if (failOnReceive && 4 == i) {
                throw new IOException();
              }

              getPacket(Objects.requireNonNull(it.next()), invocation);
              return null;
            })
        .when(socket)
        .receive(Mockito.any(DatagramPacket.class));

    return socket;
  }

  protected static void getPacket(String s, InvocationOnMock invocation)
      throws UnknownHostException {
    DatagramPacket args = (DatagramPacket) invocation.getArguments()[0];
    args.setData(s.getBytes());
    args.setAddress(InetAddress.getLocalHost());
  }
}
