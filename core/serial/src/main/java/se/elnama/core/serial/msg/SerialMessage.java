package se.elnama.core.serial.msg;

import java.util.Arrays;
import java.util.regex.Pattern;
import se.elnama.lib.core.ric.msg.Message;

public final class SerialMessage extends Message {

  private static final Pattern ASCII = Pattern.compile("^[\\u0000-\\u007F]*$");
  private static final Pattern TAB_NEWLINE = Pattern.compile("\\s\n");
  private static final int QI_POS = 18;
  private byte[] rawMessage;
  private String hexMessage;
  private String asciiMessage;

  public SerialMessage(byte[] message, boolean decrypt) {
    super();
    rawMessage = message;

    setHexMessage();
    setAsciiMessage(decrypt);
  }

  public SerialMessage(String message) {
    hexMessage = message;
    asciiMessage = message;
  }

  public static SerialMessage setHexAndAsciiMessage(byte[] message, boolean decrypt) {
    return new SerialMessage(message, decrypt);
  }

  public static SerialMessage setInfoMessage(String message) {
    return new SerialMessage(message);
  }

  private static boolean isAscii(String stringToTest) {
    return ASCII.matcher(stringToTest).matches();
  }

  public void setHexMessage() {
    hexMessage = bytesToHex(rawMessage);
  }

  public String getHexMessage() {
    return hexMessage;
  }

  public String getAsciiMessage() {
    return asciiMessage;
  }

  public void setAsciiMessage(boolean decrypt) {
    String message = new String(rawMessage);
    String messageWithoutTabsNewlines = TAB_NEWLINE.matcher(message).replaceAll(" ");

    if (!decrypt || isAscii(messageWithoutTabsNewlines)) {
      asciiMessage = messageWithoutTabsNewlines;
    } else if (isQiSend(rawMessage)) {
      byte[] payload = grabPayloadSend(messageWithoutTabsNewlines);
      byte[] payloadDecrypted = xtea.decrypt(payload, payload.length);
      byte[] headAndTail = grabHeadAndTailSend(messageWithoutTabsNewlines);
      asciiMessage = assembleSend(payloadDecrypted, headAndTail);
    } else if (isQiReceive(rawMessage)) {
      byte[] payload = grabPayloadReceive(messageWithoutTabsNewlines);
      byte[] payloadDecrypted = xtea.decrypt(payload, payload.length);
      byte[] head = grabHeadReceive(payload.length);
      asciiMessage = assembleReceive(payloadDecrypted, head);
    }
  }

  private boolean isQiSend(byte[] readData) {
    boolean isSend = false;
    int sum = 0;
    int target = 298;

    for (int i = 5; i < 9; i++) {
      sum += readData[i];
    }
    if (sum == target) {
      isSend = true;
    }
    return isSend;
  }

  private byte[] grabPayloadSend(String message) {
    int payloadSize = getSendPayloadSize(message);
    byte[] encryptedData = new byte[payloadSize];

    System.arraycopy(rawMessage, QI_POS, encryptedData, 0, payloadSize);
    return encryptedData;
  }

  private byte[] grabHeadAndTailSend(String message) {
    int payloadSize = getSendPayloadSize(message);
    byte[] headAndTail = new byte[rawMessage.length - payloadSize];

    System.arraycopy(rawMessage, 0, headAndTail, 0, QI_POS);
    System.arraycopy(rawMessage, QI_POS + payloadSize, headAndTail, QI_POS,
        headAndTail.length - QI_POS);
    return headAndTail;
  }

  private int getSendPayloadSize(String data) {
    String qiPayloadSize = data.substring(data.indexOf("=") + 1, data.indexOf(">") - 4);
    return Integer.parseInt(qiPayloadSize);
  }

  private String assembleSend(byte[] payload, byte[] headAndTail) {
    String payloadHex = bytesToHex(payload);
    String headAndTailAscii = new String(headAndTail);
    String headAndTailAsciiWithoutTabsNewlines = TAB_NEWLINE.matcher(headAndTailAscii)
        .replaceAll(" ");

    StringBuilder asciiMessageSend = new StringBuilder(headAndTailAsciiWithoutTabsNewlines);
    asciiMessageSend.replace(16, 17, payloadHex + " ");
    return asciiMessageSend.toString();
  }

  private boolean isQiReceive(byte[] readData) {
    boolean isReceive = false;
    int sum = 0;
    int target = 304;

    for (int i = 2; i < 6; i++) {
      sum += readData[i];
    }
    if (sum == target) {
      isReceive = true;
    }
    return isReceive;
  }

  private byte[] grabPayloadReceive(String message) {
    int indexPayload = message.indexOf("UDP:") + 4;
    return Arrays.copyOfRange(rawMessage, indexPayload, rawMessage.length);
  }

  private byte[] grabHeadReceive(int payloadSize) {
    return Arrays.copyOf(rawMessage, rawMessage.length - payloadSize);
  }

  private String assembleReceive(byte[] payload, byte[] head) {
    String payloadHex = bytesToHex(payload);
    String headAscii = new String(head);
    String headAsciiWithoutTabsNewlines = TAB_NEWLINE.matcher(headAscii).replaceAll(" ");

    return headAsciiWithoutTabsNewlines + payloadHex;
  }
}