package de.heiden.jem.components.ports;

/**
 * Output port abstract implementation
 */
public class OutputPortImpl implements OutputPort
{
  private OutputPortListener _outputListeners;

  private int _outputData;

  private int _outputMask;

  /**
   * Constructor.
   */
  public OutputPortImpl()
  {
    _outputListeners = null;
    _outputData = 0xFF;
    _outputMask = 0x00;
  }

  /**
   * Add port listener.
   *
   * @param newListener port listener
   * @require listener != null
   */
  public void addOutputPortListener(OutputPortListener newListener)
  {
    assert newListener != null : "newListener != null";
    assert newListener.next == null : "newListener.next == null";

    OutputPortListener listener = _outputListeners;
    if (listener == null)
    {
      _outputListeners = newListener;
    }
    else
    {
      OutputPortListener next;
      while ((next = listener.next) != null)
      {
        listener = next;
      }
      listener.next = newListener;
    }
  }

  /**
   * Remove port listener.
   *
   * @param oldListener port listener
   * @require listener != null
   */
  public void removeOutputPortListener(OutputPortListener oldListener)
  {
    assert oldListener != null : "oldListener != null";

    OutputPortListener listener = _outputListeners;
    if (listener == oldListener)
    {
      _outputListeners = oldListener.next;
    }
    else
    {
      OutputPortListener next;
      while ((next = listener.next) != oldListener)
      {
        listener = next;
      }
      listener.next = oldListener.next;
    }
    oldListener.next = null;
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
    OutputPortListener listener = _outputListeners;
    while (listener != null)
    {
      listener.outputPortChanged(_outputData, _outputMask);
      listener = listener.next;
    }
  }
}
