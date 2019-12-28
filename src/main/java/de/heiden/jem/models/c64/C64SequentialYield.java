package de.heiden.jem.models.c64;

import de.heiden.jem.components.clock.threads.SequentialYieldClock;
import de.heiden.jem.models.c64.gui.javafx.emulator.C64Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * C64 startup with {@link SequentialYieldClock}.
 */
public class C64SequentialYield {
  /**
   * Logger.
   */
  private static final Logger logger = LoggerFactory.getLogger(C64SequentialYield.class);

  /**
   * Start application.
   */
  public static void main(String[] args) {
    try {
      C64Application.start("--clock=" + SequentialYieldClock.class.getName());
    } catch (Exception e) {
      logger.error("Unable to startup", e);
    }
  }
}
