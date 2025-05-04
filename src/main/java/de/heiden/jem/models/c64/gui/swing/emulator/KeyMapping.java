package de.heiden.jem.models.c64.gui.swing.emulator;

import de.heiden.jem.models.c64.components.keyboard.Key;

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * Mapping virtual keys <-> C64 keyboard matrix.
 */
public abstract class KeyMapping {
    /**
     * Mapping characters to C64 keys.
     */
    private final Map<Character, Key[]> chars;

    /**
     * Mapping special keys to C64 keys.
     */
    private final Map<Integer, Key[]> keys;

    /**
     * Constructor.
     */
    public KeyMapping() {
        chars = new HashMap<>();
        keys = new HashMap<>();

        generateKeyMapping();
    }

    /**
     * Get the key for the virtual key.
     *
     * @param e
     *         Key(Event)
     * @return Key or null if the key is not mapped
     */
    public Key[] getKeys(KeyEvent e) {
        var result = chars.get(e.getKeyChar());
        if (result == null) {
            result = keys.get(keyID(e.getKeyLocation(), e.getKeyCode()));
        }

        return result;
    }

    /**
     * Get string representation for the virtual key.
     *
     * @param e
     *         Key(Event)
     * @return description of the virtual key
     */
    public static String toString(KeyEvent e) {
        var result = KeyEvent.getKeyText(e.getKeyCode());
        return switch (e.getKeyLocation()) {
            case KeyEvent.KEY_LOCATION_STANDARD -> result + " (standard)";
            case KeyEvent.KEY_LOCATION_LEFT -> result + " (left)";
            case KeyEvent.KEY_LOCATION_RIGHT -> result + " (right)";
            case KeyEvent.KEY_LOCATION_NUMPAD -> result + " (numpad)";
            case KeyEvent.KEY_LOCATION_UNKNOWN -> result + " (unknown)";
            default -> result + " (???)";
        };
    }

    //
    // protected interface
    //

    /**
     * Key location / key code to int.
     */
    protected final int keyID(int location, int key) {
        return location << 16 | key;
    }

    /**
     * Generate default key mapping.
     */
    protected abstract void generateKeyMapping();

    /**
     * Map a character to C64 keys.
     *
     * @param c
     *         Character
     * @param keys
     *         C64 keys
     */
    protected final void addChar(char c, Key... keys) {
        chars.put(c, keys);
    }

    /**
     * Map a special key to C64 keys.
     *
     * @param location
     *         Location of the special key
     * @param key
     *         Key code of the special key
     * @param keys
     *         C64 keys
     */
    protected final void addCode(int location, int key, Key... keys) {
        this.keys.put(keyID(location, key), keys);
    }
}
