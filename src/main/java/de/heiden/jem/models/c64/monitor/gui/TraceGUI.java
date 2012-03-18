package de.heiden.jem.models.c64.monitor.gui;

import de.heiden.c64dt.assembler.CodeBuffer;
import de.heiden.c64dt.assembler.Disassembler;
import de.heiden.c64dt.gui.JC64TextArea;
import de.heiden.jem.models.c64.components.cpu.CPU6510Debugger;
import de.heiden.jem.models.c64.components.cpu.Trace;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.StringWriter;

/**
 * GUI for showing the trace of execution.
 */
public class TraceGUI extends JPanel {
  private final JC64TextArea _text;

  private final Disassembler _disassembler;

  private CPU6510Debugger _cpu;

  /**
   * Constructor.
   */
  public TraceGUI() {
    setLayout(new BorderLayout());

    _text = new JC64TextArea(26, 10, 2, false);

    add(_text, BorderLayout.CENTER);

    _disassembler = new Disassembler();
  }

  public final void setCpu(CPU6510Debugger cpu) {
    _cpu = cpu;

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
    _text.clear();
    if (_cpu != null) {
      try {
        Trace[] traces = _cpu.getTraces();
        int currentTrace = _cpu.getCurrentTrace();
        for (int i = 9; i >= 0; i--) {
          currentTrace = (currentTrace + 1) % traces.length;
          Trace trace = traces[currentTrace];

          StringWriter output = new StringWriter(20);
          byte[] bytes = {(byte) trace.opcode.getOpcode(), ((byte) (trace.argument & 0xFF)), ((byte) (trace.argument >> 8))};
          CodeBuffer code = new CodeBuffer(trace.address, bytes);
          _disassembler.disassemble(code, output);

          _text.setText(0, i, output.toString());
        }
      } catch (IOException e) {
        // ignore
      }
    }

    repaint();
  }
}
