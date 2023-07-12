module serial.logger {
  requires serial;
  requires lib.log;
  requires lib.util;
  requires javafx.graphics;
  requires javafx.controls;
  requires javafx.fxml;
  requires java.prefs;
  requires java.logging;

  opens se.elnama.gui.seriallogger to javafx.fxml;
  opens se.elnama.gui.seriallogger.images;
  opens se.elnama.gui.seriallogger.view;

  exports se.elnama.gui.seriallogger;
}