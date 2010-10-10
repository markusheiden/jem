package de.heiden.jem.models.c64.monitor.gui;

import de.heiden.c64dt.assembler.CodeBuffer;
import de.heiden.c64dt.assembler.Disassembler;
import de.heiden.c64dt.assembler.ICodeBuffer;
import de.heiden.c64dt.gui.JC64TextArea;
import de.heiden.jem.components.bus.BusDevice;

import javax.swing.JPanel;
import javax.swing.JScrollBar;
import java.awt.BorderLayout;
import java.awt.event.*;
import java.io.IOException;
import java.io.StringWriter;

/**
 * GUI for showing the disassembled code.
 */
public class DisassemblerGUI extends JPanel
{
  private static final int BYTES_PER_LINE = 1;

  private final JC64TextArea _text;
  private final JScrollBar _scrollBar;

  private final Disassembler _disassembler;

  private BusDevice _bus;

  public DisassemblerGUI()
  {
    setLayout(new BorderLayout());

    _text = new JC64TextArea(26, 25, 2);
    add(_text, BorderLayout.CENTER);

    _scrollBar = new JScrollBar(JScrollBar.VERTICAL);
    _scrollBar.setMinimum(0);
    _scrollBar.setMaximum(0x10000 - BYTES_PER_LINE);
    _scrollBar.setVisibleAmount(25);
    _scrollBar.setBlockIncrement(25);
    _scrollBar.setUnitIncrement(BYTES_PER_LINE);
    _scrollBar.setValue(0);
    add(_scrollBar, BorderLayout.EAST);

    _disassembler = new Disassembler();

    // Update scroll bar on containing component change
    _text.addComponentListener(new ComponentAdapter()
    {
      @Override
      public void componentResized(ComponentEvent e)
      {
        _scrollBar.setVisibleAmount(_text.getRows());
        _scrollBar.setBlockIncrement(_text.getRows());
        codeChanged();
      }
    });

    // Update model on scroll bar change
    _scrollBar.addAdjustmentListener(new AdjustmentListener()
    {
      @Override
      public void adjustmentValueChanged(AdjustmentEvent e)
      {
        setAddress(e.getValue());
      }
    });

    // React on mouse wheel
    addMouseWheelListener(new MouseWheelListener()
    {
      @Override
      public void mouseWheelMoved(MouseWheelEvent e)
      {
        _scrollBar.setValue(_scrollBar.getValue() + e.getUnitsToScroll());
      }
    });
  }

  //
  //
  //

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
        ICodeBuffer code = new BusCodeBuffer(_scrollBar.getValue(), _bus);
        for (int i = 0; i < _text.getRows() && code.has(1); i++)
        {
          _disassembler.disassemble(code, output);
        }
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
