package de.heiden.jem.models.c64.gui.swing.monitor;

import de.heiden.c64dt.bytes.HexUtil;
import de.heiden.c64dt.gui.swing.JC64TextArea;
import de.heiden.jem.components.bus.BusDevice;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import static java.util.Objects.requireNonNull;

/**
 * GUI for showing a memory dump.
 */
public class MemDumpGUI extends JPanel {
    private static final int BYTES_PER_LINE = 8;

    private final JC64TextArea text;
    private final JScrollBar scrollBar;

    private @Nullable BusDevice bus;

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
     * @param bus
     *         Model.
     */
    public void setBus(@Nullable BusDevice bus) {
        this.bus = bus;
        memoryChanged();
    }

    //
    // Painting.
    //

    /**
     * The memory changed, so update image.
     */
    public void memoryChanged() {
        // Update text.
        text.clear();
        if (bus != null) {
            var addr = scrollBar.getValue();
            for (var i = 0; i < text.getRows(); i++, addr += 8) {
                dumpLine(addr, i);
            }
        }

        repaint();
    }

    /**
     * Dump line.
     *
     * @param addr
     *         Address to dump.
     * @param row
     *         Row to draw dump to.
     */
    protected void dumpLine(int addr, int row) {
        var bus = requireNonNull(this.bus, "Bus");
        text.setText(0, row, HexUtil.hexWordPlain(addr));

        for (int i = 0, c = 0; i < 8; i++, c++) {
            text.setText(6 + i * 3, row, HexUtil.hexBytePlain(bus.read(addr + i) & 0xFF));
        }

        for (int i = 0, c = 0; i < 8; i++, c++) {
            text.setText(30 + i, row, (byte) bus.read(addr + i));
        }
    }
}
