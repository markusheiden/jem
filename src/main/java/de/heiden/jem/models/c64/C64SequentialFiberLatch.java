package de.heiden.jem.models.c64;

import de.heiden.jem.components.clock.loom.SequentialFiberExecutorClock;
import de.heiden.jem.components.clock.loom.SequentialFiberLatchClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.heiden.jem.models.c64.gui.javafx.emulator.C64Application.start;

/**
 * C64 startup with {@link SequentialFiberExecutorClock}.
 */
public class C64SequentialFiberLatch {
  /**
   * Logger.
   */
  private static final Logger logger = LoggerFactory.getLogger(C64SequentialFiberLatch.class);

  /**
   * Start the application.
   */
  public static void main(String[] args) {
    try {
      start("--clock=" + SequentialFiberLatchClock.class.getName());
    } catch (Exception e) {
      logger.error("Unable to startup", e);
    }
  }
}
