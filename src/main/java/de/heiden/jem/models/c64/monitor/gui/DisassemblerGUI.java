package de.heiden.jem.models.c64.monitor.gui;

import de.heiden.c64dt.assembler.Disassembler;
import de.heiden.c64dt.gui.JC64TextArea;
import de.heiden.jem.components.bus.BusDevice;

import javax.swing.JPanel;
import javax.swing.JScrollBar;
import java.awt.BorderLayout;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.IOException;
import java.io.StringWriter;

/**
 * GUI for showing the disassembled code.
 */
public class DisassemblerGUI
  extends JPanel
  implements AdjustmentListener, MouseWheelListener
{
  private static final int BYTES_PER_LINE = 1;
  private static final int BYTES_PER_LINE_AVG = 2;
  private int _lines = 32;

  private final JC64TextArea _text;
  private final JScrollBar _scrollBar;

  private final Disassembler _disassembler;

  private BusDevice _bus;

  public DisassemblerGUI()
  {
    setLayout(new BorderLayout());

    _text = new JC64TextArea(26, _lines, 2);
    add(_text, BorderLayout.CENTER);

    _scrollBar = new JScrollBar(JScrollBar.VERTICAL);
    _scrollBar.setMinimum(0);
    _scrollBar.setMaximum(0x10000 - BYTES_PER_LINE);
    _scrollBar.setVisibleAmount(_lines / BYTES_PER_LINE_AVG);
    _scrollBar.setBlockIncrement(_lines / BYTES_PER_LINE_AVG);
    _scrollBar.setUnitIncrement(BYTES_PER_LINE);
    _scrollBar.setValue(0);
    add(_scrollBar, BorderLayout.EAST);

    _disassembler = new Disassembler();

    // Update model on scroll bar change
    _scrollBar.addAdjustmentListener(this);
    // React on mouse wheel
    addMouseWheelListener(this);
  }

  @Override
  public void mouseWheelMoved(MouseWheelEvent e)
  {
    _scrollBar.setValue(_scrollBar.getValue() + e.getUnitsToScroll());
  }

  @Override
  public void adjustmentValueChanged(AdjustmentEvent e)
  {
    setAddress(e.getValue());
  }

  /**
   * Set address to display.
   */
  public void setAddress(int addr)
  {
    _scrollBar.setValue(addr);
    codeChanged();
  }

  /**
   * Connect component to model (a bus).
   *
   * @param bus model
   */
  public void setBus(BusDevice bus)
  {
    _bus = bus;
    codeChanged();
  }

  //
  // painting
  //

  /**
   * The memory changed, so update image.
   */
  public void codeChanged()
  {
    // update text
    _text.clear();
    if (_bus != null)
    {
      try
      {
        StringWriter output = new StringWriter();
        int addr = _scrollBar.getValue();
        byte[] code = new byte[_lines];
        for (int i = 0; i < _lines; i++)
        {
          code[i] = (byte) _bus.read(addr + i);
        }
        _disassembler.disassemble(addr, code, output);

        _text.setText(0, 0, output.toString());
      }
      catch (IOException e)
      {
        // ignore
      }
    }

    repaint();
  }
}
