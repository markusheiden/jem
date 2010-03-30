package de.heiden.jem.models.c64.monitor.gui;

import de.heiden.c64dt.gui.JC64TextArea;
import de.heiden.c64dt.util.HexUtil;
import de.heiden.jem.models.c64.components.bus.BusDevice;

import javax.swing.JPanel;
import javax.swing.JScrollBar;
import java.awt.BorderLayout;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

/**
 * GUI for showing a memory dump.
 */
public class MemDumpGUI
  extends JPanel
  implements AdjustmentListener, MouseWheelListener
{
  private static final int BYTES_PER_LINE = 8;
  private int _lines = 32;

  private final JC64TextArea _text;
  private final JScrollBar _scrollBar;

  private BusDevice _bus;

  /**
   * Constructor.
   */
  public MemDumpGUI()
  {
    setLayout(new BorderLayout());

    _text = new JC64TextArea(39, _lines, 2);
    add(_text, BorderLayout.CENTER);

    _scrollBar = new JScrollBar(JScrollBar.VERTICAL);
    _scrollBar.setMinimum(0);
    _scrollBar.setMaximum(0x10000 - BYTES_PER_LINE);
    _scrollBar.setVisibleAmount(0x0100);
    _scrollBar.setBlockIncrement(0x0100);
    _scrollBar.setUnitIncrement(BYTES_PER_LINE);
    _scrollBar.setValue(0);
    add(_scrollBar, BorderLayout.EAST);

    // Update model on scroll bar change
    _scrollBar.addAdjustmentListener(this);
    // React on mouse wheel
    addMouseWheelListener(this);
  }

  @Override
  public void mouseWheelMoved(MouseWheelEvent e)
  {
    _scrollBar.setValue(_scrollBar.getValue() + e.getUnitsToScroll() * BYTES_PER_LINE);
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
    memoryChanged();
  }

  /**
   * Connect component to model (a bus).
   *
   * @param bus model
   */
  public void setBus(BusDevice bus)
  {
    _bus = bus;
    memoryChanged();
  }

  //
  // painting
  //

  /**
   * The memory changed, so update image.
   */
  public void memoryChanged()
  {
    // update text
    _text.clear();
    if (_bus != null)
    {
      int addr = _scrollBar.getValue();
      for (int i = 0; i < _lines; i++, addr += 8)
      {
        dumpLine(addr, i);
      }
    }

    repaint();
  }

  /**
   * Dump line.
   *
   * @param addr address to dump
   * @param row row to draw dump to
   */
  protected void dumpLine(int addr, int row)
  {
    _text.setText(0, row, HexUtil.hexWordPlain(addr));

    for (int i = 0, c = 0; i < 8; i++, c++)
    {
      _text.setText(6 + i * 3, row, HexUtil.hexBytePlain(_bus.read(addr + i) & 0xFF));
    }

    for (int i = 0, c = 0; i < 8; i++, c++)
    {
      _text.setText(30 + i, row, (byte) _bus.read(addr + i));
    }
  }
}
