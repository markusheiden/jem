package de.heiden.jem.models.c64.gui.javafx.emulator;

import de.heiden.jem.models.c64.components.joystick.Direction;
import de.heiden.jem.models.c64.components.joystick.Joystick;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * Joystick emulation via keys: Cursor keys for the directions, shift for fire.
 * F12 switches between off, joystick 2 and joystick 1.
 */
public class JoystickKeys {
    private static final Logger logger = LoggerFactory.getLogger(JoystickKeys.class);

    /**
     * Key to switch the mode.
     */
    private static final KeyCode TOGGLE = KeyCode.F12;

    /**
     * Keys for the joystick directions and fire.
     */
    private static final Map<KeyCode, Direction> DIRECTIONS = new EnumMap<>(Map.of(
            KeyCode.UP, Direction.UP,
            KeyCode.DOWN, Direction.DOWN,
            KeyCode.LEFT, Direction.LEFT,
            KeyCode.RIGHT, Direction.RIGHT,
            KeyCode.SHIFT, Direction.FIRE));

    /**
     * Joystick in port 1.
     */
    private final Joystick joystick1;

    /**
     * Joystick in port 2.
     */
    private final Joystick joystick2;

    /**
     * Listener for mode changes.
     */
    private final Consumer<JoystickMode> modeListener;

    /**
     * Current mode.
     */
    private JoystickMode mode = JoystickMode.OFF;

    /**
     * Keys whose press has been handled as joystick input.
     * Their releases are handled as joystick input too, even if the mode changed meanwhile.
     */
    private final Set<KeyCode> pressed = EnumSet.noneOf(KeyCode.class);

    /**
     * Constructor.
     *
     * @param joystick1
     *         Joystick in port 1.
     * @param joystick2
     *         Joystick in port 2.
     * @param modeListener
     *         Listener for mode changes.
     */
    public JoystickKeys(Joystick joystick1, Joystick joystick2, Consumer<JoystickMode> modeListener) {
        this.joystick1 = joystick1;
        this.joystick2 = joystick2;
        this.modeListener = modeListener;
    }

    /**
     * Current mode.
     */
    public JoystickMode getMode() {
        return mode;
    }

    /**
     * Handle key press.
     *
     * @return Whether the key has been handled and must not be passed to the keyboard.
     */
    public boolean pressed(KeyEvent e) {
        var code = e.getCode();
        if (code == TOGGLE) {
            switchMode();
            return true;
        }

        var direction = DIRECTIONS.get(code);
        var joystick = joystick();
        if (direction == null || joystick == null) {
            return false;
        }

        pressed.add(code);
        joystick.press(direction);
        return true;
    }

    /**
     * Handle key release.
     *
     * @return Whether the key has been handled and must not be passed to the keyboard.
     */
    public boolean released(KeyEvent e) {
        var code = e.getCode();
        if (code == TOGGLE) {
            return true;
        }

        if (!pressed.remove(code)) {
            return false;
        }

        var joystick = joystick();
        if (joystick != null) {
            joystick.release(requireNonNull(DIRECTIONS.get(code)));
        }
        return true;
    }

    /**
     * Release all joystick keys, e.g. when the window lost the focus and thus won't receive their releases.
     */
    public void releaseAll() {
        joystick1.releaseAll();
        joystick2.releaseAll();
        pressed.clear();
    }

    /**
     * Switch to the next mode: Off -> joystick 2 -> joystick 1 -> off.
     */
    private void switchMode() {
        releaseAll();

        mode = JoystickMode.values()[(mode.ordinal() + 1) % JoystickMode.values().length];
        logger.info("Joystick keys: {}", mode);
        modeListener.accept(mode);
    }

    /**
     * Joystick of the current mode, if any.
     */
    private @Nullable Joystick joystick() {
        return switch (mode) {
            case OFF -> null;
            case JOYSTICK_2 -> joystick2;
            case JOYSTICK_1 -> joystick1;
        };
    }
}
