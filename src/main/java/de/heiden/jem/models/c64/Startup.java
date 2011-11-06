package de.heiden.jem.models.c64;

import org.apache.log4j.Logger;
import org.serialthreads.agent.TransformingClassLoader;
import org.serialthreads.transformer.Strategies;

/**
 * C64 Startup with byte code transformation by a class loader.
 */
public class Startup
{
  /**
   * Logger.
   */
  private final Logger _logger = Logger.getLogger(getClass());

  private final ClassLoader _classLoader;

  public Startup()
  {
    _classLoader = new TransformingClassLoader(Startup.class.getClassLoader(), Strategies.DEFAULT, "de.heiden.jem");
  }

  public void start()
  {
    try
    {
      _logger.debug("Loading c64");
      Class<?> clazz = loadClass("de.heiden.jem.models.c64.C64");
      Object c64 = clazz.getConstructor().newInstance();
      _logger.debug("Starting c64");
      c64.getClass().getDeclaredMethod("start").invoke(c64);
    }
    catch (Exception e)
    {
      _logger.error("Unable to startup", e);
    }
  }

  public static void main(String[] args)
  {
    new Startup().start();
  }

  protected Class<?> loadClass(String className) throws Exception
  {
    return _classLoader.loadClass(className);
  }
}
