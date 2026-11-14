package de.heiden.jem.models.c64.gui.javafx.emulator;

import de.heiden.jem.models.c64.components.keyboard.IKeyboard;
import javafx.scene.Scene;

/**
 * Key listener for C64 keyboard and joysticks.
 */
public class KeyListener {
    /**
     * Attach key listeners to the scene.
     *
     * @param scene
     *         Scene
     * @param keyboard
     *         Keyboard
     * @param keyMapping
     *         Mapping from keys to C64 keys
     * @param joystickKeys
     *         Joystick emulation via keys, takes precedence over the keyboard
     */
    public static void attachTo(Scene scene, IKeyboard keyboard, KeyMapping keyMapping, JoystickKeys joystickKeys) {
        scene.setOnKeyPressed(e -> {
            if (joystickKeys.pressed(e)) {
                return;
            }

            var keys = keyMapping.getKeys(e);
            if (keys != null) {
                for (var key : keys) {
                    keyboard.press(key);
                }
            }
        });

        scene.setOnKeyReleased(e -> {
            if (joystickKeys.released(e)) {
                return;
            }

            var keys = keyMapping.getKeys(e);
            if (keys != null) {
                for (var key : keys) {
                    keyboard.release(key);
                }
            }
        });
    }
}
