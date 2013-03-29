package de.heiden.jem.components.ports;

/**
 * Output port.
 */
public interface OutputPort {
  /**
   * Add port listener.
   *
   * @param listener port listener
   * @require listener != null
   */
  public void addOutputPortListener(OutputPortListener listener);

  /**
   * Remove port listener.
   *
   * @param listener port listener
   * @require listener != null
   */
  public void removeOutputPortListener(OutputPortListener listener);

  /**
   * Port output data.
   */
  public int outputData();

  /**
   * Port output mask.
   * Set bit means port bit is output. Cleared bit means port bit is not driven.
   */
  public int outputMask();
}
