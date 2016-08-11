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
  void addInputPortListener(PortListener listener);

  /**
   * Remove port listener.
   *
   * @param listener port listener
   * @require listener != null
   */
  void removeInputPortListener(PortListener listener);

  /**
   * Connect output port to this input port.
   *
   * @param port output port to connect to.
   * @require port != null
   */
  void connect(OutputPort port);

  /**
   * Disconnect output port from this input port.
   *
   * @param port output port to disconnect.
   * @require port != null
   */
  void disconnect(OutputPort port);

  /**
   * Data of this input port.
   */
  int inputData();

  /**
   * Mask of this input port.
   * Set bit means port bit is driven. Cleared bit means port bit is not driven.
   */
  int inputMask();
}
