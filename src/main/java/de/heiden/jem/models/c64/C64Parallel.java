package de.heiden.jem.models.c64;

import de.heiden.jem.components.clock.threads.ParallelClock;
import de.heiden.jem.models.c64.gui.javafx.emulator.C64Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * C64 Startup with continuations.
 */
public class C64Parallel {
  /**
   * Logger.
   */
  private static final Logger logger = LoggerFactory.getLogger(C64Parallel.class);

  /**
   * Start application.
   */
  public static void main(String[] args) {
    try {
      C64Application.start("--clock=" + ParallelClock.class.getName());
    } catch (Exception e) {
      logger.error("Unable to startup", e);
    }
  }
}
