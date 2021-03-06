package de.heiden.jem.models.c64.gui.swing.monitor;

import de.heiden.c64dt.bytes.HexUtil;
import de.heiden.c64dt.gui.swing.JC64TextArea;
import de.heiden.jem.components.bus.BusDevice;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * GUI for showing a memory dump.
 */
public class MemDumpGUI extends JPanel {
  private static final int BYTES_PER_LINE = 8;

  private final JC64TextArea _text;
  private final JScrollBar _scrollBar;

  private BusDevice _bus;

  /**
   * Constructor.
   */
  public MemDumpGUI() {
    setLayout(new BorderLayout());

    _text = new JC64TextArea(39, 25, 2, true);
    add(_text, BorderLayout.CENTER);

    _scrollBar = new JScrollBar(JScrollBar.VERTICAL);
    _scrollBar.setMinimum(0);
    _scrollBar.setMaximum(0x10000 - BYTES_PER_LINE);
    _scrollBar.setVisibleAmount(0x0100);
    _scrollBar.setBlockIncrement(0x0100);
    _scrollBar.setUnitIncrement(BYTES_PER_LINE);
    _scrollBar.setValue(0);
    add(_scrollBar, BorderLayout.EAST);

    // Update scroll bar on containing component change
    _text.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        _scrollBar.setVisibleAmount(_text.getRows());
        _scrollBar.setBlockIncrement(_text.getRows());
        memoryChanged();
      }
    });

    // Update model on scroll bar change
    _scrollBar.addAdjustmentListener(e -> setAddress(e.getValue()));

    // React on mouse wheel
    addMouseWheelListener(e -> _scrollBar.setValue(_scrollBar.getValue() + e.getUnitsToScroll() * BYTES_PER_LINE));
  }

  /**
   * Set address to display.
   */
  public void setAddress(int addr) {
    _scrollBar.setValue(addr);
    memoryChanged();
  }

  /**
   * Connect component to model (a bus).
   *
   * @param bus model
   */
  public void setBus(BusDevice bus) {
    _bus = bus;
    memoryChanged();
  }

  //
  // painting
  //

  /**
   * The memory changed, so update image.
   */
  public void memoryChanged() {
    // update text
    _text.clear();
    if (_bus != null) {
      int addr = _scrollBar.getValue();
      for (int i = 0; i < _text.getRows(); i++, addr += 8) {
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
  protected void dumpLine(int addr, int row) {
    _text.setText(0, row, HexUtil.hexWordPlain(addr));

    for (int i = 0, c = 0; i < 8; i++, c++) {
      _text.setText(6 + i * 3, row, HexUtil.hexBytePlain(_bus.read(addr + i) & 0xFF));
    }

    for (int i = 0, c = 0; i < 8; i++, c++) {
      _text.setText(30 + i, row, (byte) _bus.read(addr + i));
    }
  }
}
