package de.heiden.jem.models.c64.gui.javafx.emulator;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;

import java.util.function.LongSupplier;

import static javafx.animation.Animation.INDEFINITE;

/**
 * Status bar showing the emulated joystick on the left and the time needed for 1 simulated second on the right.
 * Uses the window background color of the platform, which is the color of the title bar on macOS.
 */
public class StatusBar extends BorderPane {
    /**
     * Emulated joystick.
     */
    private final Label joystickInfo = createLabel("");

    /**
     * Time needed for 1 simulated second.
     */
    private final Label timeInfo = createLabel("- ms");

    /**
     * Constructor.
     */
    public StatusBar() {
        setLeft(joystickInfo);
        setRight(timeInfo);
        setPadding(new Insets(2, 6, 2, 6));

        var preferences = Platform.getPreferences();
        backgroundProperty().bind(Bindings.createObjectBinding(
                () -> Background.fill(preferences.getBackgroundColor()),
                preferences.backgroundColorProperty()));
    }

    /**
     * Label for the status bar.
     */
    private static Label createLabel(String text) {
        var label = new Label(text);
        label.textFillProperty().bind(Platform.getPreferences().foregroundColorProperty());
        return label;
    }

    /**
     * Show the joystick keys mode.
     */
    public void setJoystickMode(JoystickMode mode) {
        joystickInfo.setText("Joystick: " + switch (mode) {
            case OFF -> "none";
            case JOYSTICK_2 -> "2";
            case JOYSTICK_1 -> "1";
        });
    }

    /**
     * Show the time (ms) needed for 1 simulated second.
     *
     * @param elapsedMs
     *         Supplier for the elapsed time in milliseconds.
     */
    public void displayExecutionTime(LongSupplier elapsedMs) {
        var interval = Duration.seconds(1);
        var timeUpdate = new Timeline(new KeyFrame(interval, _ ->
                timeInfo.setText(elapsedMs.getAsLong() + " ms")));
        timeUpdate.setCycleCount(INDEFINITE);
        timeUpdate.play();
    }
}
