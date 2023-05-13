package de.heiden.jem.components.ports;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Input port implementation.
 */
public final class InputPortImpl implements InputPort {
  /**
   * Output ports and the listeners listening to them.
   */
  private final Map<OutputPort, PortListener> _outputPortListeners = new HashMap<>();

  /**
   * Output ports driving this port.
   */
  private OutputPort[] _outputPorts = new OutputPort[0];

  /**
   * Input port listeners.
   */
  private List<PortListener> _inputListenerLists = new ArrayList<>();

  /**
   * Input port listener array.
   */
  private PortListener[] _inputListeners = new PortListener[0];

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

    PortListener listener = (value, mask) -> updateInputPort();
    _outputPortListeners.put(port, listener);
    _outputPorts = _outputPortListeners.keySet().toArray(OutputPort[]::new);
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
    PortListener listener = _outputPortListeners.remove(port);
    _outputPorts = _outputPortListeners.keySet().toArray(OutputPort[]::new);
    port.removeOutputPortListener(listener);
    updateInputPort();
  }

  /**
   * Add input port listener.
   *
   * @param listener port listener.
   * @require listener != null
   */
  @Override
  public void addInputPortListener(PortListener listener) {
    assert listener != null : "listener != null";

    _inputListenerLists.add(listener);
    _inputListeners = _inputListenerLists.toArray(PortListener[]::new);
  }

  /**
   * Remove input port listener.
   *
   * @param listener port listener.
   * @require listener != null
   */
  @Override
  public void removeInputPortListener(PortListener listener) {
    assert listener != null : "listener != null";

    _inputListenerLists.remove(listener);
    _inputListeners = _inputListenerLists.toArray(PortListener[]::new);
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
    final int inputData = _inputData;
    final int inputMask = _inputMask;
    for (PortListener listener : _inputListeners) {
      listener.portChanged(inputData, inputMask);
    }
  }
}
