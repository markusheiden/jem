package de.heiden.jem.models.c64.gui.javafx.emulator;

import de.heiden.jem.models.c64.components.keyboard.IKeyboard;
import de.heiden.jem.models.c64.components.keyboard.KeyboardBuffer;
import javafx.scene.Scene;
import javafx.scene.input.Clipboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * Key listener for C64 keyboard and joysticks, with paste support.
 */
public class KeyListener {
    /**
     * Attach key listeners to the scene.
     *
     * @param scene
     *         Scene.
     * @param keyboard
     *         Keyboard.
     * @param keyboardBuffer
     *         Keyboard buffer.
     * @param keyMapping
     *         Mapping from keys to C64 keys.
     * @param joystickKeys
     *         Joystick emulation via keys, takes precedence over the keyboard.
     */
    public static void attachTo(
            Scene scene,
            IKeyboard keyboard, KeyboardBuffer keyboardBuffer,
            KeyMapping keyMapping, JoystickKeys joystickKeys) {
        scene.setOnKeyPressed(e -> {
            if (isPaste(e)) {
                var clipboard = Clipboard.getSystemClipboard();
                if (clipboard.hasString()) {
                    keyboardBuffer.type(clipboard.getString());
                }
                return;
            }

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
            if (isPaste(e)) {
                return;
            }

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

    /**
     * Is the key event the paste shortcut, e.g. command-V?
     */
    private static boolean isPaste(KeyEvent e) {
        return e.isShortcutDown() && e.getCode() == KeyCode.V;
    }
}
