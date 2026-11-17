package de.heiden.jem.models.c64.components.vic;

import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.models.c64.components.memory.ColorRAM;

/**
 * VIC 6567R8 (NTSC).
 */
public class VIC6567R8NTSC extends VIC {
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
    public VIC6567R8NTSC(Clock clock, VICBus bus, ColorRAM colorRam) {
        super(clock, bus, colorRam,
                65, 263, 13, 40, 489, 396, 512, 0x19C);

        // cyclesPerLine = 65;
        // linesPerScreen = 263;
        // firstVBlank = 13;
        // lastVBlank = 40;
        // firstVisibleX = (489 + 0x0007) & 0x01F8;
        // lastVisibleX = 396 & 0x01F8;
        // lastX = 512;
    }
}
