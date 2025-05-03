package de.heiden.jem.models.c64;

import de.heiden.jem.components.clock.serialthreads.SerialClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.heiden.jem.models.c64.gui.javafx.emulator.C64Application.start;

/**
 * C64 startup with {@link SerialClock} applying byte code transformation via an agent.
 */
public class C64SerialAgent {
  /**
   * Logger.
   */
  private static final Logger logger = LoggerFactory.getLogger(C64SerialAgent.class);

  /**
   * Start the application.
   */
  public static void main(String[] args) {
    try {
      start("--clock=" + SerialClock.class.getName());
    } catch (Exception e) {
      logger.error("Unable to startup", e);
    }
  }
}
