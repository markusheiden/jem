package de.heiden.jem.models.c64.gui.swing.emulator;

import de.heiden.jem.models.c64.components.keyboard.IKeyboard;
import de.heiden.jem.models.c64.components.keyboard.Key;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * {@link java.awt.event.KeyListener} for C64 Keyboard
 */
public class KeyListener extends KeyAdapter {
  /**
   * Keyboard.
   */
  private final IKeyboard keyboard;

  /**
   * Mapping chars/keys -> matrix entry.
   */
  private final KeyMapping keyMapping;

  /**
   * Constructor.
   *
   * @param keyboard Keyboard
   * @param keyMapping Mapping from keys to C64 keys
   */
  public KeyListener(IKeyboard keyboard, KeyMapping keyMapping) {
    this.keyboard = keyboard;
    this.keyMapping = keyMapping;
  }

  @Override
  public void keyPressed(KeyEvent e) {
    var keys = keyMapping.getKeys(e);
    if (keys != null) {
      for (Key key : keys) {
        keyboard.press(key);
      }
    }
  }

  @Override
  public void keyReleased(KeyEvent e) {
    var keys = keyMapping.getKeys(e);
    if (keys != null) {
      for (Key key : keys) {
        keyboard.release(key);
      }
    }
  }
}
