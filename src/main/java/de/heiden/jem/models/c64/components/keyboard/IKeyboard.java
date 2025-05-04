package de.heiden.jem.models.c64.components.keyboard;

/**
 * Interface for keyboard.
 */
public interface IKeyboard {
    /**
     * Execute key press on keyboard matrix.
     *
     * @param key
     *         key
     */
    void press(Key key);

    /**
     * Execute key release on keyboard matrix.
     *
     * @param key
     *         key
     */
    void release(Key key);
}
