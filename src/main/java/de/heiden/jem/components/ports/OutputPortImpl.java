package de.heiden.jem.components.ports;

import java.util.ArrayList;
import java.util.List;

/**
 * Output port abstract implementation
 */
public class OutputPortImpl implements OutputPort
{
  private final List<OutputPortListener> _outputListeners;

  private int _outputData;

  private int _outputMask;

  /**
   * Constructor.
   */
  public OutputPortImpl()
  {
    _outputListeners = new ArrayList<OutputPortListener>();
    _outputData = 0xFF;
    _outputMask = 0x00;
  }

  /**
   * Add port listener.
   *
   * @param listener port listener
   * @require listener != null
   */
  public void addOutputPortListener(OutputPortListener listener)
  {
    assert listener != null : "listener != null";

    _outputListeners.add(listener);
  }

  /**
   * Remove port listener.
   *
   * @param listener port listener
   * @require listener != null
   */
  public void removeOutputPortListener(OutputPortListener listener)
  {
    assert listener != null : "listener != null";

    _outputListeners.remove(listener);
  }

  /**
   * Port output data.
   */
  public int outputData()
  {
    return _outputData;
  }

  /**
   * Port output mask.
   * Set bit means port bit is output. Cleared bit means port bit is not driven.
   */
  public int outputMask()
  {
    return _outputMask;
  }

  /**
   * Set port output data.
   *
   * @param data new output value
   */
  public void setOutputData(int data)
  {
    _outputData = data;
    notifyOutputPortListeners();
  }

  /**
   * Set port output mask.
   * Set bit means port bit is output. Cleared bit means port bit is not driven.
   *
   * @param mask driven bits
   */
  public void setOutputMask(int mask)
  {
    _outputMask = mask;
    notifyOutputPortListeners();
  }

  //
  // protected
  //

  /**
   * Notify all listeners.
   */
  protected final void notifyOutputPortListeners()
  {
    List<OutputPortListener> listeners = _outputListeners;
    for (int i = 0, size = listeners.size(); i < size; i++)
    {
      listeners.get(i).outputPortChanged(_outputData, _outputMask);
    }
  }
}
