package de.heiden.jem.models.c64.components.keyboard;

import de.heiden.jem.components.ports.InputOutputPort;
import de.heiden.jem.components.ports.OutputPort;
import de.heiden.jem.components.ports.OutputPortImpl;
import de.heiden.jem.components.ports.OutputPortListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.heiden.jem.models.c64.components.keyboard.Key.RESTORE;

/**
 * Keyboard.
 * <p/>
 * TODO mapping of @, :, /, arrow up, ;, *, pound, commodore, run stop, arrow left
 * TODO C64 like mapping of -, +, home, shift lock, control?
 */
public class Keyboard implements IKeyboard {
  /**
   * Logger.
   */
  private final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * Ports for key matrix.
   */
  private final InputOutputPort _port0;
  private final InputOutputPort _port1;

  private final OutputPortImpl _matrixPort0;
  private final OutputPortImpl _matrixPort1;
  private final OutputPortImpl _nmi;

  /**
   * C64 key matrix.
   */
  private final int[] _matrix;

  /**
   * Constructor.
   *
   * @param port0 port for rows
   * @param port1 port for columns
   * @require port0 != null
   * @require port1 != null
   */
  public Keyboard(InputOutputPort port0, InputOutputPort port1) {
    assert port0 != null : "port0 != null";
    assert port1 != null : "port1 != null";

    // connect to input ports
    _port0 = port0;
    _port0.addOutputPortListener(new OutputPortListener() {
      /**
       * Output port changed.
       */
      @Override
      public void outputPortChanged(int value, int mask) {
        updatePorts();
      }
    });
    _port1 = port1;
    _port1.addOutputPortListener(new OutputPortListener() {
      /**
       * Output port changed.
       */
      @Override
      public void outputPortChanged(int value, int mask) {
        updatePorts();
      }
    });

    // connect matrix ports to input ports
    _matrixPort0 = new OutputPortImpl();
    _port0.connect(_matrixPort0);
    _matrixPort1 = new OutputPortImpl();
    _port1.connect(_matrixPort1);
    _nmi = new OutputPortImpl();
    _nmi.setOutputMask(0x1);

    // init matrix
    _matrix = new int[]{0, 0, 0, 0, 0, 0, 0, 0};
  }

  /**
   * NMI output.
   */
  public OutputPort getNMI() {
    assert _nmi != null : "Postcondition: result != null";
    return _nmi;
  }

  @Override
  public void press(Key key) {
    if (key == RESTORE) {
      // low active
      _nmi.setOutputData(0x0);
    } else {
      _matrix[key.getRow()] |= 1 << key.getColumn();
      updatePorts();
      logger.trace(keyMatrixToString());
    }
  }

  @Override
  public void release(Key key) {
    if (key == RESTORE) {
      // low active
      _nmi.setOutputData(0x1);
    } else {
      _matrix[key.getRow()] &= 0xFF - (1 << key.getColumn());
      updatePorts();
      logger.trace(keyMatrixToString());
    }
  }

  /**
   * Update ports from matrix.
   * <p/>
   * Assuming low is stronger than hi.
   * Assuming pull ups.
   */
  protected final void updatePorts() {
    int port1InMask = _port1.outputMask();
    int port1InDataInv = 0xFF - _port1.outputData();
    int port0OutMask = 0x00;
    int port0OutData = 0xFF;

    int port0InMask = _port0.outputMask();
    int port0InData = _port0.outputData();
    int port1OutMask = 0x00;
    int port1OutData = 0xFF;

    int bit = 0x01;
    for (int i = 0; i < _matrix.length; i++, bit <<= 1) {
      final int matrix = _matrix[i];
      final int bitmask = port1InMask & matrix; // 1: driven bits
      if (bitmask != 0) {
        port0OutMask |= bit; // bit is driven!
        if ((port1InDataInv & bitmask) != 0) {
          port0OutData &= 0xFF - bit; // save 0 bits. 0 is stronger than 1.
        }
      }

      if ((port0InMask & bit) != 0) {
        // line is driven
        port1OutMask |= matrix;
        if ((port0InData & bit) == 0) {
          // line is 0
          port1OutData &= 0xFF - matrix;
        }
      }
    }

    _matrixPort0.setOutputData(port0OutData);
    _matrixPort0.setOutputMask(port0OutMask);
    _matrixPort1.setOutputData(port1OutData);
    _matrixPort1.setOutputMask(port1OutMask);
  }

  @Override
  public String toString() {
    return keyMatrixToString();
  }

  /**
   * Print key matrix.
   */
  protected String keyMatrixToString() {
    StringBuilder result = new StringBuilder(128);
    result.append("Keyboard:\n");
    for (int row : _matrix) {
      for (int i = 7; i >= 0; i--) {
        result.append((row & (1 << i)) != 0 ? "X" : "+");
      }
      result.append("\n");
    }

    return result.toString();
  }
}
