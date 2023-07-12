package se.elnama.gui.netlogger.view;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import se.elnama.core.net.logger.NetLoggerModel;
import se.elnama.core.net.msg.NetMessage;
import se.elnama.lib.util.TimeUtils;

public final class NetLoggerLayoutController {

  private static final int CHAR_LIMIT = 100000;
  private static final int CHAR_REMOVE = 90000;
  private static final double SCROLL_HOLD = 17;
  private static final int PORT_NUMBER = 8888;
  private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
  private final String timestampStartup = TimeUtils.getTimestamp();
  private final Map<Integer, String> timestampLogRetrievals = new HashMap<>();
  private final BlockingQueue<NetMessage> queue = new ArrayBlockingQueue<>(10);
  @FXML
  TextArea textAreaHex;
  @FXML
  TextField textSendCmd;
  @FXML
  TextField textSerialNr;
  @FXML
  ToggleButton toggleUseSql;
  private NetLoggerModel netLoggerModel;

  public void updateTextAreaHex(String data) {
    if (data.length() != 0) {
      double scrollTop = textAreaHex.getScrollTop();
      double scrollLeft = textAreaHex.getScrollLeft();
      textAreaHex.setText(TimeUtils.printWithTime(data) + '\n' + textAreaHex.getText());
      if (scrollTop != 0.0) {
        textAreaHex.setScrollTop(scrollTop + SCROLL_HOLD);
        textAreaHex.setScrollLeft(scrollLeft);
      }
      removeTextAboveLimit(textAreaHex);
    }
  }

  public void closeThreads() {
    if (netLoggerModel != null) {
      netLoggerModel.closeAllPorts();
    }
  }

  private <T extends TextInputControl> void removeTextAboveLimit(T t) {
    if (t.getLength() > CHAR_LIMIT) {
      int posCaret = CHAR_LIMIT - CHAR_REMOVE;
      t.deleteText(posCaret, t.getLength());
    }
  }

  @FXML
  private void closeApplication() {
    closeThreads();
    Stage stage = (Stage) textAreaHex.getScene().getWindow();
    stage.close();
  }

  @FXML
  private void initialize() {
    textAreaHex.setId("textHex");
    netLoggerModel = new NetLoggerModel("NET1", 0, queue, PORT_NUMBER);
    new GuiNetMessageConsumer("MSG1", 0, queue);
  }

  @FXML
  private void sendCommand() {
    if (textSendCmd.getLength() == 0 || textSerialNr.getLength() == 0) {
      updateTextAreaHex("Make sure that both a serial number and a command have been entered");
      return;
    }

    int serialNumber = Integer.parseInt(textSerialNr.getText());
    String command = textSendCmd.getText();
    netLoggerModel.sendCommand(serialNumber, command);
  }

  @FXML
  private void getLog() {
    if (textSerialNr.getLength() == 0) {
      updateTextAreaHex("Enter Serial Number");
      return;
    }

    int serialNumber = Integer.parseInt(textSerialNr.getText());
    String timestamp = timestampLogRetrievals.computeIfAbsent(serialNumber,
        key -> timestampStartup);
    netLoggerModel.getLog(serialNumber, timestamp);
    updateTimestamp(serialNumber);
  }

  @FXML
  private void toggleUseSql() {
    if (toggleUseSql.isSelected()) {
      netLoggerModel.toggleUseSql(true);
      toggleUseSql.setText("ON");
    } else {
      netLoggerModel.toggleUseSql(false);
      toggleUseSql.setText("OFF");
    }
  }

  private void updateTimestamp(int serialNumber) {
    timestampLogRetrievals.put(serialNumber, TimeUtils.getTimestamp());
  }

  @FXML
  private void menuSaveFile() {
    FileChooser fileChooser = new FileChooser();
    FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter(
        "TXT files (*.txt)", "*.txt");

    fileChooser.getExtensionFilters().add(extensionFilter);
    File file = fileChooser.showSaveDialog(textAreaHex.getScene().getWindow());
    saveFile(textAreaHex.getText(), file);
  }

  private void saveFile(String content, File file) {
    try (FileWriter fileWriter = new FileWriter(file)) {
      fileWriter.write(content);
    } catch (IOException exc) {
      LOGGER.log(Level.INFO, exc.getMessage(), exc);
    }
  }

  @FXML
  private void menuClear() {
    textAreaHex.clear();
  }

  @FXML
  private void menuAbout() {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle("Elnama Logger");
    alert.setHeaderText("About");
    alert.setContentText("""
        Owner: Elnama AB
        Program: GRIC NET Logger
        Version: 1.1.0.0
        """);
    ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons()
        .add(new Image("se/elnama/gui/seriallogger/images/Eicon.jpg"));

    ButtonType buttonOk = new ButtonType("Ok");
    alert.getButtonTypes().set(0, buttonOk);
    alert.showAndWait();
  }
}