package de.heiden.jem.components.ports;

/**
 * Input / output port implementation.
 */
public final class InputOutputPortImpl implements InputOutputPort {
  /**
   * Input port.
   */
  private final InputPortImpl _inputPort = new InputPortImpl();

  /**
   * Output port.
   */
  private final OutputPortImpl _outputPort = new OutputPortImpl() {
    @Override
    public int outputData() {
      int mask = outputMask();
      return super.outputData() & mask | _inputPort.inputData() & (0xFF - mask);
    }
  };

  /**
   * Connect output port to this input port.
   *
   * @param port output port to connect to.
   * @require port != null
   */
  @Override
  public void connect(OutputPort port) {
    _inputPort.connect(port);
  }

  /**
   * Disconnect output port from this input port.
   *
   * @param port output port to disconnect.
   * @require port != null
   */
  @Override
  public void disconnect(OutputPort port) {
    _inputPort.disconnect(port);
  }

  /**
   * Add port listener.
   *
   * @param listener port listener
   * @require listener != null
   */
  @Override
  public void addInputPortListener(InputPortListener listener) {
    _inputPort.addInputPortListener(listener);
  }

  /**
   * Remove port listener.
   *
   * @param listener port listener
   * @require listener != null
   */
  @Override
  public void removeInputPortListener(InputPortListener listener) {
    _inputPort.removeInputPortListener(listener);
  }

  /**
   * Data of this input port.
   */
  @Override
  public int inputData() {
    return _inputPort.inputData();
  }

  /**
   * Mask of this input port.
   * Set bit means port bit is driven. Cleared bit means port bit is not driven.
   */
  @Override
  public int inputMask() {
    return _inputPort.inputMask();
  }

  /**
   * Add port listener.
   *
   * @param listener port listener
   * @require listener != null
   */
  @Override
  public void addOutputPortListener(OutputPortListener listener) {
    _outputPort.addOutputPortListener(listener);
  }

  /**
   * Remove port listener.
   *
   * @param listener port listener
   * @require listener != null
   */
  @Override
  public void removeOutputPortListener(OutputPortListener listener) {
    _outputPort.removeOutputPortListener(listener);
  }

  /**
   * Port output data.
   */
  @Override
  public int outputData() {
    return _outputPort.outputData();
  }

  /**
   * Port output mask.
   * Set bit means port bit is output. Cleared bit means port bit is not driven.
   */
  @Override
  public int outputMask() {
    return _outputPort.outputMask();
  }

  /**
   * Set port output data.
   */
  public void setOutputData(int data) {
    _outputPort.setOutputData(data);
  }

  /**
   * Set port output mask.
   * Set bit means port bit is output. Cleared bit means port bit is not driven.
   */
  public void setOutputMask(int mask) {
    _outputPort.setOutputMask(mask);
  }

  /**
   * Consolidated value of data register.
   */
  public int data() {
    return outputData() & outputMask() | inputData() & ~outputMask();
  }
}
