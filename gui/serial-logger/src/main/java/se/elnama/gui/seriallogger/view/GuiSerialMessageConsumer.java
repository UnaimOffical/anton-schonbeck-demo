package se.elnama.gui.seriallogger.view;

import java.util.concurrent.BlockingQueue;
import javafx.application.Platform;
import se.elnama.core.serial.msg.SerialMessage;
import se.elnama.core.serial.msg.SerialMessageConsumer;
import se.elnama.gui.seriallogger.Main;

final class GuiSerialMessageConsumer extends SerialMessageConsumer {

  GuiSerialMessageConsumer(String name, int threadNumber, BlockingQueue<SerialMessage> queue) {
    super(name, threadNumber, queue);
  }

  @Override
  public void printHex() {
    Platform.runLater(() -> Main.getLoggerLayoutControllerHandle()
        .updateTextAreaHex(getSerialMessage().getHexMessage()));
  }

  @Override
  public void printAscii() {
    Platform.runLater(() -> Main.getLoggerLayoutControllerHandle()
        .updateTextAreaAscii(getSerialMessage().getAsciiMessage()));
  }
}