package de.heiden.jem.components.ports;

/**
 * Output port listener.
 */
public abstract class OutputPortListener {
  /**
   * Next output port listener.
   * Used for efficient list implementation in output ports.
   */
  OutputPortListener next;

  /**
   * Output port changed.
   *
   * @param value new port value
   * @param mask driven bits
   */
  public abstract void outputPortChanged(int value, int mask);
}
