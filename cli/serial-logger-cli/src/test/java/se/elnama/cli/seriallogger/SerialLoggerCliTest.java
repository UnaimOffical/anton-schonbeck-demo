package se.elnama.cli.seriallogger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import se.elnama.core.serial.logger.SerialLoggerModel;

@TestMethodOrder(OrderAnnotation.class)
class SerialLoggerCliTest {

  private AutoCloseable closeable;

  @Mock
  private SerialLoggerModel mockSerialLoggerModel;

  @Spy
  @InjectMocks
  private SerialLoggerCli spySerialLoggerCli;

  @BeforeEach
  public void eachSetup() {
    closeable = MockitoAnnotations.openMocks(this);
  }

  @AfterEach
  public void tearDownEachSetup() throws Exception {
    closeable.close();
  }

  @Test
  @Order(1)
  void initialize() {
    System.setIn(new ByteArrayInputStream("1\n".getBytes()));
    spySerialLoggerCli.initialize();

    Map<Thread, StackTraceElement[]> threadMap = getThreadMap();
    assertThreadExistsWithName(threadMap, "COM1");
    assertThreadExistsWithName(threadMap, "MSG1");
  }

  private Map<Thread, StackTraceElement[]> getThreadMap() {
    return Thread.getAllStackTraces();
  }

  private void assertThreadExistsWithName(Map<Thread, StackTraceElement[]> threadMap,
      String threadName) {
    long count = threadMap.keySet().stream().filter(t -> t.getName().equals(threadName)).count();
    assertEquals(1, count,
        "Expected exactly 1 thread with name " + threadName + ", found " + count);
  }

  @Test
  @Order(2)
  void runExecution() {
    handleSwitchDefault();
    assertTrue(spySerialLoggerCli.runExecution());
  }

  private void handleSwitchDefault() {
    System.setIn(new ByteArrayInputStream("1".getBytes()));
    when(spySerialLoggerCli.readFromKeyboard()).thenReturn("");
  }

  @Test
  @Order(3)
  void setComPortSettingsTestBaudRate() {
    handleSwitchCaseSet();
    assertTrue(spySerialLoggerCli.runExecution());
    verify(mockSerialLoggerModel, times(1)).setBaudRate(anyInt());
  }

  @Test
  @Order(4)
  void getComPortSettingsTestBaudRate() {
    handleSwitchCaseGet();
    assertTrue(spySerialLoggerCli.runExecution());
    verify(mockSerialLoggerModel, times(1)).getBaudRate();
  }

  @Test
  @Order(5)
  void setComPortSettingsTestInterval() {
    handleSwitchCaseSet();
    assertTrue(spySerialLoggerCli.runExecution());
    verify(mockSerialLoggerModel, times(1)).setIntervalTime(anyInt());
  }

  @Test
  @Order(6)
  void getComPortSettingsTestInterval() {
    handleSwitchCaseGet();
    assertTrue(spySerialLoggerCli.runExecution());
    verify(mockSerialLoggerModel, times(1)).getIntervalTime();
  }

  @Test
  @Order(7)
  void setComPortSettingsTestDecryption() {
    handleSwitchCaseSet();
    assertTrue(spySerialLoggerCli.runExecution());
    verify(mockSerialLoggerModel, times(1)).setDecrypt(anyBoolean());
  }

  @Test
  @Order(8)
  void getComPortSettingsTestDecryption() {
    handleSwitchCaseGet();
    assertTrue(spySerialLoggerCli.runExecution());
    verify(mockSerialLoggerModel, times(1)).getDecrypt();
  }

  private void handleSwitchCaseSet() {
    System.setIn(new ByteArrayInputStream("1".getBytes()));
    when(spySerialLoggerCli.readFromKeyboard()).thenReturn("set").thenReturn("38400")
        .thenReturn("50").thenReturn("false");
  }

  private void handleSwitchCaseGet() {
    System.setIn(new ByteArrayInputStream("1".getBytes()));
    when(spySerialLoggerCli.readFromKeyboard()).thenReturn("get");
  }

  @Test
  @Order(9)
  void closeThreads() {
    handleSwitchCaseQ();
    assertFalse(spySerialLoggerCli.runExecution());
    verify(mockSerialLoggerModel, times(1)).closeAllPorts();
  }

  private void handleSwitchCaseQ() {
    System.setIn(new ByteArrayInputStream("1".getBytes()));
    when(spySerialLoggerCli.readFromKeyboard()).thenReturn("q");
  }
}