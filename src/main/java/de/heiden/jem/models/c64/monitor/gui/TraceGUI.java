package de.heiden.jem.models.c64.monitor.gui;

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
public class TraceGUI extends JPanel
{
  private final JC64TextArea _text;

  private final Disassembler _disassembler;

  private CPU6510Debugger _cpu;

  /**
   * Constructor.
   */
  public TraceGUI()
  {
    setLayout(new BorderLayout());

    _text = new JC64TextArea(26, 10, 2, false);

    add(_text, BorderLayout.CENTER);

    _disassembler = new Disassembler();
  }

  public final void setCpu(CPU6510Debugger cpu)
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
      int currentTrace = _cpu.getCurrentTrace();
      if (currentTrace >= 0)
      {
        Trace[] traces = _cpu.getTraces();
        for (int i = 0; i < 10; i++, currentTrace = (currentTrace + 1) % traces.length)
        {
          Trace trace = traces[currentTrace];
          StringWriter output = new StringWriter(20);
          byte[] bytes = {(byte) trace.opcode.getOpcode(), ((byte) (trace.argument & 0xFF)), ((byte) (trace.argument >> 8))};
          try
          {
            _disassembler.disassemble(trace.address, bytes, output);
          }
          catch (IOException e)
          {
            e.printStackTrace();
          }
          _text.setText(0, i, output.toString());
        }
      }
    }

    repaint();
  }
}
