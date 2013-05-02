package de.heiden.jem.models.c64.javafx;

import de.heiden.jem.models.c64.components.keyboard.Key;
import de.heiden.jem.models.c64.components.keyboard.Keyboard;
import javafx.scene.Scene;

/**
 * Key listener for C64 Keyboard.
 */
public class KeyListener {
  /**
   * Attach key listeners to scene.
   *
   * @param scene Scene
   * @param keyboard Keyboard
   * @param keyMapping Mapping from keys to C64 keys
   */
  public static void attachTo(Scene scene, Keyboard keyboard, KeyMapping keyMapping) {
    scene.setOnKeyPressed(e -> {
      Key[] keys = keyMapping.getKeys(e);
      if (keys != null) {
        for (Key key : keys) {
          keyboard.press(key);
        }
      }
    });

    scene.setOnKeyReleased(e -> {
      Key[] keys = keyMapping.getKeys(e);
      if (keys != null) {
        for (Key key : keys) {
          keyboard.release(key);
        }
      }
    });
  }
}