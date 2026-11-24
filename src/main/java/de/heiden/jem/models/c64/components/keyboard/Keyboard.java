package de.heiden.jem.models.c64.components.keyboard;

import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.components.clock.ClockEvent;
import de.heiden.jem.components.ports.InputOutputPort;
import de.heiden.jem.components.ports.OutputPort;
import de.heiden.jem.components.ports.OutputPortImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keyboard.
 * <p>
 * Keys are pressed by the GUI thread, but the ports are driven by the C64 thread only,
 * because the ports are not thread safe.
 * <p>
 * TODO Mapping of @, :, /, arrow up, ;, *, pound, commodore, run stop, arrow left.
 * TODO C64 like mapping of -, +, home, shift lock, control?
 */
public class Keyboard extends ClockEvent implements IKeyboard {
    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Ticks between port updates: One PAL raster line.
     */
    private static final int INTERVAL = 63;

    /**
     * Clock.
     */
    private final Clock clock;

    /**
     * Ports for key matrix.
     */
    private final InputOutputPort port0;
    private final InputOutputPort port1;

    private final OutputPortImpl matrixPort0;
    private final OutputPortImpl matrixPort1;
    private final OutputPortImpl nmi;

    /**
     * C64 key matrix, as currently driven to the ports.
     */
    private final int[] matrix;

    /**
     * Pressed keys: Row i in bits 8*i to 8*i+7.
     * Written by the GUI thread, read by the C64 thread.
     */
    private volatile long pressed = 0;

    /**
     * Pressed keys, as currently driven to the ports.
     */
    private long driven = 0;

    /**
     * Is restore pressed?
     * Written by the GUI thread, read by the C64 thread.
     */
    private volatile boolean restore = false;

    /**
     * Is restore pressed, as currently driven to the NMI output?
     */
    private boolean restoreDriven = false;

    /**
     * Constructor.
     *
     * @param clock
     *         Clock.
     * @param port0
     *         Port for rows.
     * @param port1
     *         Port for columns.
     */
    public Keyboard(Clock clock, InputOutputPort port0, InputOutputPort port1) {
        super("Keyboard");

        this.clock = clock;

        // Connect to input ports.
        this.port0 = port0;
        this.port0.addOutputPortListener((value, mask) -> updatePorts());
        this.port1 = port1;
        this.port1.addOutputPortListener((value, mask) -> updatePorts());

        // Connect matrix ports to input ports.
        matrixPort0 = new OutputPortImpl();
        this.port0.connect(matrixPort0);
        matrixPort1 = new OutputPortImpl();
        this.port1.connect(matrixPort1);
        nmi = new OutputPortImpl();
        nmi.setOutputMask(0x1);

        // Init matrix.
        matrix = new int[] { 0, 0, 0, 0, 0, 0, 0, 0 };

        clock.addClockEvent(INTERVAL, this);
    }

    /**
     * NMI output.
     */
    public OutputPort getNMI() {
        return nmi;
    }

    @Override
    public void press(Key key) {
        switch (key) {
            case RESTORE -> restore = true;
            default -> pressed |= bit(key);
        }
    }

    @Override
    public void release(Key key) {
        switch (key) {
            case RESTORE -> restore = false;
            default -> pressed &= ~bit(key);
        }
    }

    /**
     * Bit of the key in {@link #pressed}.
     */
    private static long bit(Key key) {
        return 1L << (key.getRow() * 8 + key.getColumn());
    }

    @Override
    public void execute(long tick) {
        updateKeys();
        clock.addClockEvent(tick + INTERVAL, this);
    }

    /**
     * Drive the pressed keys to the ports and restore to the NMI output.
     */
    void updateKeys() {
        var restore = this.restore;
        if (restore != restoreDriven) {
            restoreDriven = restore;
            // Low active.
            nmi.setOutputData(restore ? 0x0 : 0x1);
        }

        var pressed = this.pressed;
        if (pressed != driven) {
            driven = pressed;
            for (var i = 0; i < matrix.length; i++) {
                matrix[i] = (int) (pressed >>> (i * 8)) & 0xFF;
            }
            updatePorts();
            logKeyMatrix();
        }
    }

    /**
     * Update ports from matrix.
     * <p>
     * Assuming low is stronger than hi.
     * Assuming pull-ups.
     */
    protected final void updatePorts() {
        var port1InMask = port1.outputMask();
        var port1InDataInv = 0xFF - port1.outputData();
        var port0OutMask = 0x00;
        var port0OutData = 0xFF;

        var port0InMask = port0.outputMask();
        var port0InData = port0.outputData();
        var port1OutMask = 0x00;
        var port1OutData = 0xFF;

        for (int i = 0, columnBit = 0x01; i < matrix.length; i++, columnBit <<= 1) {
            var matrix = this.matrix[i];
            var bitmask = port1InMask & matrix; // 1: Driven bits.
            if (bitmask != 0) {
                port0OutMask |= columnBit; // Bit is driven!
                if ((port1InDataInv & bitmask) != 0) {
                    port0OutData &= 0xFF - columnBit; // Save 0 bits. 0 is stronger than 1.
                }
            }

            if ((port0InMask & columnBit) != 0) {
                // Line is driven.
                port1OutMask |= matrix;
                if ((port0InData & columnBit) == 0) {
                    // Line is 0.
                    port1OutData &= 0xFF - matrix;
                }
            }
        }

        matrixPort0.setOutputData(port0OutData);
        matrixPort0.setOutputMask(port0OutMask);
        matrixPort1.setOutputData(port1OutData);
        matrixPort1.setOutputMask(port1OutMask);
    }

    /**
     * Log key matrix.
     */
    private void logKeyMatrix() {
        if (logger.isTraceEnabled()) {
            logger.trace(keyMatrixToString());
        }
    }

    @Override
    public String toString() {
        return keyMatrixToString();
    }

    /**
     * Print key matrix.
     */
    protected String keyMatrixToString() {
        var result = new StringBuilder(128);
        result.append("Keyboard:\n");

        // Column headers.
        result.append("  ");
        for (var j = 0; j < 8; j++) {
            result.append(j);
        }
        result.append("\n");
        // Column port bits.
        result.append("  ");
        var port1 = matrixPort1.outputData();
        for (int j = 0, columnBit = 0x01; j < 8; j++, columnBit <<= 1) {
            result.append((port1 & columnBit) != 0 ? "H" : "L");
        }
        result.append("\n");

        var port0 = matrixPort0.outputData();
        for (int i = 0, rowBit = 0x01; i < matrix.length; i++, rowBit <<= 1) {
            // Row header, row port bit.
            result.append(i).append((port0 & rowBit) != 0 ? "H" : "L");

            // Values.
            var row = matrix[i];
            for (int j = 0, columnBit = 0x01; j < 8; j++, columnBit <<= 1) {
                result.append((row & columnBit) != 0 ? "X" : "+");
            }
            result.append("\n");
        }

        return result.toString();
    }
}
