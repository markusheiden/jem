package de.heiden.jem.models.c64.javafx;

import de.heiden.jem.models.c64.C64;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Application for C64 emulation.
 */
public class C64Application extends Application {
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
    KeyboardKeyListener.attachTo(scene, c64.getKeyboard(), new PCMapping());

    stage.setScene(scene);
    stage.show();

    thread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          c64.start();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });

    thread.setName("C64");
    // JavaFX thread keeps vm running
    thread.setDaemon(true);
    thread.start();
  }

  @Override
  public void stop() throws Exception {
    thread.interrupt();
    thread.join(100);
    thread.stop();
  }
}
