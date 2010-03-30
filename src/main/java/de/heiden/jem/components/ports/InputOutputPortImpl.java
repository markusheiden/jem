package de.heiden.jem.components.ports;

/**
 * Input / output port abstract implementation.
 */
public class InputOutputPortImpl implements InputOutputPort
{
  /**
   * Constructor.
   */
  public InputOutputPortImpl ()
  {
    _inputPort = new InputPortImpl();
    _outputPort = new OutputPortImpl();
  }

  /**
   * Connect output port to this input port.
   *
   * @param port output port to connect to.
   * @require port != null
   */
  public void connect (OutputPort port)
  {
    _inputPort.connect(port);
  }

  /**
   * Disconnect output port from this input port.
   *
   * @param port output port to disconnect.
   * @require port != null
   */
  public void disconnect (OutputPort port)
  {
    _inputPort.disconnect(port);
  }

  /**
   * Add port listener.
   *
   * @param listener port listener
   * @require listener != null
   */
  public void addInputPortListener (InputPortListener listener)
  {
    _inputPort.addInputPortListener(listener);
  }

  /**
   * Remove port listener.
   *
   * @param listener port listener
   * @require listener != null
   */
  public void removeInputPortListener (InputPortListener listener)
  {
    _inputPort.removeInputPortListener(listener);
  }

  /**
   * Data of this input port.
   */
  public int inputData ()
  {
    return _inputPort.inputData();
  }

  /**
   * Mask of this input port.
   * Set bit means port bit is driven. Cleared bit means port bit is not driven.
   */
  public int inputMask ()
  {
    return _inputPort.inputMask();
  }

  /**
   * Add port listener.
   *
   * @param listener port listener
   * @require listener != null
   */
  public void addOutputPortListener (OutputPortListener listener)
  {
    _outputPort.addOutputPortListener(listener);
  }

  /**
   * Remove port listener.
   *
   * @param listener port listener
   * @require listener != null
   */
  public void removeOutputPortListener (OutputPortListener listener)
  {
    _outputPort.removeOutputPortListener(listener);
  }

  /**
   * Port output data.
   */
  public int outputData ()
  {
    int mask = _outputPort.outputMask();
    return _outputPort.outputData() & mask | _inputPort.inputData() & (0xFF - mask);
  }

  /**
   * Port output mask.
   * Set bit means port bit is output. Cleared bit means port bit is not driven.
   */
  public int outputMask ()
  {
    return _outputPort.outputMask();
  }

  /**
   * Set port output data.
   */
  public void setOutputData (int data)
  {
    _outputPort.setOutputData(data);
  }

  /**
   * Set port output mask.
   * Set bit means port bit is output. Cleared bit means port bit is not driven.
   */
  public void setOutputMask (int mask)
  {
    _outputPort.setOutputMask(mask);
  }

  //
  // private attributes
  //

  private final InputPortImpl _inputPort;

  private final OutputPortImpl _outputPort;
}
