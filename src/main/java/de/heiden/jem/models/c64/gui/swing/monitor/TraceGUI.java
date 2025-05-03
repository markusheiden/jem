package de.heiden.jem.models.c64.gui.swing.monitor;

import de.heiden.c64dt.assembler.Disassembler;
import de.heiden.c64dt.gui.swing.JC64TextArea;
import de.heiden.jem.models.c64.components.cpu.CPU6510Debugger;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.StringWriter;

/**
 * GUI for showing the trace of execution.
 */
public class TraceGUI extends JPanel {
  private final JC64TextArea text;

  private final Disassembler disassembler;

  private CPU6510Debugger cpu;

  /**
   * Constructor.
   */
  public TraceGUI() {
    setLayout(new BorderLayout());

    text = new JC64TextArea(26, 10, 2, false);

    add(text, BorderLayout.CENTER);

    disassembler = new Disassembler();
  }

  public final void setCpu(CPU6510Debugger cpu) {
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
      try {
        var currentTrace = cpu.getCurrentTrace();
        var traces = cpu.getTraces();
        for (int i = 9; i >= 0; i--) {
          currentTrace--;
          if (currentTrace < 0) {
            currentTrace = traces.length - 1;
          }
          var trace = traces[currentTrace];

          var output = new StringWriter(20);
          disassembler.disassemble(trace.toCodeBuffer(), output);

          text.setText(0, i, output.toString());

        }
      } catch (IOException e) {
        // ignore
      }
    }

    repaint();
  }
}
