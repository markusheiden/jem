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

        var screen = new VICScreen(c64.getVIC().displayUnit);

        var scene = new Scene(screen, screen.getWidth(), screen.getHeight());
        var joystickKeys = new JoystickKeys(c64.getJoystick1(), c64.getJoystick2(), mode -> setTitle(stage, mode));
        KeyListener.attachTo(scene, c64.getKeyboard(), new PCMapping(), joystickKeys);
        setTitle(stage, joystickKeys.getMode());

        stage.setScene(scene);
        stage.show();

        thread = new Thread(this::runC64);
        thread.setName("C64");
        // JavaFX thread keeps vm running.
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Show the joystick keys mode in the window title.
     */
    private static void setTitle(Stage stage, JoystickMode mode) {
        stage.setTitle("C64 - " + switch (mode) {
            case OFF -> "no joystick";
            case JOYSTICK_2 -> "joystick 2";
            case JOYSTICK_1 -> "joystick 1";
        });
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
