package de.heiden.jem.components.ports;

import java.util.ArrayList;
import java.util.List;

/**
 * Input port implementation.
 */
public class InputPortImpl implements InputPort
{
  /**
   * Constructor.
   */
  public InputPortImpl()
  {
    _outputPorts = new ArrayList<OutputPort>();
    _outputListener = new OutputPortListener()
    {
      /**
       * Output port changed.
       */
      public void outputPortChanged(int value, int mask)
      {
        updateInputPort();
      }
    };
    _inputListeners = new ArrayList<InputPortListener>();
    _inputData = 0xFF;
    _inputMask = 0xFF;
  }

  /**
   * Connect output port to this input port.
   *
   * @param port output port to connect to.
   * @require port != null
   */
  public void connect(OutputPort port)
  {
    assert port != null : "port != null";

    _outputPorts.add(port);
    port.addOutputPortListener(_outputListener);

    updateInputPort();
  }

  /**
   * Disconnect output port from this input port.
   *
   * @param port output port to disconnect.
   * @require port != null
   */
  public void disconnect(OutputPort port)
  {
    _outputPorts.remove(port);
    port.removeOutputPortListener(_outputListener);
    updateInputPort();
  }

  /**
   * Add port listener.
   *
   * @param listener port listener
   * @require listener != null
   */
  public void addInputPortListener(InputPortListener listener)
  {
    assert listener != null : "listener != null";

    _inputListeners.add(listener);
  }

  /**
   * Remove port listener.
   *
   * @param listener port listener
   * @require listener != null
   */
  public void removeInputPortListener(InputPortListener listener)
  {
    assert listener != null : "listener != null";

    _inputListeners.remove(listener);
  }

  /**
   * Data of this input port.
   */
  public int inputData()
  {
    return _inputData;
  }

  /**
   * Mask of this input port.
   * Set bit means port bit is driven. Cleared bit means port bit is not driven.
   */
  public int inputMask()
  {
    return _inputMask;
  }

  //
  // protected interface
  //

  /**
   * Update current value of input port.
   */
  protected void updateInputPort()
  {
    int inputData = 0xFF;
    int inputMask = 0x00;
    for (int i = 0; i < _outputPorts.size(); i++)
    {
      OutputPort port = _outputPorts.get(i);
      inputData &= port.outputData();
      inputMask |= port.outputMask();
    }
    inputData |= 0xFF - inputMask;

    if (inputData != _inputData || inputMask != _inputMask)
    {
      _inputData = inputData;
      _inputMask = inputMask;
      notifyInputPortListeners();
    }
  }

  /**
   * Notify all listeners.
   */
  protected void notifyInputPortListeners()
  {
    for (int i = 0; i < _inputListeners.size(); i++)
    {
      _inputListeners.get(i).inputPortChanged(_inputData, _inputMask);
    }
  }

  //
  //
  // private attributes
  //

  private final List<OutputPort> _outputPorts;

  private final OutputPortListener _outputListener;

  private final List<InputPortListener> _inputListeners;

  private int _inputData;

  private int _inputMask;
}
