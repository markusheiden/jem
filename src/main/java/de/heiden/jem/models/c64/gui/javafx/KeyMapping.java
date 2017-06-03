package de.heiden.jem.models.c64.gui.javafx;

import de.heiden.jem.models.c64.components.keyboard.Key;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Mapping virtual keys <-> C64 keyboard matrix.
 */
public abstract class KeyMapping {
  /**
   * Mapping characters to C64 keys.
   */
  private final Map<String, Key[]> _chars;

  /**
   * Mapping special keys to C64 keys.
   */
  private final Map<KeyCode, Key[]> _keys;

  /**
   * Constructor.
   */
  public KeyMapping() {
    _chars = new HashMap<>();
    _keys = new HashMap<>();

    generateKeyMapping();
  }

  /**
   * Get key for virtual key.
   *
   * @param e Key(Event)
   * @return Key or null if key is not mapped
   */
  public Key[] getKeys(KeyEvent e) {
    Key[] result = _chars.get(e.getText());
    if (result == null) {
      result = _keys.get(e.getCode());
    }

    return result;
  }

  /**
   * Get string representation for key.
   *
   * @param e Key(Event)
   * @return description of key
   */
  public static String toString(KeyEvent e) {
    String result = e.getText();
    return result;
  }

  //
  // protected interface
  //

  /**
   * Generate default key mapping.
   */
  protected abstract void generateKeyMapping();

  /**
   * Map a character to C64 keys.
   *
   * @param c Character
   * @param keys C64 keys
   */
  protected final void addChar(char c, Key... keys) {
    _chars.put(String.valueOf(c), keys);
  }

  /**
   * Map a special key to C64 keys.
   *
   * @param c Key code of special key
   * @param keys C64 keys
   */
  protected final void addCode(KeyCode c, Key... keys) {
    _keys.put(c, keys);
  }
}
