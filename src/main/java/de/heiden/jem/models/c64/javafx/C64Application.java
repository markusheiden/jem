package de.heiden.jem.models.c64.javafx;

import de.heiden.jem.models.c64.C64;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Application for C64 emulation.
 */
public class C64Application extends Application {
  @Override
  public void start(Stage stage) throws Exception {
    C64 c64 = new C64();
//    VICScreen screen = new VICScreen(c64);
//    new KeyboardKeyListener(screen, keyboard, new PCMapping());

//    stage.setScene(new Scene(screen, screen.getWidth(), screen.getHeight()));
    stage.show();
  }

  /**
   * Start application.
   */
  public static void main(String[] args) {
    launch();
  }
}
