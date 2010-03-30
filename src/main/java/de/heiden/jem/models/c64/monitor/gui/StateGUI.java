package de.heiden.jem.models.c64.monitor.gui;

import de.heiden.c64dt.gui.JC64TextArea;
import de.heiden.c64dt.util.HexUtil;
import de.heiden.jem.models.c64.components.cpu.CPU6510;
import de.heiden.jem.models.c64.components.cpu.CPU6510State;

import javax.swing.JPanel;
import java.awt.BorderLayout;

/**
 * State of the processor.
 */
public class StateGUI extends JPanel
{
  private final JC64TextArea _text;

  private CPU6510 _cpu;

  /**
   * Constructor.
   */
  public StateGUI()
  {
    setLayout(new BorderLayout());

    _text = new JC64TextArea(12, 7, 2);

    add(_text, BorderLayout.CENTER);
  }

  public final void setCpu(CPU6510 cpu)
  {
    _cpu = cpu;

    stateChanged();
  }

  //
  // painting
  //

  /**
   * The state changed, so update text.
   */
  public void stateChanged()
  {
    // update text
    _text.clear();
    if (_cpu != null)
    {
      CPU6510State state = _cpu.getState();

      _text.setText(0, 0, "PC=" + HexUtil.hexWord(state.PC));
      _text.setText(0, 1, "S =" + HexUtil.hexWord(state.S));
      _text.setText(0, 2, "A =" + HexUtil.hexByte(state.A));
      _text.setText(0, 3, "X =" + HexUtil.hexByte(state.X));
      _text.setText(0, 4, "Y =" + HexUtil.hexByte(state.Y));
      _text.setText(0, 5, "P =");
      int c = 3;
      _text.setText(c++, 5, state.N ? "N" : "n");
      _text.setText(c++, 5, state.V ? "V" : "v");
      _text.setText(c++, 5, "1");
      _text.setText(c++, 5, state.B ? "B" : "b");
      _text.setText(c++, 5, state.D ? "D" : "d");
      _text.setText(c++, 5, state.I ? "I" : "i");
      _text.setText(c++, 5, state.Z ? "Z" : "z");
      _text.setText(c++, 5, state.C ? "C" : "c");
      _text.setText(0, 6, (state.NMI ? "NMI" : "") + (state.IRQ ? " IRQ" : ""));
    }

    repaint();
  }
}
