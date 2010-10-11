package de.heiden.jem.models.c64;

import org.apache.log4j.Logger;

/**
 * C64 Startup with byte code transformation by an agent.
 */
public class StartupAgent
{
  /**
   * Logger.
   */
  private final Logger _logger = Logger.getLogger(getClass());

  public void start()
  {
    try
    {
      _logger.debug("Starting c64");
      new C64().start();
    }
    catch (Exception e)
    {
      _logger.error("Unable to startup", e);
    }
  }

  public static void main(String[] args)
  {
    new StartupAgent().start();
  }
}
