package se.elnama.core.net.msg;

import static se.elnama.lib.util.TimeUtils.getDateTimeFile;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class NetMessageStoreMap {

  private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
  Map<Integer, List<NetMessage>> messageMap = new HashMap<>();

  public void putMessage(NetMessage message) {
    int id = message.getId();

    messageMap.computeIfAbsent(id, k -> new ArrayList<>());
    messageMap.get(id).add(message);
  }

  public void printMessageListToFile(int id) {
    File file = new File("./log/" + getDateTimeFile() + "-logSimple.txt");
    List<NetMessage> messageList = messageMap.get(id);

    if (messageList == null) {
      LOGGER.log(Level.WARNING, () -> "No message list found for id: " + id);
      return;
    }

    try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)))) {
      messageList.forEach(s -> pw.println(s.getTimestamp() + " - " + s.getHexMessage()));
    } catch (IOException exc) {
      Thread.currentThread().interrupt();
      LOGGER.log(Level.INFO, exc.getMessage(), exc);
    }
  }

  public NetMessage getLastMessage(int id) {
    return messageMap.get(id).get(messageMap.get(id).size() - 1);
  }
}