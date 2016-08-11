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
  void addOutputPortListener(PortListener listener);

  /**
   * Remove port listener.
   *
   * @param listener port listener
   * @require listener != null
   */
  void removeOutputPortListener(PortListener listener);

  /**
   * Port output data.
   */
  int outputData();

  /**
   * Port output mask.
   * Set bit means port bit is output. Cleared bit means port bit is not driven.
   */
  int outputMask();

  /**
   * Set port output data.
   *
   * @param data new output value.
   */
  void setOutputData(int data);

  /**
   * Set port output mask.
   * Set bit means port bit is output. Cleared bit means port bit is not driven.
   *
   * @param mask driven bits.
   */
  void setOutputMask(int mask);
}
