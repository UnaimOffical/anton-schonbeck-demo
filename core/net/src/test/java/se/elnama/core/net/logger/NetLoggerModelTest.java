package se.elnama.core.net.logger;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static se.elnama.lib.util.TimeUtils.getDateTimeFile;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import se.elnama.core.net.msg.NetMessage;
import se.elnama.lib.util.TimeUtils;

@TestMethodOrder(OrderAnnotation.class)
class NetLoggerModelTest {

  private static final BlockingQueue<NetMessage> queue = new ArrayBlockingQueue<>(10);
  private static final int portNumber = 8888;
  private static DatagramSocket datagramSocket;
  private static NetLoggerModel netLoggerModel;

  @BeforeAll
  static void allSetup() throws SocketException {
    datagramSocket = new DatagramSocket(9999);
    netLoggerModel = new NetLoggerModel("NET1", 0, queue, portNumber);
  }

  @AfterAll
  static void tearDownAllSetup() {
    if (!NetLoggerModel.getStopped()[0]) {
      netLoggerModel.closeAllPorts();
    }
    datagramSocket.close();
  }

  @AfterEach
  void tearDownEachSetup() {
    queue.clear();
  }

  @Test
  @Order(1)
  void getStopped() {
    assertFalse(NetLoggerModel.getStopped()[0]);
  }

  @Test
  @Order(2)
  void getUseSql() {
    assertTrue(netLoggerModel.getUseSql());
  }

  @Test
  @Order(3)
  void toggleUseSqlTestFalse() {
    netLoggerModel.toggleUseSql(false);
    assertFalse(netLoggerModel.getUseSql(), "failed to toggle useSql false");
  }

  @Test
  @Order(4)
  void toggleUseSqlTestTrue() {
    netLoggerModel.toggleUseSql(true);
    assertTrue(netLoggerModel.getUseSql(), "failed to toggle useSql true");
  }

  @Test
  @Order(5)
  void getLogTestLogFromDatabase() throws IOException {
    String timestamp = TimeUtils.getTimestamp();

    netLoggerModel.toggleUseSql(true);
    sendMessageToLogger();
    compareGetLogFromDatabase(netLoggerModel, timestamp);
  }

  private void compareGetLogFromDatabase(NetLoggerModel netLoggerModel, String timestamp) {
    netLoggerModel.toggleUseSql(true);
    netLoggerModel.getLog(9916, timestamp);

    await().atMost(2, SECONDS).untilAsserted(() -> {
      String actualSql = "./log/" + getDateTimeFile() + "-log.txt";
      String expectedSql = "./log/useSqlTrueCompare.txt";

      assertAll("Files should exist and have equal size",
          () -> assertTrue(new File(actualSql).exists(), "useSqlTrueCompare.txt not found"),
          () -> assertTrue(new File(expectedSql).exists(),
              "compareGetLogFromSql expected log file not found"),
          () -> assertEquals(Files.size(Paths.get(actualSql)), Files.size(Paths.get(expectedSql)),
              "compareGetLogFromSql file size"));
    });
  }

  @Test
  @Order(6)
  void getLogTestLogFromMap() throws IOException {
    String timestamp = TimeUtils.getTimestamp();

    netLoggerModel.toggleUseSql(false);
    sendMessageToLogger();
    compareGetLogFromMap(netLoggerModel, timestamp);
  }

  private void compareGetLogFromMap(NetLoggerModel netLoggerModel, String timestamp) {
    netLoggerModel.toggleUseSql(false);
    netLoggerModel.getLog(9916, timestamp);

    await().atMost(2, SECONDS).untilAsserted(() -> {
      String actualMap = "./log/" + getDateTimeFile() + "-logSimple.txt";
      String expectedMap = "./log/useSqlFalseCompare.txt";

      assertAll("Files should exist and have equal size",
          () -> assertTrue(new File(actualMap).exists(), "useSqlFalseCompare.txt not found"),
          () -> assertTrue(new File(expectedMap).exists(),
              "compareGetLogFromMap expected log file not found"),
          () -> assertEquals(Files.size(Paths.get(actualMap)), Files.size(Paths.get(expectedMap)),
              "compareGetLogFromMap"));
    });
  }

  @Test
  @Order(7)
  void sendCommandTestRequisitesFromDatabase() throws IOException {
    sendCommandToggleSql(true);
    compareSentMessage();
  }

  @Test
  @Order(8)
  void sendCommandTestRequisitesFromMap() throws IOException {
    sendCommandToggleSql(false);
    compareSentMessage();
  }

  private void sendCommandToggleSql(boolean toggleSql) throws IOException {
    netLoggerModel.toggleUseSql(toggleSql);
    sendMessageToLogger();
    netLoggerModel.sendCommand(9916, "speed");
  }

  private void compareSentMessage() {
    String actualSendPrerequisiteSql = findMessageReceived();
    String expectedSendPrerequisiteSql = "09 25 42 D1 16 99 00 00 40 30 00 00 00 00 00 00";

    assertEquals(expectedSendPrerequisiteSql, actualSendPrerequisiteSql);
  }

