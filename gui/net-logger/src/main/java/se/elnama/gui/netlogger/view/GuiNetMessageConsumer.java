package se.elnama.gui.netlogger.view;

import java.util.concurrent.BlockingQueue;
import javafx.application.Platform;
import se.elnama.core.net.msg.NetMessage;
import se.elnama.core.net.msg.NetMessageConsumer;
import se.elnama.gui.netlogger.Main;

final class GuiNetMessageConsumer extends NetMessageConsumer {

  GuiNetMessageConsumer(String name, int threadNumber, BlockingQueue<NetMessage> queue) {
    super(name, threadNumber, queue);
  }

  @Override
  public void printHex() {
    StringBuilder formattedMessage = new StringBuilder(getNetMessage().getHexMessage());

    try {
      for (int i = 0, j = 24; i < formattedMessage.length() / 24; i++, j += 26) {
        formattedMessage.insert(j, " |");
      }
      Platform.runLater(() -> Main.getLoggerLayoutControllerHandle()
          .updateTextAreaHex(formattedMessage.toString()));
    } catch (StringIndexOutOfBoundsException exc) {
      Platform.runLater(() -> Main.getLoggerLayoutControllerHandle()
          .updateTextAreaHex(getNetMessage().getHexMessage()));
    }
  }
}