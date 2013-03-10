package de.heiden.jem.models.c64;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * C64 Startup with byte code transformation by an agent.
 */
public class StartupAgent {
  /**
   * Logger.
   */
  private final Log logger = LogFactory.getLog(getClass());

  public void start() {
    try {
      logger.debug("Starting c64");
      new C64().start();
    } catch (Exception e) {
      logger.error("Unable to startup", e);
    }
  }

  public static void main(String[] args) {
    new StartupAgent().start();
  }
}
