package de.heiden.jem.components.ports;

/**
 * Port listener.
 */
public interface PortListener {
  /**
   * Port changed.
   *
   * @param value new port value
   * @param mask driven bits
   */
  void portChanged(int value, int mask);
}
