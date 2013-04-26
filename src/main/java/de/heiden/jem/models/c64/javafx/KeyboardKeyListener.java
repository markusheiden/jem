package de.heiden.jem.models.c64.javafx;

import de.heiden.jem.models.c64.components.keyboard.Key;
import de.heiden.jem.models.c64.components.keyboard.Keyboard;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.input.KeyEvent;

/**
 * Key listener for C64 Keyboard.
 */
public class KeyboardKeyListener {
  /**
   * Constructor.
   *
   * @param node Node to attach listeners to
   * @param keyboard Keyboard
   * @param keyMapping Mapping from Java keys to C64 keys
   */
  public KeyboardKeyListener(Node node, Keyboard keyboard, KeyMapping keyMapping) {
    node.setOnKeyPressed(new EventHandler<KeyEvent>() {
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

    node.setOnKeyReleased(new EventHandler<KeyEvent>() {
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
