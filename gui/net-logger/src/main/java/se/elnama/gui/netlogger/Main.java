package se.elnama.gui.netlogger;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import se.elnama.gui.netlogger.view.NetLoggerLayoutController;
import se.elnama.lib.log.ElnamaErrorLog;

public class Main extends Application {

  private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
  private static NetLoggerLayoutController netLoggerLayoutControllerHandle;

  public static NetLoggerLayoutController getLoggerLayoutControllerHandle() {
    return netLoggerLayoutControllerHandle;
  }

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage primaryStage) throws Exception {
    ElnamaErrorLog.elnamaLoggerSetup();
    FXMLLoader loader = new FXMLLoader(getClass().getResource("view/NetLoggerLayout.fxml"));
    Parent root = loader.load();
    netLoggerLayoutControllerHandle = loader.getController();

    primaryStage.setTitle("GRIC NET Logger");
    try {
      Image appIcon = new Image(
          Objects.requireNonNull(getClass().getResourceAsStream("images/Eicon.jpg")));
      primaryStage.getIcons().add(appIcon);
    } catch (NullPointerException exc) {
      LOGGER.log(Level.INFO, exc.getMessage(), exc);
    }
    primaryStage.setScene(new Scene(root));
    primaryStage.show();
  }

  @Override
  public void stop() throws Exception {
    netLoggerLayoutControllerHandle.closeThreads();
    ElnamaErrorLog.deleteEmptyErrorLog();
    super.stop();
  }
}