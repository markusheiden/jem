package de.heiden.jem.models.c64;

import de.heiden.jem.models.c64.gui.javafx.C64Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * C64 Startup with byte code transformation by an agent.
 */
public class StartupAgent {
  /**
   * Logger.
   */
  private final Logger logger = LoggerFactory.getLogger(getClass());

  public void start() {
    try {
      logger.debug("Starting c64");
      new C64Application().start();
    } catch (Exception e) {
      logger.error("Unable to startup", e);
    }
  }

  public static void main(String[] args) {
    new StartupAgent().start();
  }
}
