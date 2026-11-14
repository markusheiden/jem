package de.heiden.jem.models.c64.gui.javafx.emulator;

import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.models.c64.components.C64;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static javafx.application.Platform.runLater;

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

        var screen = new VICScreen(c64.getVIC().displayUnit);

        var statusBar = new StatusBar();
        c64.getRealTimeSlowDown().setListener(elapsedMs ->
                runLater(() -> statusBar.setTime(elapsedMs)));

        var root = new BorderPane(screen);
        root.setBottom(statusBar);

        var scene = new Scene(root);
        var joystickKeys = new JoystickKeys(c64.getJoystick1(), c64.getJoystick2(), statusBar::setJoystickMode);
        KeyListener.attachTo(scene, c64.getKeyboard(), c64.getKeyboardBuffer(), new MacMapping(), joystickKeys);
        statusBar.setJoystickMode(joystickKeys.getMode());

        stage.setTitle("C64");
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
