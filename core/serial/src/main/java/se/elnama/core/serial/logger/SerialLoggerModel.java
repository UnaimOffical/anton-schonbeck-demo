package se.elnama.core.serial.logger;

import static se.elnama.lib.util.TimeUtils.getDateTimeFile;

import com.fazecast.jSerialComm.SerialPort;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import se.elnama.core.serial.msg.SerialMessage;
import se.elnama.lib.util.TimeUtils;

public final class SerialLoggerModel implements Runnable {

  private static final boolean[] STOPPED = new boolean[4];
  private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
  private static final String BAUD_RATE = "BaudRate";
  private static final String INTERVAL = "Interval";
  private static final String MODE = "Mode";
  private static final String DECRYPT = "Decrypt";
  private final String comPortName;
  private final int threadNumber;
  private final BlockingQueue<SerialMessage> queue;
  private final SerialPort serialPort;
  private final Preferences prefs = Preferences.userRoot().node(SerialLoggerModel.class.getName());

  public SerialLoggerModel(String name, int threadNumber, BlockingQueue<SerialMessage> queue) {
    Thread thread = new Thread(this, name);
    comPortName = name;
    this.threadNumber = threadNumber;
    this.queue = queue;
    serialPort = SerialPort.getCommPort(name);

    thread.start();
  }

  public static boolean[] getStopped() {
    return STOPPED;
  }

  private static void setStoppedTrue(int threadNumber) {
    STOPPED[threadNumber] = true;
  }

  public void run() {
    createDirectoryIfNotFound();
    try (FileOutputStream fOut = new FileOutputStream("./log/" + getDateTimeFile() + "-log.txt")) {
      PrintStream logFile = new PrintStream(fOut, true);

      openAndInitializePort();
      while (!STOPPED[threadNumber]) {
        byte[] readData = readDataWhenAvailable();
        SerialMessage message = SerialMessage.setHexAndAsciiMessage(readData,
            prefs.getBoolean(comPortName + DECRYPT, false));
        logFile.println(TimeUtils.printWithTime(message.getAsciiMessage()));
        logFile.println(TimeUtils.printWithTime(message.getHexMessage()));
        queue.put(message);
      }
    } catch (IOException | InterruptedException exc) {
      Thread.currentThread().interrupt();
      LOGGER.log(Level.INFO, exc.getMessage(), exc);
    } finally {
      serialPort.closePort();
    }
  }

  public void setBaudRate(int baudRate) {
    prefs.putInt(comPortName + BAUD_RATE, baudRate);
    if (serialPort.isOpen()) {
      serialPort.setBaudRate(prefs.getInt(comPortName + BAUD_RATE, 38400));
    }
  }

  public int getBaudRate() {
    if (serialPort.isOpen()) {
      return serialPort.getBaudRate();
    }
    return prefs.getInt(comPortName + BAUD_RATE, 38400);
  }

  public void setIntervalTime(int interval) {
    prefs.putInt(comPortName + INTERVAL, interval);
    if (serialPort.isOpen()) {
      final int writeTimeout = 0;
      serialPort.setComPortTimeouts(
          prefs.getInt(comPortName + MODE, SerialPort.TIMEOUT_READ_BLOCKING),
          prefs.getInt(comPortName + INTERVAL, 50), writeTimeout);
    }
  }

  public int getIntervalTime() {
    if (serialPort.isOpen()) {
      return serialPort.getReadTimeout();
    }
    return prefs.getInt(comPortName + INTERVAL, 50);
  }

  public void setTimeoutMode(int timeoutMode) {
    prefs.putInt(comPortName + MODE, timeoutMode);
    if (serialPort.isOpen()) {
      final int writeTimeout = 0;
      serialPort.setComPortTimeouts(
          prefs.getInt(comPortName + MODE, SerialPort.TIMEOUT_READ_BLOCKING),
          serialPort.getReadTimeout(), writeTimeout);
    }
  }

  public void setDecrypt(boolean decrypt) {
    prefs.putBoolean(comPortName + DECRYPT, decrypt);
  }

  public boolean getDecrypt() {
    return prefs.getBoolean(comPortName + DECRYPT, false);
  }

  public void closePort(int threadNumber) {
    setStoppedTrue(threadNumber);
    if (!queue.offer(SerialMessage.setInfoMessage("EXITED"))) {
      Thread.currentThread().interrupt();
    }
  }

  public void closeAllPorts() {
    Arrays.fill(STOPPED, true);
    if (!queue.offer(SerialMessage.setInfoMessage("EXITED"))) {
      Thread.currentThread().interrupt();
    }
  }

  private void openAndInitializePort() {
    if (serialPort.openPort()) {
      setBaudRate(prefs.getInt(comPortName + BAUD_RATE, 38400));
      setIntervalTime(prefs.getInt(comPortName + INTERVAL, 50));
      setTimeoutMode(prefs.getInt(comPortName + MODE, SerialPort.TIMEOUT_READ_BLOCKING));
      setDecrypt(prefs.getBoolean(comPortName + DECRYPT, false));
    } else {
      try {
        queue.put(SerialMessage.setInfoMessage("FAILED TO OPEN / UNAVAILABLE PORT"));
      } catch (InterruptedException exc) {
        Thread.currentThread().interrupt();
        LOGGER.log(Level.INFO, exc.getMessage(), exc);
      }
      setStoppedTrue(threadNumber);
    }
  }

  private byte[] readDataWhenAvailable() {
    byte[] serialPortReadBuffer = new byte[100];
    int lengthOfReceivedData = 0;

    while (!STOPPED[threadNumber] && lengthOfReceivedData == 0) {
      lengthOfReceivedData = serialPort.readBytes(serialPortReadBuffer,
          serialPortReadBuffer.length);
    }
    return Arrays.copyOf(serialPortReadBuffer, lengthOfReceivedData);
  }

  private void createDirectoryIfNotFound() {
    File directory = new File("./log/");

    if (!directory.exists()) {
      directory.mkdirs();
    }
  }
}