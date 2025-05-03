package de.heiden.jem.models.c64.gui.javafx.emulator;

import de.heiden.jem.models.c64.components.keyboard.Key;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Mapping virtual keys <-> C64 keyboard matrix.
 */
public abstract class KeyMapping {
  /**
   * Mapping characters to C64 keys.
   */
  private final Map<String, Key[]> chars;

  /**
   * Mapping special keys to C64 keys.
   */
  private final Map<KeyCode, Key[]> keys;

  /**
   * Constructor.
   */
  public KeyMapping() {
    chars = new HashMap<>();
    keys = new EnumMap<>(KeyCode.class);

    generateKeyMapping();
  }

  /**
   * Get key for the virtual key.
   *
   * @param e Key(Event).
   * @return Key or null if the key is not mapped.
   */
  public Key[] getKeys(KeyEvent e) {
    var result = chars.get(e.getText());
    if (result == null) {
      result = keys.get(e.getCode());
    }

    return result;
  }

  /**
   * Get string representation for the key.
   *
   * @param e Key(Event).
   * @return description of the key.
   */
  public static String toString(KeyEvent e) {
      return e.getText();
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
   * @param c Character.
   * @param keys C64 keys.
   */
  protected final void addChar(char c, Key... keys) {
    chars.put(String.valueOf(c), keys);
  }

  /**
   * Map a special key to C64 keys.
   *
   * @param c Key code of the special key.
   * @param keys C64 keys.
   */
  protected final void addCode(KeyCode c, Key... keys) {
    this.keys.put(c, keys);
  }
}
