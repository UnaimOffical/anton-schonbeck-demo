package se.elnama.cli.seriallogger;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import se.elnama.core.serial.logger.GetAvailablePorts;
import se.elnama.core.serial.logger.SerialLoggerModel;
import se.elnama.core.serial.msg.SerialMessage;
import se.elnama.core.serial.msg.SerialMessageConsumer;
import se.elnama.lib.cli.ElnamaCli;
import se.elnama.lib.log.ElnamaErrorLog;

final class SerialLoggerCli implements ElnamaCli {

  private final BlockingQueue<SerialMessage> queue = new ArrayBlockingQueue<>(10);
  private SerialLoggerModel serialLoggerModel;

  @Override
  public void initialize() {
    try {
      ElnamaErrorLog.elnamaLoggerSetup();
    } catch (IOException exc) {
      LOGGER.log(Level.INFO, exc.getMessage(), exc);
    }
    serialLoggerModel = new SerialLoggerModel(getComPort(), 0, queue);
    new SerialMessageConsumer("MSG1", 0, queue);
    printlnOut("Waiting for incoming data");
  }

  @Override
  public boolean runExecution() {
    return switch (readFromKeyboard()) {
      case "set" -> {
        setComPortSettings();
        yield true;
      }
      case "get" -> {
        getComPortSettings();
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

  private void setComPortSettings() {
    printOut("Enter Baud Rate: ");
    serialLoggerModel.setBaudRate(Integer.parseInt(readFromKeyboard()));

    printOut("Enter Interval (ms): ");
    serialLoggerModel.setIntervalTime(Integer.parseInt(readFromKeyboard()));

    printOut("Decrypt true(on)/false(off): ");
    serialLoggerModel.setDecrypt(Boolean.parseBoolean(readFromKeyboard()));
  }

  private void getComPortSettings() {
    printlnOut("Baud Rate: " + serialLoggerModel.getBaudRate());
    printlnOut("Interval (ms): " + serialLoggerModel.getIntervalTime());
    printlnOut("Decryption: " + serialLoggerModel.getDecrypt());
  }

  private void closeThreads() {
    if (serialLoggerModel != null) {
      serialLoggerModel.closeAllPorts();
    }
  }

  private String getComPort() {
    String portChose = "";
    try {
      String[] ports = getAvailablePorts();
      printAvailablePorts(ports);
      portChose = ports[choosePort()];
    } catch (InterruptedException | ExecutionException exc) {
      Thread.currentThread().interrupt();
      LOGGER.log(Level.INFO, exc.getMessage(), exc);
    }
    return portChose;
  }

  private String[] getAvailablePorts() throws ExecutionException, InterruptedException {
    GetAvailablePorts task = new GetAvailablePorts();
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Future<String[]> future = executor.submit(task);
    executor.shutdown();
    return future.get();
  }

  private void printAvailablePorts(String[] availablePorts) {
    int i = 0;
    for (String portName : availablePorts) {
      printlnOut(++i + ". " + portName + " Available");
    }
  }

  private int choosePort() {
    printOut("choose port by entering the corresponding number to the left of the port: ");
    int port = Integer.parseInt(readFromKeyboard());
    return --port;
  }
}