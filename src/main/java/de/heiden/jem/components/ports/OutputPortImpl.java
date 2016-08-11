package de.heiden.jem.components.ports;

/**
 * Output port abstract implementation
 */
public class OutputPortImpl implements OutputPort {
  /**
   * Listeners
   */
  private OutputPortListener _outputListeners;

  /**
   * Port data.
   */
  private volatile int _outputData = 0xFF;

  /**
   * Output mask: bit == 1 -> line is output.
   */
  private volatile int _outputMask = 0x00;

  /**
   * Add port listener.
   *
   * @param newListener port listener
   * @require listener != null
   */
  @Override
  public void addOutputPortListener(OutputPortListener newListener) {
    assert newListener != null : "newListener != null";
    assert newListener.next == null : "newListener.next == null";

    OutputPortListener listener = _outputListeners;
    if (listener == null) {
      _outputListeners = newListener;
    } else {
      while (listener.next != null) {
        listener = listener.next;
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
  @Override
  public void removeOutputPortListener(OutputPortListener oldListener) {
    assert oldListener != null : "oldListener != null";

    OutputPortListener listener = _outputListeners;
    if (listener == oldListener) {
      _outputListeners = oldListener.next;
    } else {
      while (listener.next != oldListener) {
        listener = listener.next;
      }
      listener.next = oldListener.next;
    }
    oldListener.next = null;
  }

  /**
   * Port output data.
   */
  @Override
  public int outputData() {
    return _outputData;
  }

  /**
   * Port output mask.
   * Set bit means port bit is output. Cleared bit means port bit is not driven.
   */
  @Override
  public final int outputMask() {
    return _outputMask;
  }

  /**
   * Set port output data.
   *
   * @param data new output value
   */
  public void setOutputData(int data) {
    _outputData = data;
    notifyOutputPortListeners();
  }

  /**
   * Set port output mask.
   * Set bit means port bit is output. Cleared bit means port bit is not driven.
   *
   * @param mask driven bits
   */
  public void setOutputMask(int mask) {
    _outputMask = mask;
    notifyOutputPortListeners();
  }

  //
  // protected
  //

  /**
   * Notify all listeners.
   */
  protected final void notifyOutputPortListeners() {
    OutputPortListener listener = _outputListeners;
    final int outputData = outputData();
    final int outputMask = _outputMask;
    while (listener != null) {
      listener.outputPortChanged(outputData, outputMask);
      listener = listener.next;
    }
  }
}
