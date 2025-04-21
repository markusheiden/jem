package de.heiden.jem.models.c64;

import de.heiden.jem.components.clock.threads.SequentialSpinClock;
import de.heiden.jem.models.c64.gui.javafx.emulator.C64Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static javafx.application.Application.launch;

/**
 * C64 startup with {@link SequentialSpinClock}.
 */
public class C64SequentialSpin {
  /**
   * Logger.
   */
  private static final Logger logger = LoggerFactory.getLogger(C64SequentialSpin.class);

  /**
   * Start the application.
   */
  public static void main(String[] args) {
    try {
      launch(C64Application.class, "--clock=" + SequentialSpinClock.class.getName());
    } catch (Exception e) {
      logger.error("Unable to startup", e);
    }
  }
}
