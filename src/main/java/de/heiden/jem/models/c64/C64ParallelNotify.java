package de.heiden.jem.models.c64;

import de.heiden.jem.components.clock.threads.ParallelNotifyClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.heiden.jem.models.c64.gui.javafx.emulator.C64Application.start;

/**
 * C64 startup with {@link ParallelNotifyClock}.
 */
public class C64ParallelNotify {
  /**
   * Logger.
   */
  private static final Logger logger = LoggerFactory.getLogger(C64ParallelNotify.class);

  /**
   * Start the application.
   */
  public static void main(String[] args) {
    try {
      start("--clock=" + ParallelNotifyClock.class.getName());
    } catch (Exception e) {
      logger.error("Unable to startup", e);
    }
  }
}
