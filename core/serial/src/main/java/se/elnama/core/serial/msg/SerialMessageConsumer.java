package se.elnama.core.serial.msg;

import java.io.PrintStream;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import se.elnama.core.serial.logger.SerialLoggerModel;

public class SerialMessageConsumer implements Runnable {

  private static final boolean[] STOPPED = SerialLoggerModel.getStopped();
  private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
  private final int threadNumber;
  private final BlockingQueue<SerialMessage> queue;
  private final PrintStream printStream;
  private SerialMessage message;

  public SerialMessageConsumer(String name, int threadNumber, BlockingQueue<SerialMessage> queue) {
    Thread thread = new Thread(this, name);
    this.threadNumber = threadNumber;
    this.queue = queue;
    printStream = System.out;

    thread.start();
  }

  @Override
  public void run() {
    try {
      while (!STOPPED[threadNumber]) {
        message = queue.take();
        printHex();
        printAscii();
      }
    } catch (InterruptedException exc) {
      Thread.currentThread().interrupt();
      LOGGER.log(Level.INFO, exc.getMessage(), exc);
    }
  }

  public void printHex() {
    printStream.println(message.getHexMessage());
  }

  public void printAscii() {
    printStream.println(message.getAsciiMessage());
  }

  protected SerialMessage getSerialMessage() {
    return message;
  }
}