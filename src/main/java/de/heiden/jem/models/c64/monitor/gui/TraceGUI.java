package de.heiden.jem.models.c64.monitor.gui;

import de.heiden.c64dt.gui.JC64TextArea;
import de.heiden.jem.models.c64.components.cpu.CPU6510Debugger;
import de.heiden.jem.models.c64.components.cpu.Trace;

import javax.swing.*;
import java.awt.*;

/**
 * GUI for showing the trace of execution.
 */
public class TraceGUI extends JPanel
{
  private final JC64TextArea _text;

  private CPU6510Debugger _cpu;

  /**
   * Constructor.
   */
  public TraceGUI()
  {
    setLayout(new BorderLayout());

    _text = new JC64TextArea(12, 7, 2, false);

    add(_text, BorderLayout.CENTER);
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
      int tracePoint = _cpu.getTracePoint();
      if (tracePoint >= 0)
      {
        Trace[] traces = _cpu.getTraces();
        for (int i = 0; i < 10; i++, tracePoint = (tracePoint + 1) % traces.length)
        {
          _text.setText(0, i, traces[tracePoint].toString());
        }
      }
    }

    repaint();
  }
}
