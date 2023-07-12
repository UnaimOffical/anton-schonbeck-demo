package se.elnama.gui.seriallogger.view;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import se.elnama.core.serial.logger.GetAvailablePorts;
import se.elnama.core.serial.logger.SerialLoggerModel;
import se.elnama.core.serial.msg.SerialMessage;
import se.elnama.lib.util.TimeUtils;

public final class SerialLoggerLayoutController {

  private static final int CHAR_LIMIT = 100000;
  private static final int CHAR_REMOVE = 90000;
  private static final double SCROLL_HOLD = 8.5;
  private static final String LABEL_BAUD = "Baud Rate : ";
  private static final String LABEL_INTERVAL = "Interval (ms) : ";
  private static final String DEFAULT_BAUD_RATE = "38400";
  private static final String BAUD_RATE = "BaudRate";
  private static final String INTERVAL = "Interval";
  private static final String DECRYPT = "Decrypt";
  private static final int TIMEOUT_READ_BLOCKING = 16;
  private static final int TIMEOUT_READ_SEMI_BLOCKING = 1;
  private static final int TIMEOUT_NONBLOCKING = 0;
  private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
  private final BlockingQueue<SerialMessage> queue = new ArrayBlockingQueue<>(10);
  private final Preferences prefs = Preferences.userRoot()
      .node("se.elnama.core.serial.logger.SerialLoggerModel");
  @FXML
  TextArea textAreaHex;
  @FXML
  TextArea textAreaAscii;
  @FXML
  TextField textBaud;
  @FXML
  Label labelBaud;
  @FXML
  TextField textInterval;
  @FXML
  Label labelInterval;
  @FXML
  CheckBox checkOne;
  @FXML
  CheckBox checkTwo;
  @FXML
  CheckBox checkThree;
  @FXML
  CheckBox checkFour;
  @FXML
  CheckMenuItem checkMenuBlock;
  @FXML
  CheckMenuItem checkMenuSemiBlock;
  @FXML
  CheckMenuItem checkMenuNonBlock;
  @FXML
  ToggleButton toggleDecryption;
  private String comPortName;
  private SerialLoggerModel serialLoggerModel = null;

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

  public void updateTextAreaAscii(String data) {
    if (data.length() != 0) {
      double scrollTop = textAreaAscii.getScrollTop();
      double scrollLeft = textAreaAscii.getScrollLeft();
      textAreaAscii.setText(TimeUtils.printWithTime(data) + '\n' + textAreaAscii.getText());
      if (scrollTop != 0.0) {
        textAreaAscii.setScrollTop(scrollTop + SCROLL_HOLD);
        textAreaAscii.setScrollLeft(scrollLeft);
      }
      removeTextAboveLimit(textAreaAscii);
    }
  }

