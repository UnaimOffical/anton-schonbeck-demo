module net.logger {
  requires net;
  requires lib.log;
  requires lib.util;
  requires javafx.graphics;
  requires javafx.controls;
  requires javafx.fxml;
  requires java.prefs;
  requires java.logging;

  opens se.elnama.gui.netlogger to javafx.fxml;
  opens se.elnama.gui.netlogger.images;
  opens se.elnama.gui.netlogger.view;

  exports se.elnama.gui.netlogger;
}