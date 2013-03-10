package de.heiden.jem.models.c64;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.serialthreads.agent.TransformingClassLoader;
import org.serialthreads.transformer.Strategies;

/**
 * C64 Startup with byte code transformation by a class loader.
 */
public class Startup {
  /**
   * Logger.
   */
  private final Log logger = LogFactory.getLog(getClass());

  private final ClassLoader _classLoader;

  public Startup() {
    _classLoader = new TransformingClassLoader(Startup.class.getClassLoader(), Strategies.DEFAULT, "de.heiden.jem");
  }

  public void start() {
    try {
      logger.debug("Loading c64");
      Class<?> clazz = loadClass("de.heiden.jem.models.c64.C64");
      Object c64 = clazz.getConstructor().newInstance();
      logger.debug("Starting c64");
      c64.getClass().getDeclaredMethod("start").invoke(c64);
    } catch (Exception e) {
      logger.error("Unable to startup", e);
    }
  }

  public static void main(String[] args) {
    new Startup().start();
  }

  protected Class<?> loadClass(String className) throws Exception {
    return _classLoader.loadClass(className);
  }
}
