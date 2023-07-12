package se.elnama.core.net.logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.DatatypeConverter;
import se.elnama.core.net.msg.NetMessage;
import se.elnama.core.net.msg.NetMessageStoreSql.SendRequisite;
import se.elnama.lib.core.ric.msg.Message;

final class NetSenderModel extends Message implements Callable<String> {

  private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
  private final int id;
  private final String command;
  private final DatagramSocket socket;
  private final CountDownLatch latch = new CountDownLatch(1);
  private String ipAddress;
  private int port;
  private byte[] block1;

  NetSenderModel(int id, String command, DatagramSocket sock) {
    this.id = id;
    this.command = command;
    socket = sock;
  }

  @Override
  public String call() {
    try {
      latch.await();
    } catch (InterruptedException exc) {
      Thread.currentThread().interrupt();
      LOGGER.warning("Interrupted while waiting for setSendRequisiteSql() to complete.");
    }
    byte[] hexCommand = DatatypeConverter.parseHexBinary(getCmdFromFile(command));
    return sendCommand(updateCommand(hexCommand));
  }

  void setSendRequisiteSql() {
    SendRequisite sendRequisite = SendRequisite.INSTANCE.getInstance();
    sendRequisite.setSendRequisite(id);
    ipAddress = sendRequisite.getIpAddress();
    port = sendRequisite.getPort();
    block1 = sendRequisite.getBlock1();

    latch.countDown();
  }

  void setSendRequisiteList(NetMessage lastMessage) {
    ipAddress = lastMessage.getIpAddress();
    port = lastMessage.getPort();
    block1 = convertToHexBinary(lastMessage.getBlock1());

    latch.countDown();
  }

  private String getCmdFromFile(String cmd) {
    File file = new File("cmd.txt");
    String cmdFromFile = "";

    try (Scanner scanner = new Scanner(new FileReader(file))) {
      while (scanner.hasNext()) {
        if (scanner.findInLine(cmd + ":") != null) {
          cmdFromFile = scanner.nextLine();
        } else {
          scanner.nextLine();
        }
      }
    } catch (FileNotFoundException exc) {
      LOGGER.log(Level.INFO, exc.getMessage(), exc);
    }
    return cmdFromFile;
  }

  private String sendCommand(byte[] cmd) {
    String messageSent = "failed";
    byte[] encryptedCommand = xtea.encrypt(cmd, cmd.length);

    try {
      InetAddress address = InetAddress.getByName(ipAddress);
      DatagramPacket datagramPacket = new DatagramPacket(encryptedCommand, encryptedCommand.length,
          address, port);
      socket.send(datagramPacket);
      messageSent = address.getHostAddress() + " : " + port + " : " + bytesToHex(cmd);
    } catch (IOException exc) {
      LOGGER.log(Level.INFO, exc.getMessage(), exc);
    }
    return messageSent;
  }

  private byte[] updateCommand(byte[] cmd) {
    byte serialMostSig = block1[5];
    byte serialLeastSig = block1[4];
    byte increment = block1[3];
    byte protocol = block1[0];
    cmd[5] = serialMostSig;
    cmd[4] = serialLeastSig;
    cmd[3] = increment;
    cmd[0] = protocol;

    return cmd;
  }

  private byte[] convertToHexBinary(String hex) {
    String hexNoSpaces = hex.replaceAll("\\s", "");
    return DatatypeConverter.parseHexBinary(hexNoSpaces);
  }
}