package de.heiden.jem.models.c64.components.keyboard;

import de.heiden.jem.components.ports.InputOutputPort;
import de.heiden.jem.components.ports.OutputPort;
import de.heiden.jem.components.ports.OutputPortImpl;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keyboard.
 * <p>
 * TODO mapping of @, :, /, arrow up, ;, *, pound, commodore, run stop, arrow left
 * TODO C64 like mapping of -, +, home, shift lock, control?
 */
public class Keyboard implements IKeyboard {
    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Ports for key matrix.
     */
    private final InputOutputPort port0;
    private final InputOutputPort port1;

    private final OutputPortImpl matrixPort0;
    private final OutputPortImpl matrixPort1;
    private final OutputPortImpl nmi;

    /**
     * C64 key matrix.
     */
    private final int[] matrix;

    /**
     * Constructor.
     *
     * @param port0
     *         port for rows
     * @param port1
     *         port for columns
     * @require port0 != null
     * @require port1 != null
     */
    public Keyboard(@Nonnull InputOutputPort port0, @Nonnull InputOutputPort port1) {
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
    }

    /**
     * NMI output.
     */
    public OutputPort getNMI() {
        assert nmi != null : "Postcondition: result != null";
        return nmi;
    }

    @Override
    public void press(Key key) {
        switch (key) {
            case RESTORE -> {
                // Low active.
                nmi.setOutputData(0x0);
            }
            default -> {
                matrix[key.getRow()] |= 1 << key.getColumn();
                updatePorts();
                logKeyMatrix();
            }
        }
    }

    @Override
    public void release(Key key) {
        switch (key) {
            case RESTORE -> {
                // Low active.
                nmi.setOutputData(0x1);
            }
            default -> {
                matrix[key.getRow()] &= 0xFF - (1 << key.getColumn());
                updatePorts();
                logKeyMatrix();
            }
        }
    }

    /**
     * Update ports from matrix.
     * <p>
     * Assuming low is stronger than hi.
     * Assuming pull-ups.
     */
    protected final void updatePorts() {
        int port1InMask = port1.outputMask();
        int port1InDataInv = 0xFF - port1.outputData();
        int port0OutMask = 0x00;
        int port0OutData = 0xFF;

        int port0InMask = port0.outputMask();
        int port0InData = port0.outputData();
        int port1OutMask = 0x00;
        int port1OutData = 0xFF;

        for (int i = 0, columnBit = 0x01; i < matrix.length; i++, columnBit <<= 1) {
            final int matrix = this.matrix[i];
            final int bitmask = port1InMask & matrix; // 1: driven bits
            if (bitmask != 0) {
                port0OutMask |= columnBit; // bit is driven!
                if ((port1InDataInv & bitmask) != 0) {
                    port0OutData &= 0xFF - columnBit; // save 0 bits. 0 is stronger than 1.
                }
            }

            if ((port0InMask & columnBit) != 0) {
                // line is driven
                port1OutMask |= matrix;
                if ((port0InData & columnBit) == 0) {
                    // line is 0
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
        for (int j = 0; j < 8; j++) {
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
            // Row header, row port bit
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
