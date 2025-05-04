package de.heiden.jem.models.c64;

import de.heiden.jem.components.clock.serialthreads.SerialClock;
import de.heiden.jem.models.c64.gui.javafx.emulator.C64Application;
import org.serialthreads.agent.TransformingClassLoader;
import org.serialthreads.transformer.Strategies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Thread.currentThread;

/**
 * C64 startup with {@link SerialClock} applying byte code transformation via a class loader.
 */
public class C64Serial {
    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Class loader.
     */
    private final ClassLoader classLoader;

    /**
     * Constructor.
     */
    public C64Serial() {
        classLoader = new TransformingClassLoader(C64Serial.class.getClassLoader(), Strategies.DEFAULT, "de.heiden.jem");
    }

    /**
     * Start the application.
     */
    public void start() {
        currentThread().setContextClassLoader(classLoader);

        try {
            logger.debug("Loading c64");
            var application = loadClass(C64ApplicationSerial.class.getName());
            logger.debug("Starting c64");
            application.getMethod("start").invoke(null);
        } catch (Exception e) {
            logger.error("Unable to startup", e);
        }
    }

    /**
     * Load a class with the transforming class loader.
     *
     * @param className
     *         Name of class
     */
    protected Class<?> loadClass(String className) throws Exception {
        return classLoader.loadClass(className);
    }

    /**
     * Start the application.
     */
    public static void main(String[] args) {
        new C64Serial().start();
    }

    public static class C64ApplicationSerial extends C64Application {
        /**
         * Start the application.
         */
        public static void start() {
            launch("--clock=" + SerialClock.class.getName());
        }
    }
}
