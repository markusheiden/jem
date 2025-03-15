package de.heiden.jem.models.c64;

import de.heiden.jem.components.clock.loom.SequentialFiberExecutorClock;
import de.heiden.jem.components.clock.loom.SequentialFiberLatchClock;
import de.heiden.jem.models.c64.gui.javafx.emulator.C64Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * C64 startup with {@link SequentialFiberExecutorClock}.
 */
public class C64SequentialFiberLatch {
  /**
   * Logger.
   */
  private static final Logger logger = LoggerFactory.getLogger(C64SequentialFiberLatch.class);

  /**
   * Start application.
   */
  public static void main(String[] args) {
    try {
      C64Application.start("--clock=" + SequentialFiberLatchClock.class.getName());
    } catch (Exception e) {
      logger.error("Unable to startup", e);
    }
  }
}
