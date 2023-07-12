package se.elnama.core.net.msg;

import java.io.PrintStream;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import se.elnama.core.net.logger.NetLoggerModel;

public class NetMessageConsumer implements Runnable {

  private static final boolean[] STOPPED = NetLoggerModel.getStopped();
  private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
  private final int threadNumber;
  private final BlockingQueue<NetMessage> queue;
  private final PrintStream printStream;
  private NetMessage message;

  public NetMessageConsumer(String name, int threadNumber, BlockingQueue<NetMessage> queue) {
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
      }
    } catch (InterruptedException exc) {
      Thread.currentThread().interrupt();
      LOGGER.log(Level.INFO, exc.getMessage(), exc);
    }
  }

  public void printHex() {
    printStream.print(getNetMessage().getTimestamp() + " - ");
    formatMessageVerticalBar();
  }

  protected NetMessage getNetMessage() {
    return message;
  }

  private void formatMessageVerticalBar() {
    StringBuilder formattedMessage = new StringBuilder(getNetMessage().getHexMessage());

    try {
      for (int i = 0, j = 24; i < formattedMessage.length() / 24; i++, j += 26) {
        formattedMessage.insert(j, " |");
      }
      printStream.println(formattedMessage);
    } catch (StringIndexOutOfBoundsException exc) {
      printStream.println(getNetMessage().getHexMessage());
    }
  }
}