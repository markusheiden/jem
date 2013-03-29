package de.heiden.jem.components.ports;

import java.util.HashMap;
import java.util.Map;

/**
 * Input port implementation.
 */
public final class InputPortImpl implements InputPort {
  /**
   * Output ports and the listeners listening to them.
   */
  private final Map<OutputPort, OutputPortListener> _outputPortListeners = new HashMap<>();

  /**
   * Output ports driving this port.
   */
  private OutputPort[] _outputPorts = new OutputPort[0];

  /**
   * Input listeners.
   */
  private InputPortListener _inputListeners = null;

  /**
   * Port data.
   */
  private int _inputData = 0xFF;

  /**
   * Input mask: bit == 1 -> line is input.
   */
  private int _inputMask = 0xFF;

  /**
   * Connect output port to this input port.
   *
   * @param port output port to connect to.
   * @require port != null
   */
  @Override
  public void connect(OutputPort port) {
    assert port != null : "port != null";

    OutputPortListener listener = new OutputPortListener() {
      /**
       * Output port changed.
       */
      @Override
      public final void outputPortChanged(int value, int mask) {
        updateInputPort();
      }
    };

    _outputPortListeners.put(port, listener);
    _outputPorts = _outputPortListeners.keySet().toArray(new OutputPort[_outputPortListeners.size()]);
    port.addOutputPortListener(listener);
    updateInputPort();
  }

  /**
   * Disconnect output port from this input port.
   *
   * @param port output port to disconnect.
   * @require port != null
   */
  @Override
  public void disconnect(OutputPort port) {
    OutputPortListener listener = _outputPortListeners.remove(port);
    _outputPorts = _outputPortListeners.keySet().toArray(new OutputPort[_outputPortListeners.size()]);
    port.removeOutputPortListener(listener);
    updateInputPort();
  }

  /**
   * Add port listener.
   *
   * @param newListener port listener
   * @require listener != null
   */
  @Override
  public void addInputPortListener(InputPortListener newListener) {
    assert newListener != null : "newListener != null";
    assert newListener.next == null : "newListener.next == null";

    InputPortListener listener = _inputListeners;
    if (listener == null) {
      _inputListeners = newListener;
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
  public void removeInputPortListener(InputPortListener oldListener) {
    assert oldListener != null : "oldListener != null";

    InputPortListener listener = _inputListeners;
    if (listener == oldListener) {
      _inputListeners = oldListener.next;
    } else {
      while (listener.next != oldListener) {
        listener = listener.next;
      }
      listener.next = oldListener.next;
    }
    oldListener.next = null;
  }

  /**
   * Data of this input port.
   */
  @Override
  public int inputData() {
    return _inputData;
  }

  /**
   * Mask of this input port.
   * Set bit means port bit is driven. Cleared bit means port bit is not driven.
   */
  @Override
  public int inputMask() {
    return _inputMask;
  }

  //
  // protected interface
  //

  /**
   * Update current value of input port.
   */
  protected final void updateInputPort() {
    int inputData = 0xFF;
    int inputMask = 0x00;
    for (OutputPort port : _outputPorts) {
      inputData &= port.outputData();
      inputMask |= port.outputMask();
    }
    inputData |= 0xFF - inputMask;

    if (inputData != _inputData || inputMask != _inputMask) {
      _inputData = inputData;
      _inputMask = inputMask;
      notifyInputPortListeners();
    }
  }

  /**
   * Notify all listeners.
   */
  protected final void notifyInputPortListeners() {
    InputPortListener listener = _inputListeners;
    final int inputData = _inputData;
    final int inputMask = _inputMask;
    while (listener != null) {
      listener.inputPortChanged(inputData, inputMask);
      listener = listener.next;
    }
  }
}
