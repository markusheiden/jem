package de.heiden.jem.components.ports;

import jakarta.annotation.Nonnull;

/**
 * Input / output port implementation.
 */
public final class InputOutputPortImpl implements InputOutputPort {
  /**
   * Input port.
   */
  private final InputPort inputPort = new InputPortImpl();

  /**
   * Output port.
   */
  private final OutputPort outputPort = new OutputPortImpl();

  /**
   * Connect the output port to this input port.
   *
   * @param port output port to connect to.
   * @require port != null
   */
  @Override
  public void connect(@Nonnull OutputPort port) {
    inputPort.connect(port);
  }

  /**
   * Disconnect the output port from this input port.
   *
   * @param port output port to disconnect.
   * @require port != null
   */
  @Override
  public void disconnect(@Nonnull OutputPort port) {
    inputPort.disconnect(port);
  }

  /**
   * Add the input port listener.
   *
   * @param listener port listener.
   * @require listener != null
   */
  @Override
  public void addInputPortListener(@Nonnull PortListener listener) {
    inputPort.addInputPortListener(listener);
  }

  /**
   * Remove the input port listener.
   *
   * @param listener port listener.
   * @require listener != null
   */
  @Override
  public void removeInputPortListener(@Nonnull PortListener listener) {
    inputPort.removeInputPortListener(listener);
  }

  /**
   * Data of this input port.
   */
  @Override
  public int inputData() {
    return inputPort.inputData();
  }

  /**
   * Mask of this input port.
   * Set bit means port bit is driven. Cleared bit means port bit is not driven.
   */
  @Override
  public int inputMask() {
    return inputPort.inputMask();
  }

  /**
   * Add the output port listener.
   *
   * @param listener port listener.
   * @require listener != null
   */
  @Override
  public void addOutputPortListener(@Nonnull PortListener listener) {
    outputPort.addOutputPortListener(listener);
  }

  /**
   * Remove the output port listener.
   *
   * @param listener port listener.
   * @require listener != null
   */
  @Override
  public void removeOutputPortListener(@Nonnull PortListener listener) {
    outputPort.removeOutputPortListener(listener);
  }

  /**
   * The port output data.
   */
  @Override
  public int outputData() {
    int mask = outputPort.outputMask();
    return outputPort.outputData() & mask | inputPort.inputData() & ~mask;
  }

  /**
   * The port output mask.
   * Set bit means port bit is output. Cleared bit means port bit is not driven.
   */
  @Override
  public int outputMask() {
    return outputPort.outputMask();
  }

  /**
   * Set the port output data.
   */
  @Override
  public void setOutputData(int data) {
    outputPort.setOutputData(data);
  }

  /**
   * Set the port output mask.
   * Set bit means port bit is output. Cleared bit means port bit is not driven.
   */
  @Override
  public void setOutputMask(int mask) {
    outputPort.setOutputMask(mask);
  }
}
