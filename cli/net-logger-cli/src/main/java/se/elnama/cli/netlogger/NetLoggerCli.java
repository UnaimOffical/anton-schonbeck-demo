package se.elnama.cli.netlogger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import se.elnama.core.net.logger.NetLoggerModel;
import se.elnama.core.net.msg.NetMessage;
import se.elnama.core.net.msg.NetMessageConsumer;
import se.elnama.lib.cli.ElnamaCli;
import se.elnama.lib.log.ElnamaErrorLog;
import se.elnama.lib.util.TimeUtils;

final class NetLoggerCli implements ElnamaCli {

  private static final int PORT_NUMBER = 8888;
  private static final String PRESSED_WRONG_BUTTON = "invalid enter only integers";
  private final String timestampStartup = TimeUtils.getTimestamp();
  private final Map<Integer, String> timestampLogRetrievals = new HashMap<>();
  private final BlockingQueue<NetMessage> queue = new ArrayBlockingQueue<>(10);
  private NetLoggerModel netLoggerModel;

  @Override
  public void initialize() {
    try {
      ElnamaErrorLog.elnamaLoggerSetup();
    } catch (IOException exc) {
      LOGGER.log(Level.INFO, exc.getMessage(), exc);
    }
    netLoggerModel = new NetLoggerModel("NET1", 0, queue, PORT_NUMBER);
    new NetMessageConsumer("MSG1", 0, queue);
    printlnOut("Waiting for incoming data!");
  }

  @Override
  public boolean runExecution() {
    return switch (readFromKeyboard()) {
      case "log" -> {
        getLog();
        yield true;
      }
      case "send" -> {
        sendCommand();
        yield true;
      }
      case "sql" -> {
        toggleSql();
        yield true;
      }
      case "q" -> {
        closeThreads();
        ElnamaErrorLog.deleteEmptyErrorLog();
        yield false;
      }
      default -> {
        printlnOut("Invalid input, try again.");
        yield true;
      }
    };
  }

  private void getLog() {
    printOut("Enter serial number: ");
    try {
      int serialNumber = Integer.parseInt(readFromKeyboard());
      String timestamp = timestampLogRetrievals.computeIfAbsent(serialNumber,
          key -> timestampStartup);
      netLoggerModel.getLog(serialNumber, timestamp);
      updateTimestamp(serialNumber);
    } catch (NumberFormatException exc) {
      printlnOut(PRESSED_WRONG_BUTTON);
    }
  }

  private void updateTimestamp(int serialNumber) {
    timestampLogRetrievals.put(serialNumber, TimeUtils.getTimestamp());
  }

  private void sendCommand() {
    try {
      netLoggerModel.sendCommand(enterId(), enterCmd());
    } catch (NumberFormatException exc) {
      printlnOut(PRESSED_WRONG_BUTTON);
    }
  }

  private int enterId() {
    printOut("Enter serial number: ");
    return Integer.parseInt(readFromKeyboard());
  }

  private String enterCmd() {
    printOut("Enter command to send: ");
    return readFromKeyboard();
  }

  private void toggleSql() {
    printOut("Turn true/false storing messages in database: ");
    try {
      netLoggerModel.toggleUseSql(Boolean.parseBoolean(readFromKeyboard()));
    } catch (NumberFormatException exc) {
      printlnOut(PRESSED_WRONG_BUTTON);
    }
  }

  private void closeThreads() {
    if (netLoggerModel != null) {
      netLoggerModel.closeAllPorts();
    }
  }
}