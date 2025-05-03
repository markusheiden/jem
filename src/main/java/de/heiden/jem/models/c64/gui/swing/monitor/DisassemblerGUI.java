package de.heiden.jem.models.c64.gui.swing.monitor;

import de.heiden.c64dt.assembler.Disassembler;
import de.heiden.c64dt.gui.swing.JC64List;
import de.heiden.jem.components.bus.BusDevice;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;
import java.io.StringWriter;

/**
 * GUI for showing the disassembled code.
 */
public class DisassemblerGUI extends JPanel {
  private static final int BYTES_PER_LINE = 1;

  private final JC64List text;
  private final JScrollBar scrollBar;

  private final Disassembler disassembler;

  private BusDevice bus;

  public DisassemblerGUI() {
    setLayout(new BorderLayout());

    text = new JC64List(26, 25, 2, true);
    text.setMinimumSize(new Dimension(text.getWidth(), 0));
    add(text, BorderLayout.CENTER);

    scrollBar = new JScrollBar(JScrollBar.VERTICAL);
    scrollBar.setMinimum(0);
    scrollBar.setMaximum(0x10000 - BYTES_PER_LINE);
    scrollBar.setVisibleAmount(25);
    scrollBar.setBlockIncrement(25);
    scrollBar.setUnitIncrement(BYTES_PER_LINE);
    scrollBar.setValue(0);
    add(scrollBar, BorderLayout.EAST);

    disassembler = new Disassembler();

    text.addListSelectionListener(e -> {
      if (e.getFirstIndex() == 0) {
      }
    });

    // Update scroll bar on containing component change.
    text.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        scrollBar.setVisibleAmount(25);
        scrollBar.setBlockIncrement(25);
        codeChanged();
      }
    });

    // Update model on scroll bar change.
    scrollBar.addAdjustmentListener(e -> setAddress(e.getValue()));

    // React on the mouse wheel.
    addMouseWheelListener(e -> scrollBar.setValue(scrollBar.getValue() + e.getUnitsToScroll()));
  }

  //
  //
  //

  /**
   * Set the address to display.
   */
  public void setAddress(int addr) {
    scrollBar.setValue(addr);
    codeChanged();
  }

  /**
   * Connect component to model (a bus).
   *
   * @param bus model
   */
  public void setBus(BusDevice bus) {
    this.bus = bus;
    codeChanged();
  }

  //
  // painting
  //

  /**
   * The memory changed, so update image.
   */
  public void codeChanged() {
    if (bus == null) {
      return;
    }

    // update text
    try {
      var model = new DefaultListModel<String>();
      var code = new BusCodeBuffer(scrollBar.getValue(), bus);
      model.addElement("TOP");
      for (int i = 0; i < text.getRows() && code.hasMore(); i++) {
        var output = new StringWriter(64);
        disassembler.disassemble(code, output);
        model.addElement(output.toString());
      }
      model.addElement("BOTTOM");
      text.setModel(model);

    } catch (IOException e) {
      // ignore
    }

    repaint();
  }
}
