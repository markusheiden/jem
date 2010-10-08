package de.heiden.jem.models.c64;

import org.apache.log4j.Logger;

/**
 * C64 Startup with byte code transformation by an agent.
 */
public class StartupAgent
{
  public void start() throws Exception
  {
    _logger.debug("Starting c64");
    new C64().start();
  }

  public static void main(String[] args)
  {
    try
    {
      new StartupAgent().start();
    }
    catch (Exception e)
    {
      _logger.error("Unable to startup", e);
    }
  }

  //
  // private attributes
  //

  /**
   * Logger.
   */
  private static final Logger _logger = Logger.getLogger(StartupAgent.class);
}
