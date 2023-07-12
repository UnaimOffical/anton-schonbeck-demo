package se.elnama.core.net.msg;

import java.net.DatagramPacket;
import se.elnama.lib.core.ric.msg.Message;

public final class NetMessage extends Message {

  private static final String IPV4_MAPPING = "::ffff:";
  private String ipAddress;
  private int port;
  private int length;
  private boolean validMessage = false;
  private String hexMessage;
  private int id;
  private String[] messageBlocks;

  public NetMessage(DatagramPacket datagramPacket) {
    super();
    ipAddress = IPV4_MAPPING + datagramPacket.getAddress().getHostAddress();
    port = datagramPacket.getPort();
    length = datagramPacket.getLength();

    checkValidMessage();
    setHexMessage(datagramPacket.getData());
    setId();
    divideMessageIntoBlocks();
  }

  public NetMessage(String message) {
    hexMessage = message;
    divideInfoMessageIntoBlocks(message);
  }

  public static NetMessage setHexMessage(DatagramPacket datagramPacket) {
    return new NetMessage(datagramPacket);
  }

  public static NetMessage setInfoMessage(String message) {
    return new NetMessage(message);
  }

  public String getIpAddress() {
    return ipAddress;
  }

  public int getPort() {
    return port;
  }

  public int getLength() {
    return length;
  }

  public String getHexMessage() {
    return hexMessage;
  }

  public int getId() {
    return id;
  }

  public String[] getMessageBlocks() {
    return messageBlocks;
  }

  public String getBlock1() {
    return messageBlocks[0];
  }

  public boolean getValidMessage() {
    return validMessage;
  }

  private void checkValidMessage() {
    validMessage = checkMessageLength() && checkMessageDivisible();
  }

  private boolean checkMessageLength() {
    int minLength = Protocol.MIN_LENGTH.getValue();
    int maxLength = Protocol.MAX_LENGTH.getValue();

    return length >= minLength && length <= maxLength;
  }

  private boolean checkMessageDivisible() {
    int blockLength = Protocol.BLOCK_LENGTH.getValue();
    return length % blockLength == 0;
  }

  private void setHexMessage(byte[] rawMessage) {
    byte[] messageDecrypted;
    if (validMessage) {
      messageDecrypted = xtea.decrypt(rawMessage, length);
    } else {
      messageDecrypted = fakeDecryptForCorrectFormatting(rawMessage);
    }
    hexMessage = bytesToHex(messageDecrypted);
  }

  private void setId() {
    if (validMessage) {
      int firstIndex = 16;
      String first = hexMessage.substring(firstIndex, firstIndex + 2);
      int secondIndex = 13;
      String second = hexMessage.substring(secondIndex, secondIndex + 2);
      String concatId = first + second;

      try {
        id = Integer.decode(concatId);
      } catch (NumberFormatException exc) {
        validMessage = false;
      }
    }
  }

  private void divideMessageIntoBlocks() {
    if (validMessage) {
      messageBlocks = new String[Protocol.NUMBER_OF_BLOCKS.getValue()];
      int twoDigitHexPlusSpace = 3;
      int blockLength = Protocol.BLOCK_LENGTH.getValue() * twoDigitHexPlusSpace;

      for (int i = 0, s = 0; i < hexMessage.length(); i += blockLength, s++) {
        messageBlocks[s] = hexMessage.substring(i + 1, i + blockLength);
      }
    }
  }

  private void divideInfoMessageIntoBlocks(String message) {
    messageBlocks = new String[Protocol.NUMBER_OF_BLOCKS.getValue()];
    messageBlocks[0] = message;

    for (int i = 1; i < messageBlocks.length; i++) {
      messageBlocks[i] = "";
    }
  }

  private byte[] fakeDecryptForCorrectFormatting(byte[] rawMessage) {
    byte[] message = new byte[length];

    System.arraycopy(rawMessage, 0, message, 0, length);
    return message;
  }
}