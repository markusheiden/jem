package de.heiden.jem.models.c64;

import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.components.clock.serialthreads.SerialClock;
import de.heiden.jem.models.c64.gui.javafx.emulator.C64Application;
import org.serialthreads.agent.TransformingClassLoader;
import org.serialthreads.transformer.Strategies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * C64 Startup with byte code transformation by a class loader.
 */
public class C64Serial {
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
  public C64Serial() {
    _classLoader = new TransformingClassLoader(C64Serial.class.getClassLoader(), Strategies.DEFAULT, "de.heiden.jem");
  }

  /**
   * Start application.
   */
  public void start() {
    Thread.currentThread().setContextClassLoader(_classLoader);

    try {
      logger.debug("Loading c64");
      Class<?> application = loadClass("de.heiden.jem.models.c64.StartupSerial$C64ApplicationSerial");
      logger.debug("Starting c64");
      application.getMethod("start").invoke(null);
    } catch (Exception e) {
      logger.error("Unable to startup", e);
    }
  }

  public static class C64ApplicationSerial extends C64Application {
    /**
     * Start application.
     */
    public static void start() {
      launch();
    }

    @Override
    protected Clock createClock() {
      return new SerialClock();
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
    new C64Serial().start();
  }
}
