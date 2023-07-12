package se.elnama.cli.netlogger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
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
import se.elnama.core.net.logger.NetLoggerModel;

@TestMethodOrder(OrderAnnotation.class)
class NetLoggerCliTest {

  private AutoCloseable closeable;

  @Mock
  private NetLoggerModel mockNetLoggerModel;

  @Spy
  @InjectMocks
  private NetLoggerCli spyNetLoggerCli;

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
    spyNetLoggerCli.initialize();
    verify(spyNetLoggerCli, times(1)).initialize();

    Map<Thread, StackTraceElement[]> threadMap = getThreadMap();
    assertThreadExistsWithName(threadMap, "NET1");
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
    assertTrue(spyNetLoggerCli.runExecution());
  }

  private void handleSwitchDefault() {
    System.setIn(new ByteArrayInputStream("".getBytes()));
    when(spyNetLoggerCli.readFromKeyboard()).thenReturn("");
  }

  @Test
  @Order(3)
  void getLog() {
    handleSwitchCaseLog();
    assertTrue(spyNetLoggerCli.runExecution());
    verify(mockNetLoggerModel, times(1)).getLog(anyInt(), anyString());
  }

  private void handleSwitchCaseLog() {
    System.setIn(new ByteArrayInputStream("".getBytes()));
    when(spyNetLoggerCli.readFromKeyboard()).thenReturn("log").thenReturn("1234");
  }

  @Test
  @Order(4)
  void sendCommand() {
    handleSwitchCaseSend();
    assertTrue(spyNetLoggerCli.runExecution());
    verify(mockNetLoggerModel, times(1)).sendCommand(anyInt(), anyString());
  }

  private void handleSwitchCaseSend() {
    System.setIn(new ByteArrayInputStream("".getBytes()));
    when(spyNetLoggerCli.readFromKeyboard()).thenReturn("send").thenReturn("1234")
        .thenReturn("cmd");
  }

  @Test
  @Order(5)
  void toggleSql() {
    handleSwitchCaseSql();
    assertTrue(spyNetLoggerCli.runExecution());
    verify(mockNetLoggerModel, times(1)).toggleUseSql(anyBoolean());
  }

  private void handleSwitchCaseSql() {
    System.setIn(new ByteArrayInputStream("".getBytes()));
    when(spyNetLoggerCli.readFromKeyboard()).thenReturn("sql").thenReturn("true");
  }

  @Test
  @Order(6)
  void closeThreads() {
    handleSwitchCaseQ();
    assertFalse(spyNetLoggerCli.runExecution());
    verify(mockNetLoggerModel, times(1)).closeAllPorts();
  }

  private void handleSwitchCaseQ() {
    System.setIn(new ByteArrayInputStream("".getBytes()));
    when(spyNetLoggerCli.readFromKeyboard()).thenReturn("q");
  }
}