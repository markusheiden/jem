package de.heiden.jem.models.c64.gui;

import de.heiden.jem.models.c64.components.keyboard.Key;
import de.heiden.jem.models.c64.components.keyboard.Keyboard;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * {@link java.awt.event.KeyListener} for C64 Keyboard
 */
public class KeyListener extends KeyAdapter {
  /**
   * Keyboard.
   */
  private final Keyboard keyboard;

  /**
   * Mapping chars/keys -> matrix entry.
   */
  private final KeyMapping keyMapping;

  /**
   * Constructor.
   *
   * @param keyboard Keyboard
   * @param keyMapping Mapping from Java keys to C64 keys
   */
  public KeyListener(Keyboard keyboard, KeyMapping keyMapping) {
    this.keyboard = keyboard;
    this.keyMapping = keyMapping;
  }

  @Override
  public void keyPressed(KeyEvent e) {
    Key[] keys = keyMapping.getKeys(e);
    if (keys != null) {
      for (Key key : keys) {
        keyboard.press(key);
      }
    }
  }

  @Override
  public void keyReleased(KeyEvent e) {
    Key[] keys = keyMapping.getKeys(e);
    if (keys != null) {
      for (Key key : keys) {
        keyboard.release(key);
      }
    }
  }
}
