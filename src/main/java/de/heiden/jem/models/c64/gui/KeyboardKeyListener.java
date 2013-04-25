package de.heiden.jem.models.c64.gui;

import de.heiden.jem.models.c64.components.keyboard.Keyboard;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * {@link KeyListener} for C64 Keyboard
 */
public class KeyboardKeyListener extends KeyAdapter {
  /**
   * Keyboard.
   */
  private final Keyboard keyboard;

  /**
   * Constructor.
   *
   * @param keyboard Keyboard
   */
  public KeyboardKeyListener(Keyboard keyboard) {
    this.keyboard = keyboard;
  }

  @Override
  public void keyPressed(KeyEvent e) {
    keyboard.keyPressed(e);
  }

  @Override
  public void keyReleased(KeyEvent e) {
    keyboard.keyReleased(e);
  }
}
