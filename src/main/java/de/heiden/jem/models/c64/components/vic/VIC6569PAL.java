package de.heiden.jem.models.c64.components.vic;

import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.models.c64.components.memory.ColorRAM;

/**
 * VIC 6569 (PAL).
 */
public class VIC6569PAL extends VIC {
    /**
     * Constructor.
     *
     * @param clock
     *         system clock (1 MHz).
     * @param bus
     *         vic memory bus.
     * @param colorRam
     *         color ram.
     * @require clock != null
     * @require bus != null
     * @require colorRam != null
     */
    public VIC6569PAL(Clock clock, VICBus bus, ColorRAM colorRam) {
        super(clock, bus, colorRam,
                63, 312, 300, 15, 480, 380, 504);

        // 6569
        // _cyclesPerLine = 63;
        // _linesPerScreen = 312;
        // _firstVBlank = 300;
        // _lastVBlank = 15;
        // _firstVisibleX = (480 + 0x0007) & 0x01F8;
        // _lastVisibleX = 380 & 0x01F8;
        // _lastX = 504;
    }
}
