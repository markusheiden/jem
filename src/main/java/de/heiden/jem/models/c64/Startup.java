package de.heiden.jem.models.c64;

import org.serialthreads.agent.TransformingClassLoader;
import org.serialthreads.transformer.Strategies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * C64 Startup with byte code transformation by a class loader.
 */
public class Startup {
  /**
   * Logger.
   */
  private final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * Class loader.
   */
  private final ClassLoader _classLoader;

  /**
   * Constructor.
   */
  public Startup() {
    _classLoader = new TransformingClassLoader(Startup.class.getClassLoader(), Strategies.DEFAULT, "de.heiden.jem");
  }

  /**
   * Start application.
   */
  public void start() {
    Thread.currentThread().setContextClassLoader(_classLoader);

    try {
      logger.debug("Loading c64");
      Class<?> clazz = loadClass("de.heiden.jem.models.c64.gui.javafx.emulator.C64Application");
      Object c64 = clazz.getConstructor().newInstance();
      logger.debug("Starting c64");
      c64.getClass().getDeclaredMethod("start").invoke(c64);
    } catch (Exception e) {
      logger.error("Unable to startup", e);
    }
  }

  /**
   * Load a class with transforming class loader.
   *
   * @param className Name of class
   */
  protected Class<?> loadClass(String className) throws Exception {
    return _classLoader.loadClass(className);
  }

  /**
   * Start application.
   */
  public static void main(String[] args) {
    new Startup().start();
  }
}
