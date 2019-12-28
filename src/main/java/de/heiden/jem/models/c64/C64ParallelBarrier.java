package de.heiden.jem.models.c64;

import de.heiden.jem.components.clock.threads.ParallelBarrierClock;
import de.heiden.jem.models.c64.gui.javafx.emulator.C64Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * C64 startup with {@link ParallelBarrierClock}.
 */
public class C64ParallelBarrier {
  /**
   * Logger.
   */
  private static final Logger logger = LoggerFactory.getLogger(C64ParallelBarrier.class);

  /**
   * Start application.
   */
  public static void main(String[] args) {
    try {
      C64Application.start("--clock=" + ParallelBarrierClock.class.getName());
    } catch (Exception e) {
      logger.error("Unable to startup", e);
    }
  }
}
