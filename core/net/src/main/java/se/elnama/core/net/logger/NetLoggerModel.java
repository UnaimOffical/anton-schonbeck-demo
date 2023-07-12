package se.elnama.core.net.logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import se.elnama.core.net.msg.NetMessage;
import se.elnama.core.net.msg.NetMessageStoreMap;
import se.elnama.core.net.msg.NetMessageStoreSql;

public final class NetLoggerModel implements Runnable {

  private static final boolean[] STOPPED = new boolean[1];
  private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
  private static boolean useSql = true;
  private final int threadNumber;
  private final int portNumber;
  private final BlockingQueue<NetMessage> queue;
  private final NetMessageStoreSql storeSql = new NetMessageStoreSql();
  private final NetMessageStoreMap storeMap = new NetMessageStoreMap();
  private final ExecutorService executor = Executors.newFixedThreadPool(2);
  private DatagramSocket socket;

  public NetLoggerModel(String name, int threadNumber, BlockingQueue<NetMessage> queue,
      int portNumber) {
    Thread thread = new Thread(this, name);
    this.threadNumber = threadNumber;
    this.portNumber = portNumber;
    this.queue = queue;

    thread.start();
  }

  public static boolean[] getStopped() {
    return STOPPED;
  }

  public synchronized boolean getUseSql() {
    return useSql;
  }

  public synchronized void toggleUseSql(boolean toggle) {
    useSql = toggle;
  }

  @Override
  public void run() {
    try (DatagramSocket sock = new DatagramSocket(portNumber)) {
      socket = sock;
      byte[] inBuffer = new byte[64];
      DatagramPacket datagramPacket = new DatagramPacket(inBuffer, inBuffer.length);

      while (!STOPPED[threadNumber]) {
        socket.receive(datagramPacket);
        NetMessage message = NetMessage.setHexMessage(datagramPacket);
        if (message.getValidMessage()) {
          queue.put(message);
          storeMessage(message);
        } else {
          queue.put(NetMessage.setInfoMessage(message.getHexMessage()));
        }
      }
    } catch (IOException | InterruptedException exc) {
      Thread.currentThread().interrupt();
      LOGGER.log(Level.INFO, exc.getMessage(), exc);
    }
  }

  public void getLog(int id, String timestamp) {
    Runnable runnable = () -> {
      if (!(useSql && storeSql.callSelectLog(id, timestamp))) {
        storeMap.printMessageListToFile(id);
      }
    };
    executor.execute(runnable);
  }

  public void sendCommand(int id, String commandToSend) {
    NetSenderModel netSenderModel = new NetSenderModel(id, commandToSend, socket);
    setSendRequisites(netSenderModel, id);

    try {
      Future<String> future = executor.submit(netSenderModel);
      String messageSent = future.get();
      queue.put(NetMessage.setInfoMessage(messageSent));
    } catch (InterruptedException | ExecutionException exc) {
      Thread.currentThread().interrupt();
      LOGGER.log(Level.INFO, exc.getMessage(), exc);
    }
  }

  public void closeAllPorts() {
    Arrays.fill(STOPPED, true);
    try {
      byte[] empty = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
      DatagramPacket datagramPacket = new DatagramPacket(empty, empty.length,
          InetAddress.getLocalHost(), socket.getLocalPort());

      socket.send(datagramPacket);
      queue.put(NetMessage.setInfoMessage("EXITED"));
      executor.shutdown();
    } catch (IOException | InterruptedException exc) {
      Thread.currentThread().interrupt();
      LOGGER.log(Level.INFO, exc.getMessage(), exc);
    }
  }

  private void setSendRequisites(NetSenderModel netSenderModel, int id) {
    Runnable runnable = () -> {
      if (useSql) {
        netSenderModel.setSendRequisiteSql();
      } else {
        netSenderModel.setSendRequisiteList(storeMap.getLastMessage(id));
      }
    };
    executor.execute(runnable);
  }

  private void storeMessage(NetMessage message) {
    Runnable runnable = () -> {
      if (!(useSql && storeSql.callInsertLog(message))) {
        storeMap.putMessage(message);
      }
    };
    executor.execute(runnable);
  }
}