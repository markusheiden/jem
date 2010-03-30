package de.heiden.jem.components.ports;

/**
 * Input port listener.
 */
public interface InputPortListener
{
  /**
   * Input port changed.
   *
   * @param value new port value
   * @param mask driven bits
   */
  public void inputPortChanged (int value, int mask);
}
