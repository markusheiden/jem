package de.heiden.jem.models.c64.components.vic;

import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.models.c64.components.memory.ColorRAM;

/**
 * VIC 6567R56A (NTSC).
 */
public class VIC6567R56ANTSC extends VIC {
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
    public VIC6567R56ANTSC(Clock clock, VICBus bus, ColorRAM colorRam) {
        super(clock, bus, colorRam,
                64, 262, 13, 40, 488, 388, 512, 0x19C);

        // 6569R56A
        // cyclesPerLine = 64;
        // linesPerScreen = 262;
        // firstVBlank = 13;
        // lastVBlank = 40;
        // firstVisibleX = (488 + 0x0007) & 0x01F8;
        // lastVisibleX = 388 & 0x01F8;
        // lastX = 512;
    }
}
