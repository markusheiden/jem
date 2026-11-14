package de.heiden.jem.models.c64.gui.javafx.emulator;

import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.models.c64.components.C64;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
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

        var statusBar = createStatusBar();
        c64.getRealTimeSlowDown().setListener(elapsedMs ->
                runLater(() -> statusBar.setText(elapsedMs + " ms")));

        var root = new BorderPane(screen);
        root.setBottom(statusBar);

        var scene = new Scene(root);
        var joystickKeys = new JoystickKeys(c64.getJoystick1(), c64.getJoystick2(), mode -> setTitle(stage, mode));
        KeyListener.attachTo(scene, c64.getKeyboard(), c64.getKeyboardBuffer(), new MacMapping(), joystickKeys);
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
     * Status bar showing the time needed for 1 simulated second.
     * Uses the window background color of the platform, which is the color of the title bar on macOS.
     */
    private static Label createStatusBar() {
        var statusBar = new Label("- ms");
        statusBar.setPadding(new Insets(2, 6, 2, 6));
        statusBar.setMaxWidth(Double.MAX_VALUE);
        statusBar.setAlignment(Pos.CENTER_RIGHT);

        var preferences = Platform.getPreferences();
        statusBar.backgroundProperty().bind(Bindings.createObjectBinding(
                () -> Background.fill(preferences.getBackgroundColor()),
                preferences.backgroundColorProperty()));
        statusBar.textFillProperty().bind(preferences.foregroundColorProperty());

        return statusBar;
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
