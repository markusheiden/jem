package de.heiden.jem.models.c64.gui.javafx.emulator;

import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.models.c64.components.C64;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application for C64 emulation.
 */
public class C64Application extends Application {
    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(C64Application.class);

    /**
     * C64.
     */
    private C64 c64;

    /**
     * Thread running the emulated C64.
     */
    private Thread thread;

    /**
     * Start the application.
     */
    public static void start(String... args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        c64 = new C64(createClock());

        var screen = new VICScreen(c64.getVIC()._displayUnit);

        var scene = new Scene(screen, screen.getWidth(), screen.getHeight());
        KeyListener.attachTo(scene, c64.getKeyboard(), new PCMapping());

        stage.setScene(scene);
        stage.show();

        thread = new Thread(this::runC64);
        thread.setName("C64");
        // JavaFX thread keeps vm running.
        thread.setDaemon(true);
        thread.start();
    }

    private void runC64() {
        try {
            c64.start();
            logger.info("C64 stopped.");
        } catch (Exception e) {
            logger.error("C64 failed.", e);
        }
    }

    /**
     * Create the clock via reflection from the command line parameter "clock".
     */
    private Clock createClock() throws Exception {
        var clockClass = getParameters().getNamed().get("clock");
        if (clockClass == null) {
            throw new IllegalArgumentException("Clock ");
        }
        return (Clock) Class.forName(clockClass).getConstructor().newInstance();
    }

    @Override
    public void stop() throws Exception {
        logger.debug("Stopping...");
        c64.stop();
        thread.interrupt();
        thread.join(100);
        logger.debug("Stopped.");
    }
}
