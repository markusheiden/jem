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

  private final JC64TextArea text;
  private final JScrollBar scrollBar;

  private BusDevice bus;

  /**
   * Constructor.
   */
  public MemDumpGUI() {
    setLayout(new BorderLayout());

    text = new JC64TextArea(39, 25, 2, true);
    add(text, BorderLayout.CENTER);

    scrollBar = new JScrollBar(JScrollBar.VERTICAL);
    scrollBar.setMinimum(0);
    scrollBar.setMaximum(0x10000 - BYTES_PER_LINE);
    scrollBar.setVisibleAmount(0x0100);
    scrollBar.setBlockIncrement(0x0100);
    scrollBar.setUnitIncrement(BYTES_PER_LINE);
    scrollBar.setValue(0);
    add(scrollBar, BorderLayout.EAST);

    // Update scroll bar on containing component change.
    text.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        scrollBar.setVisibleAmount(text.getRows());
        scrollBar.setBlockIncrement(text.getRows());
        memoryChanged();
      }
    });

    // Update model on scroll bar change.
    scrollBar.addAdjustmentListener(e -> setAddress(e.getValue()));

    // React on the mouse wheel.
    addMouseWheelListener(e -> scrollBar.setValue(scrollBar.getValue() + e.getUnitsToScroll() * BYTES_PER_LINE));
  }

  /**
   * Set the address to display.
   */
  public void setAddress(int addr) {
    scrollBar.setValue(addr);
    memoryChanged();
  }

  /**
   * Connect component to model (a bus).
   *
   * @param bus model
   */
  public void setBus(BusDevice bus) {
    this.bus = bus;
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
    text.clear();
    if (bus != null) {
      int addr = scrollBar.getValue();
      for (int i = 0; i < text.getRows(); i++, addr += 8) {
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
    text.setText(0, row, HexUtil.hexWordPlain(addr));

    for (int i = 0, c = 0; i < 8; i++, c++) {
      text.setText(6 + i * 3, row, HexUtil.hexBytePlain(bus.read(addr + i) & 0xFF));
    }

    for (int i = 0, c = 0; i < 8; i++, c++) {
      text.setText(30 + i, row, (byte) bus.read(addr + i));
    }
  }
}
