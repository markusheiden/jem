package de.heiden.jem.models.c64.gui.swing.monitor;

import de.heiden.c64dt.bytes.HexUtil;
import de.heiden.c64dt.gui.swing.JC64TextArea;
import de.heiden.jem.models.c64.components.cpu.CPU6510;

import javax.swing.*;
import java.awt.*;

/**
 * GUI for showing the state of the processor.
 */
public class StateGUI extends JPanel {
  private final JC64TextArea text;

  private CPU6510 cpu;

  /**
   * Constructor.
   */
  public StateGUI() {
    setLayout(new BorderLayout());

    text = new JC64TextArea(12, 7, 2, false);

    add(text, BorderLayout.CENTER);
  }

  public final void setCpu(CPU6510 cpu) {
    this.cpu = cpu;

    stateChanged();
  }

  //
  // painting
  //

  /**
   * The state changed, so update text.
   */
  public void stateChanged() {
    // update text
    text.clear();
    if (cpu != null) {
      var state = cpu.getState();

      text.setText(0, 0, "PC=" + HexUtil.hexWord(state.PC));
      text.setText(0, 1, "S =" + HexUtil.hexWord(state.getS()));
      text.setText(0, 2, "A =" + HexUtil.hexByte(state.A));
      text.setText(0, 3, "X =" + HexUtil.hexByte(state.X));
      text.setText(0, 4, "Y =" + HexUtil.hexByte(state.Y));
      text.setText(0, 5, "P =");
      int c = 3;
      text.setText(c++, 5, state.N ? "N" : "n");
      text.setText(c++, 5, state.V ? "V" : "v");
      text.setText(c++, 5, "1");
      text.setText(c++, 5, state.B ? "B" : "b");
      text.setText(c++, 5, state.D ? "D" : "d");
      text.setText(c++, 5, state.I ? "I" : "i");
      text.setText(c++, 5, state.Z ? "Z" : "z");
      text.setText(c++, 5, state.C ? "C" : "c");
      text.setText(0, 6, (state.NMI ? "NMI" : "") + (state.IRQ ? " IRQ" : ""));
    }

    repaint();
  }
}
