package de.heiden.jem.components.ports;

/**
 * Input Port.
 */
public interface InputPort {
  /**
   * Add port listener.
   *
   * @param listener port listener
   * @require listener != null
   */
  public void addInputPortListener(InputPortListener listener);

  /**
   * Remove port listener.
   *
   * @param listener port listener
   * @require listener != null
   */
  public void removeInputPortListener(InputPortListener listener);

  /**
   * Connect output port to this input port.
   *
   * @param port output port to connect to.
   * @require port != null
   */
  public void connect(OutputPort port);

  /**
   * Disconnect output port from this input port.
   *
   * @param port output port to disconnect.
   * @require port != null
   */
  public void disconnect(OutputPort port);

  /**
   * Data of this input port.
   */
  public int inputData();

  /**
   * Mask of this input port.
   * Set bit means port bit is driven. Cleared bit means port bit is not driven.
   */
  public int inputMask();
}
