package se.elnama.core.serial.logger;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.fazecast.jSerialComm.SerialPort;
import java.lang.reflect.Field;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.prefs.Preferences;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;
import se.elnama.core.serial.msg.SerialMessage;

@TestMethodOrder(OrderAnnotation.class)
class SerialLoggerModelTest {

  private static final String comPortName = "COM1";
  private static final BlockingQueue<SerialMessage> queue = new ArrayBlockingQueue<>(10);
  private static SerialLoggerModel serialLoggerModel;
  private final Preferences prefs = Preferences.userRoot()
      .node("se.elnama.core.serial.logger.SerialLoggerModel");

  @BeforeAll
  static void allSetup() {
    serialLoggerModel = new SerialLoggerModel(comPortName, 0, queue);
  }

  @AfterAll
  static void tearDownAllSetup() {
    if (!SerialLoggerModel.getStopped()[0]) {
      serialLoggerModel.closeAllPorts();
    }
  }

  @AfterEach
  void tearDownEachSetup() {
    queue.clear();
  }

  @Test
  @Order(1)
  void getStopped() {
    assertFalse(SerialLoggerModel.getStopped()[0]);
  }

  @Test
  @Order(2)
  void setBaud() {
    serialLoggerModel.setBaudRate(38123);
    await().atMost(2, SECONDS).until(() -> serialLoggerModel.getBaudRate() == 38123);
    assertEquals(38123, serialLoggerModel.getBaudRate());

    serialLoggerModel.setBaudRate(38400);
  }

  @Test
  @Order(3)
  void getBaud() {
    await().atMost(2, SECONDS).until(() -> serialLoggerModel.getBaudRate() == 38400);
    assertEquals(serialLoggerModel.getBaudRate(), prefs.getInt(comPortName + "BaudRate", 38400));
  }

  @Test
  @Order(4)
  void setIntervalTime() {
    serialLoggerModel.setIntervalTime(51);
    await().atMost(2, SECONDS).until(() -> serialLoggerModel.getIntervalTime() == 51);
    assertEquals(51, serialLoggerModel.getIntervalTime());

    serialLoggerModel.setIntervalTime(50);
  }

  @Test
  @Order(5)
  void getIntervalTime() {
    await().atMost(2, SECONDS).until(() -> serialLoggerModel.getIntervalTime() == 50);
    assertEquals(serialLoggerModel.getIntervalTime(), prefs.getInt(comPortName + "Interval", 50));
  }

  @Test
  @Order(6)
  void setTimeoutMode() {
    serialLoggerModel.setTimeoutMode(SerialPort.TIMEOUT_READ_BLOCKING);
    await().atMost(2, SECONDS)
        .until(() -> prefs.getInt(comPortName + "Mode", 16) == SerialPort.TIMEOUT_READ_BLOCKING);
    assertEquals(SerialPort.TIMEOUT_READ_BLOCKING, prefs.getInt(comPortName + "Mode", 16));

    serialLoggerModel.setTimeoutMode(SerialPort.TIMEOUT_READ_BLOCKING);
  }

  @Test
  @Order(7)
  void setDecrypt() {
    serialLoggerModel.setDecrypt(true);
    assertTrue(prefs.getBoolean(comPortName + "Decrypt", false));

    serialLoggerModel.setDecrypt(false);
  }

  @Test
  @Order(8)
  void getDecrypt() {
    assertFalse(prefs.getBoolean(comPortName + "Decrypt", false));
  }

  @Test
  @Order(9)
  void run() throws NoSuchFieldException, IllegalAccessException {
    SerialPort mockSerialPort = Mockito.mock(SerialPort.class);
    Field serialPortField = SerialLoggerModel.class.getDeclaredField("serialPort");
    serialPortField.setAccessible(true);
    serialPortField.set(serialLoggerModel, mockSerialPort);

    byte[] testData = "Test data".getBytes();
    Mockito.when(mockSerialPort.readBytes(Mockito.any(byte[].class), Mockito.anyInt()))
        .thenReturn(testData.length);

    await().atMost(2, SECONDS).until(() -> queue.size() == 10);
    assertEquals(10, queue.size());
  }

  @Test
  @Order(10)
  void closePort() {
    int threadsBeforeClose = Thread.activeCount();
    int threadSerialLoggerModel = 1;

    serialLoggerModel.closePort(0);
    waitForThreadsToFinish(threadsBeforeClose - threadSerialLoggerModel);
  }

  @Test
  @Order(11)
  void closeAllPorts() {
    serialLoggerModel = new SerialLoggerModel("COM1", 0, queue);
    serialLoggerModel = new SerialLoggerModel("COM2", 1, queue);
    int threadsBeforeClose = Thread.activeCount();
    int threadSerialLoggerModel = 2;

    serialLoggerModel.closeAllPorts();
    waitForThreadsToFinish(threadsBeforeClose - threadSerialLoggerModel);
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