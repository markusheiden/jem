package de.heiden.jem.components.ports;

/**
 * Output port listener.
 */
public interface OutputPortListener
{
  /**
   * Output port changed.
   *
   * @param value new port value
   * @param mask driven bits
   */
  public void outputPortChanged (int value, int mask);
}
