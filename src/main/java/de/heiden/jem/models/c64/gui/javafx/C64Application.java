package de.heiden.jem.models.c64.gui.javafx;

import de.heiden.jem.models.c64.C64;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application for C64 emulation.
 */
public class C64Application extends Application {
  /**
   * Logger.
   */
  private final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * C64.
   */
  private C64 c64;

  /**
   * Thread mit emulated C64.
   */
  private Thread thread;

  /**
   * Start application.
   */
  public void start() {
    launch();
  }

  @Override
  public void start(Stage stage) throws Exception {
    c64 = new C64();

    VICScreen screen = new VICScreen(c64.getVIC()._displayUnit);

    Scene scene = new Scene(screen, screen.getWidth(), screen.getHeight());
    KeyListener.attachTo(scene, c64.getKeyboard(), new PCMapping());

    stage.setScene(scene);
    stage.show();

    thread = new Thread(() -> {
      try {
        c64.start();
        logger.info("C64 stopped.");
      } catch (Exception e) {
        logger.error("C64 failed.", e);
      }
    });

    thread.setName("C64");
    // JavaFX thread keeps vm running
    thread.setDaemon(true);
    thread.start();
  }

  @Override
  @SuppressWarnings("deprecation")
  public void stop() throws Exception {
    thread.interrupt();
    thread.join(100);
    thread.stop();
  }
}
