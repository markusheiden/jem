package de.heiden.jem.models.c64.components.sid;

import de.heiden.jem.components.bus.BusDevice;
import de.heiden.jem.components.clock.Clock;
import org.jspecify.annotations.Nullable;

/**
 * Placeholder for the SID 6581 until it is emulated.
 * <p>
 * Emulates just the data bus of the SID: Its registers are write only, except the paddle, oscillator 3
 * and envelope 3 registers. Reading a write only register returns the byte, which has been on the data bus last.
 * This byte fades to 0 after some time.
 * <p>
 * Does not write through to the RAM below, because programs may store code or data there.
 */
public class SIDStub implements BusDevice {
    /**
     * Address mask: The registers are mirrored every $20 bytes.
     */
    private static final int MASK = 0x1F;

    /**
     * Readable registers: Paddle X, paddle Y, oscillator 3 and envelope 3.
     */
    private static final int POT_X = 0x19;
    private static final int POT_Y = 0x1A;
    private static final int OSC_3 = 0x1B;
    private static final int ENV_3 = 0x1C;

    /**
     * Cycles, after which the byte on the data bus of a SID 6581 has faded to 0. Value of reSID.
     */
    private static final int BUS_TTL = 0x1D00;

    /**
     * Clock, null if the byte on the data bus does not fade.
     */
    private final @Nullable Clock clock;

    /**
     * Byte, which has been on the data bus last.
     */
    private int bus = 0;

    /**
     * Tick, at which the byte on the data bus has faded.
     */
    private long busFaded = 0;

    /**
     * Constructor. The byte on the data bus does not fade.
     */
    public SIDStub() {
        this.clock = null;
    }

    /**
     * Constructor.
     *
     * @param clock
     *         Clock.
     */
    public SIDStub(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void write(int value, int address) {
        bus(value);
    }

    @Override
    public int read(int address) {
        return switch (address & MASK) {
            // No paddles connected.
            case POT_X, POT_Y -> bus(0xFF);
            // No sound.
            case OSC_3, ENV_3 -> bus(0x00);
            default -> clock == null || clock.getTick() < busFaded ? bus : 0x00;
        };
    }

    /**
     * Put a byte on the data bus.
     */
    private int bus(int value) {
        bus = value;
        if (clock != null) {
            busFaded = clock.getTick() + BUS_TTL;
        }
        return value;
    }
}