  public void closeThreads() {
    if (serialLoggerModel != null) {
      serialLoggerModel.closeAllPorts();
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
    Stage stage = (Stage) labelBaud.getScene().getWindow();
    stage.close();
  }

  @FXML
  private void initialize() {
    textAreaAscii.scrollTopProperty().bindBidirectional(textAreaHex.scrollTopProperty());
    textAreaHex.setId("textHex");
    getAvailablePorts();
    startSettingReadMode();
  }

  private void getAvailablePorts() {
    GetAvailablePorts task = new GetAvailablePorts();
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Future<String[]> future = executor.submit(task);
    executor.shutdown();

    try {
      String[] availablePorts = future.get();
      for (String portName : availablePorts) {
        updateTextAreaHex(portName + " Available");
      }
    } catch (InterruptedException | ExecutionException exc) {
      Thread.currentThread().interrupt();
      LOGGER.log(Level.INFO, exc.getMessage(), exc);
    }
  }

  private void startSettingReadMode() {
    switch (prefs.getInt("mode", TIMEOUT_READ_BLOCKING)) {
      case TIMEOUT_READ_BLOCKING -> checkMenuBlock.setSelected(true);
      case TIMEOUT_READ_SEMI_BLOCKING -> checkMenuSemiBlock.setSelected(true);
      case TIMEOUT_NONBLOCKING -> checkMenuNonBlock.setSelected(true);
      default -> {
        prefs.putInt("mode", TIMEOUT_READ_BLOCKING);
        checkMenuBlock.setSelected(true);
      }
    }
  }

  @FXML
  private void checkOne() {
    if (checkOne.isSelected()) {
      serialLoggerModel = new SerialLoggerModel("COM1", 0, queue);
      new GuiSerialMessageConsumer("MSG1", 0, queue);
      comPortName = "COM1";
      getInitialComPortSettings(comPortName);
    } else {
      serialLoggerModel.closePort(0);
    }
  }

  @FXML
  private void checkTwo() {
    if (checkTwo.isSelected()) {
      serialLoggerModel = new SerialLoggerModel("COM2", 1, queue);
      new GuiSerialMessageConsumer("MSG2", 0, queue);
      comPortName = "COM2";
      getInitialComPortSettings(comPortName);
    } else {
      serialLoggerModel.closePort(1);
    }
  }

  @FXML
  private void checkThree() {
    if (checkThree.isSelected()) {
      serialLoggerModel = new SerialLoggerModel("COM3", 2, queue);
      new GuiSerialMessageConsumer("MSG3", 0, queue);
      comPortName = "COM3";
      getInitialComPortSettings(comPortName);
    } else {
      serialLoggerModel.closePort(2);
    }
  }

  @FXML
  private void checkFour() {
    if (checkFour.isSelected()) {
      serialLoggerModel = new SerialLoggerModel("COM4", 3, queue);
      new GuiSerialMessageConsumer("MSG4", 0, queue);
      comPortName = "COM4";
      getInitialComPortSettings(comPortName);
    } else {
      serialLoggerModel.closePort(3);
    }
  }

  private void getInitialComPortSettings(String comPortName) {
    labelBaud.setText(LABEL_BAUD + prefs.getInt(comPortName + BAUD_RATE, 38400));
    labelInterval.setText(LABEL_INTERVAL + prefs.getInt(comPortName + INTERVAL, 50));

    if (serialLoggerModel.getDecrypt()) {
      toggleDecryption.setText("ON");
      prefs.putBoolean(comPortName + DECRYPT, true);
    } else {
      toggleDecryption.setText("OFF");
      prefs.putBoolean(comPortName + DECRYPT, false);
    }
  }

  @FXML
  private void setComBaudRate() {
    if (textBaud.getLength() == 0) {
      updateTextAreaHex("Empty values not allowed");
      return;
    }
    if (serialLoggerModel == null) {
      updateTextAreaHex("Select a COM port before changing baud rate");
      return;
    }

    String newBaudRate = textBaud.getText();
    serialLoggerModel.setBaudRate(Integer.parseInt(newBaudRate));
    labelBaud.setText(LABEL_BAUD + newBaudRate);
    textBaud.clear();
  }

  @FXML
  private void setIntervalTime() {
    if (textInterval.getLength() == 0) {
      updateTextAreaHex("Empty values not allowed");
      return;
    }
    if (serialLoggerModel == null) {
      updateTextAreaHex("Select a COM port before changing interval time");
      return;
    }

    String newTimeoutInterval = textInterval.getText();
    serialLoggerModel.setIntervalTime(Integer.parseInt(newTimeoutInterval));
    labelInterval.setText(LABEL_INTERVAL + newTimeoutInterval);
    textInterval.clear();
  }

  @FXML
  private void checkTimeoutBlock() {
    if (serialLoggerModel != null) {
      serialLoggerModel.setTimeoutMode(TIMEOUT_READ_BLOCKING);
    }
    checkMenuSemiBlock.setSelected(false);
    checkMenuNonBlock.setSelected(false);
  }

  @FXML
  private void checkTimeoutSemiBlock() {
    if (serialLoggerModel != null) {
      serialLoggerModel.setTimeoutMode(TIMEOUT_READ_SEMI_BLOCKING);
    }
    checkMenuBlock.setSelected(false);
    checkMenuNonBlock.setSelected(false);
  }

  @FXML
  private void checkTimeoutNonBlock() {
    if (serialLoggerModel != null) {
      serialLoggerModel.setTimeoutMode(TIMEOUT_NONBLOCKING);
    }
    checkMenuBlock.setSelected(false);
    checkMenuSemiBlock.setSelected(false);
  }

  @FXML
  private void toggleDecryption() {
    if (!serialLoggerModel.getDecrypt()) {
      toggleDecryption.setText("ON");
      prefs.putBoolean(comPortName + DECRYPT, true);
    } else {
      toggleDecryption.setText("OFF");
      prefs.putBoolean(comPortName + DECRYPT, false);
    }
  }

  @FXML
  private void menuSaveFile() {
    FileChooser fileChooser = new FileChooser();
    FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter(
        "TXT files (*.txt)", "*.txt");

    fileChooser.getExtensionFilters().add(extensionFilter);
    File file = fileChooser.showSaveDialog(labelBaud.getScene().getWindow());
    if (file != null && textAreaHex.isFocused()) {
      saveFile(textAreaHex.getText(), file);
    } else if (file != null) {
      saveFile(textAreaAscii.getText(), file);
    }
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
    textAreaAscii.clear();
  }

  @FXML
  private void menuTimeoutDefault() {
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle("Default Settings");
    alert.setHeaderText("Apply Default Settings ?");
    alert.setContentText("""
        Timeout Mode: TIMEOUT_READ_BLOCKING
        Baud Rate: 38400
        Interval (ms): 50
        """);
    ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons()
        .add(new Image("se/elnama/gui/seriallogger/images/Eicon.jpg"));

    ButtonType buttonOk = new ButtonType("Ok");
    ButtonType buttonCancel = new ButtonType("Cancel");
    alert.getButtonTypes().setAll(buttonOk, buttonCancel);

    Window window = alert.getDialogPane().getScene().getWindow();
    window.setOnCloseRequest(e -> alert.close());
    Optional<ButtonType> result = alert.showAndWait();
    if (result.orElse(buttonCancel) == buttonOk) {
      setTimeoutDefaults();
    }
  }

  private void setTimeoutDefaults() {
    setBaudRateDefault();
    setIntervalTimeDefault();
    setTimeoutModeDefault();
  }

  private void setBaudRateDefault() {
    if (serialLoggerModel != null) {
      serialLoggerModel.setBaudRate(38400);
    }
    labelBaud.setText(LABEL_BAUD + DEFAULT_BAUD_RATE);
  }

  private void setIntervalTimeDefault() {
    if (serialLoggerModel != null) {
      serialLoggerModel.setIntervalTime(TIMEOUT_READ_BLOCKING);
    }
    labelInterval.setText(LABEL_INTERVAL + 50);
  }

  private void setTimeoutModeDefault() {
    checkTimeoutBlock();
    checkMenuBlock.setSelected(true);
  }

  @FXML
  private void menuAbout() {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle("Elnama Logger");
    alert.setHeaderText("About");
    alert.setContentText("""
        Owner: Elnama AB
        Program: GRIC SERIAL Logger
        Version: 1.3.1.0
        """);
    ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons()
        .add(new Image("se/elnama/gui/seriallogger/images/Eicon.jpg"));

    ButtonType buttonOk = new ButtonType("Ok");
    alert.getButtonTypes().set(0, buttonOk);
    alert.showAndWait();
  }
}