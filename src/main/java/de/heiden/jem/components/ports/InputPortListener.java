package de.heiden.jem.components.ports;

/**
 * Input port listener.
 */
public abstract class InputPortListener {
  /**
   * Next input port listener.
   * Used for efficient list implementation in input ports.
   */
  InputPortListener next;

  /**
   * Input port changed.
   *
   * @param value new port value
   * @param mask driven bits
   */
  public abstract void inputPortChanged(int value, int mask);
}
