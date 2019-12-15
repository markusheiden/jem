package de.heiden.jem.components.ports;

import java.util.ArrayList;
import java.util.List;

/**
 * Output port abstract implementation
 */
public final class OutputPortImpl implements OutputPort {
  /**
   * Output port listeners.
   */
  private final List<PortListener> _outputListenerList = new ArrayList<>();

  /**
   * Output port listener array.
   */
  private PortListener[] _outputListeners = new PortListener[0];

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
   * @param listener port listener.
   * @require listener != null
   */
  @Override
  public void addOutputPortListener(PortListener listener) {
    assert listener != null : "listener != null";

    _outputListenerList.add(listener);
    _outputListeners = _outputListenerList.toArray(new PortListener[0]);
  }

  /**
   * Remove port listener.
   *
   * @param listener port listener.
   * @require listener != null
   */
  @Override
  public void removeOutputPortListener(PortListener listener) {
    assert listener != null : "listener != null";

    _outputListenerList.remove(listener);
    _outputListeners = _outputListenerList.toArray(new PortListener[0]);
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
  public int outputMask() {
    return _outputMask;
  }

  /**
   * Set port output data.
   *
   * @param data new output value.
   */
  @Override
  public void setOutputData(int data) {
    _outputData = data;
    notifyOutputPortListeners();
  }

  /**
   * Set port output mask.
   * Set bit means port bit is output. Cleared bit means port bit is not driven.
   *
   * @param mask driven bits.
   */
  @Override
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
    final int outputData = _outputData;
    final int outputMask = _outputMask;
    for (PortListener listener : _outputListeners) {
      listener.portChanged(outputData, outputMask);
    }
  }
}
