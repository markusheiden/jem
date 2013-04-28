package de.heiden.jem.models.c64.javafx;

import de.heiden.jem.models.c64.components.keyboard.Key;
import de.heiden.jem.models.c64.components.keyboard.Keyboard;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;

/**
 * Key listener for C64 Keyboard.
 */
public class KeyListener {
  /**
   * Attach key listeners to scene.
   *
   * @param scene Scene
   * @param keyboard Keyboard
   * @param keyMapping Mapping from Java keys to C64 keys
   */
  public static void attachTo(Scene scene, Keyboard keyboard, KeyMapping keyMapping) {
    scene.setOnKeyPressed(new EventHandler<KeyEvent>() {
      @Override
      public void handle(KeyEvent e) {
        Key[] keys = keyMapping.getKeys(e);
        if (keys != null) {
          for (Key key : keys) {
            keyboard.press(key);
          }
        }
      }
    });

    scene.setOnKeyReleased(new EventHandler<KeyEvent>() {
      @Override
      public void handle(KeyEvent e) {
        Key[] keys = keyMapping.getKeys(e);
        if (keys != null) {
          for (Key key : keys) {
            keyboard.release(key);
          }
        }
      }
    });
  }
}
