package de.heiden.jem.models.c64.components.keyboard;

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * Mapping virtual keys <-> C64 keyboard matrix.
 */
public abstract class KeyMapping
{
  /**
   * Constructor.
   */
  public KeyMapping()
  {
    _chars = new HashMap<Character, Key[]>();
    _keys = new HashMap<Integer, Key[]>();

    generateKeyMapping();
  }

  /**
   * Get key for virtual key.
   *
   * @param e Key(Event)
   * @return Key or null if key is not mapped
   */
  public Key[] getKeys(KeyEvent e)
  {
    Key[] result = _chars.get(e.getKeyChar());
    if (result == null)
    {
      result = _keys.get(keyID(e.getKeyLocation(), e.getKeyCode()));
    }

    return result;
  }

  /**
   * Get string representation for virtual key.
   *
   * @param e Key(Event)
   * @return description of virtual key
   */
  public static String toString(KeyEvent e)
  {
    String result = KeyEvent.getKeyText(e.getKeyCode());
    switch (e.getKeyLocation())
    {
      case KeyEvent.KEY_LOCATION_STANDARD:
      {
        return result + " (standard)";
      }
      case KeyEvent.KEY_LOCATION_LEFT:
      {
        return result + " (left)";
      }
      case KeyEvent.KEY_LOCATION_RIGHT:
      {
        return result + " (right)";
      }
      case KeyEvent.KEY_LOCATION_NUMPAD:
      {
        return result + " (numpad)";
      }
      case KeyEvent.KEY_LOCATION_UNKNOWN:
      {
        return result + " (unknown)";
      }
      default:
      {
        return result + " (???)";
      }
    }
  }

  //
  // protected interface
  //

  /**
   * Key location / key code to int.
   */
  protected final int keyID(int location, int key)
  {
    return location << 16 | key;
  }

  /**
   * Generate default key mapping.
   */
  protected abstract void generateKeyMapping();

  //
  // private attributes
  //

  protected Map<Character, Key[]> _chars;
  protected Map<Integer, Key[]> _keys;
}