  private String findMessageReceived() {
    return Stream.generate(() -> {
      try {
        return Objects.requireNonNull(queue.poll(1, SECONDS)).getHexMessage();
      } catch (InterruptedException exc) {
        exc.printStackTrace();
        return null;
      }
    }).map(received -> {
      String messageSent = Objects.requireNonNull(received);
      if (messageSent.length() > 47) {
        return messageSent.substring(messageSent.length() - 47);
      } else {
        return messageSent;
      }
    }).filter(received -> Objects.requireNonNull(received)
        .equals("09 25 42 D1 16 99 00 00 40 30 00 00 00 00 00 00")).findFirst().orElse(null);
  }

  @Test
  @Order(9)
  void runTestMessageValid() throws IOException, InterruptedException {
    sendMessageToLogger();

    assertEquals(1, queue.size(), "valid message not added to the queue");
    assertTrue(queue.take().getValidMessage(), "validMessage returned false");
  }

  @Test
  @Order(10)
  void runTestMessageTooShort() throws IOException, InterruptedException {
    byte[] messageTooShort = {};

    sendMessageToLogger(messageTooShort);
    assertEquals(1, queue.size(), "short message not added to the queue");
    assertFalse(queue.take().getValidMessage(), "validMessage returned true for short message");
  }

  @Test
  @Order(11)
  void runTestMessageTooLong() throws IOException, InterruptedException {
    byte[] messageTooLong = {0x48, (byte) 0x81, 0x76, 0x7F, (byte) 0xB4, (byte) 0xE5, (byte) 0x8B,
        (byte) 0xBF, (byte) 0xB4, 0x5A, 0x45, (byte) 0x8C, 0x25, 0x1E, (byte) 0x8F, 0x18, 0x45,
        0x37, (byte) 0x9E, (byte) 0x88, 0x3F, 0x70, 0x3E, (byte) 0x97, 0x48, (byte) 0x81, 0x76,
        0x7F, (byte) 0xB4, (byte) 0xE5, (byte) 0x8B, (byte) 0xBF, (byte) 0xB4, 0x5A, 0x45,
        (byte) 0x8C, 0x25, 0x1E, (byte) 0x8F, 0x18, 0x45, 0x37, (byte) 0x9E, (byte) 0x88, 0x3F,
        0x70, 0x3E, (byte) 0x97, 0x48, (byte) 0x81, 0x76, 0x7F, (byte) 0xB4, (byte) 0xE5,
        (byte) 0x8B, (byte) 0xBF, (byte) 0xB4, 0x5A, 0x45, (byte) 0x8C, 0x25, 0x1E, (byte) 0x8F,
        0x18, 0x45, 0x37, (byte) 0x9E, (byte) 0x88, 0x3F, 0x70, 0x3E, (byte) 0x97};

    sendMessageToLogger(messageTooLong);
    assertEquals(1, queue.size(), "long message not added to the queue");
    assertFalse(queue.take().getValidMessage(), "validMessage returned true for long message");
  }

  @Test
  @Order(12)
  void runTestMessageWrongId() throws IOException, InterruptedException {
    byte[] messageWrongId = {0x48, (byte) 0x81, 0x76, 0x7F, (byte) 0xAA, (byte) 0xAA, (byte) 0x8B,
        (byte) 0xBF};

    sendMessageToLogger(messageWrongId);
    assertEquals(1, queue.size(), "noId message not added to the queue");
    assertFalse(queue.take().getValidMessage(), "validMessage returned true for wrong ID message");
  }

  private void sendMessageToLogger() throws IOException {
    byte[] message = {0x48, (byte) 0x81, 0x76, 0x7F, (byte) 0xB4, (byte) 0xE5, (byte) 0x8B,
        (byte) 0xBF, (byte) 0xB4, 0x5A, 0x45, (byte) 0x8C, 0x25, 0x1E, (byte) 0x8F, 0x18, 0x45,
        0x37, (byte) 0x9E, (byte) 0x88, 0x3F, 0x70, 0x3E, (byte) 0x97};

    DatagramPacket dp = new DatagramPacket(message, message.length, InetAddress.getLocalHost(),
        portNumber);
    datagramSocket.send(dp);
    await().atMost(2, SECONDS).until(() -> !queue.isEmpty());
  }

  private void sendMessageToLogger(byte[] message) throws IOException {
    DatagramPacket dp = new DatagramPacket(message, message.length, InetAddress.getLocalHost(),
        portNumber);
    datagramSocket.send(dp);
    await().atMost(2, SECONDS).until(() -> !queue.isEmpty());
  }

  @Test
  @Order(13)
  void closeAllPorts() {
    int threadsBeforeClose = Thread.activeCount();
    int threadsNetLoggerModel = 1;
    int threadsNetLoggerModelAndExecutor = 3;

    netLoggerModel.closeAllPorts();
    if (threadsBeforeClose > 3) {
      waitForThreadsToFinish(threadsBeforeClose - threadsNetLoggerModelAndExecutor);
    } else {
      waitForThreadsToFinish(threadsBeforeClose - threadsNetLoggerModel);
    }
  }

  private void waitForThreadsToFinish(int expectedThreadCount) {
    long startTime = System.currentTimeMillis();

    while (Thread.activeCount() > expectedThreadCount) {
      if (System.currentTimeMillis() - startTime >= 2000) {
        fail("expected threads : " + expectedThreadCount + " actual : " + Thread.activeCount());
      }
      Thread.yield();
    }
    assertEquals(expectedThreadCount, Thread.activeCount());
  }
}