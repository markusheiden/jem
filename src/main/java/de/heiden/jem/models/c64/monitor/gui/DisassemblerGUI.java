package de.heiden.jem.models.c64.monitor.gui;

import de.heiden.c64dt.assembler.Disassembler;
import de.heiden.c64dt.assembler.ICodeBuffer;
import de.heiden.c64dt.gui.JC64List;
import de.heiden.jem.components.bus.BusDevice;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.StringWriter;

/**
 * GUI for showing the disassembled code.
 */
public class DisassemblerGUI extends JPanel {
  private static final int BYTES_PER_LINE = 1;

  private final JC64List _text;
  private final JScrollBar _scrollBar;

  private final Disassembler _disassembler;

  private BusDevice _bus;

  public DisassemblerGUI() {
    setLayout(new BorderLayout());

    _text = new JC64List(26, 25, 2, true);
    _text.setMinimumSize(new Dimension(_text.getWidth(), 0));
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

    _text.addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        if (e.getFirstIndex() == 0) {
        }
      }
    });

    // Update scroll bar on containing component change
    _text.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        _scrollBar.setVisibleAmount(25);
        _scrollBar.setBlockIncrement(25);
        codeChanged();
      }
    });

    // Update model on scroll bar change
    _scrollBar.addAdjustmentListener(new AdjustmentListener() {
      @Override
      public void adjustmentValueChanged(AdjustmentEvent e) {
        setAddress(e.getValue());
      }
    });

    // React on mouse wheel
    addMouseWheelListener(new MouseWheelListener() {
      @Override
      public void mouseWheelMoved(MouseWheelEvent e) {
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
  public void setAddress(int addr) {
    _scrollBar.setValue(addr);
    codeChanged();
  }

  /**
   * Connect component to model (a bus).
   *
   * @param bus model
   */
  public void setBus(BusDevice bus) {
    _bus = bus;
    codeChanged();
  }

  //
  // painting
  //

  /**
   * The memory changed, so update image.
   */
  public void codeChanged() {
    if (_bus == null) {
      return;
    }

    // update text
    try {
      DefaultListModel<String> model = new DefaultListModel<>();
      ICodeBuffer code = new BusCodeBuffer(_scrollBar.getValue(), _bus);
      model.addElement("TOP");
      for (int i = 0; i < _text.getRows() && code.has(1); i++) {
        StringWriter output = new StringWriter(64);
        _disassembler.disassemble(code, output);
        model.addElement(output.toString());
      }
      model.addElement("BOTTOM");
      _text.setModel(model);

    } catch (IOException e) {
      // ignore
    }

    repaint();
  }
}
