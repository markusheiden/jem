package de.heiden.jem.models.c64.components;

import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.models.c64.components.cia.TodInput;
import de.heiden.jem.models.c64.components.memory.ColorRAM;
import de.heiden.jem.models.c64.components.vic.VIC;
import de.heiden.jem.models.c64.components.vic.VIC6567R56ANTSC;
import de.heiden.jem.models.c64.components.vic.VIC6567R8NTSC;
import de.heiden.jem.models.c64.components.vic.VIC6569PAL;
import de.heiden.jem.models.c64.components.vic.VICBus;

/**
 * Video system of a C64: Defines its VIC, the frequency of its CPU clock and of its power line.
 */
public enum VideoSystem {
    /**
     * PAL-B: VIC 6569 with 63 cycles per line and 312 lines, power line 50 Hz.
     */
    PAL(TodInput.PAL) {
        @Override
        public VIC createVic(Clock clock, VICBus bus, ColorRAM colorRam) {
            return new VIC6569PAL(clock, bus, colorRam);
        }
    },

    /**
     * NTSC-M: VIC 6567R8 with 65 cycles per line and 263 lines, power line 60 Hz.
     */
    NTSC(TodInput.NTSC) {
        @Override
        public VIC createVic(Clock clock, VICBus bus, ColorRAM colorRam) {
            return new VIC6567R8NTSC(clock, bus, colorRam);
        }
    },

    /**
     * NTSC-M of early C64s: VIC 6567R56A with 64 cycles per line and 262 lines, power line 60 Hz.
     */
    NTSC_OLD(TodInput.NTSC) {
        @Override
        public VIC createVic(Clock clock, VICBus bus, ColorRAM colorRam) {
            return new VIC6567R56ANTSC(clock, bus, colorRam);
        }
    };

    /**
     * System property to select the video system of the emulator: "pal" (default), "ntsc" or "ntsc_old".
     */
    public static final String PROPERTY = "jem.video";

    /**
     * TOD input of the CIAs.
     */
    private final TodInput todInput;

    VideoSystem(TodInput todInput) {
        this.todInput = todInput;
    }

    /**
     * Video system selected by the system property {@link #PROPERTY}, PAL by default.
     */
    public static VideoSystem selected() {
        return valueOf(System.getProperty(PROPERTY, PAL.name()).toUpperCase());
    }

    /**
     * Create the VIC.
     */
    public abstract VIC createVic(Clock clock, VICBus bus, ColorRAM colorRam);

    /**
     * TOD input of the CIAs: The frequency of the power line relative to the CPU clock.
     */
    public TodInput todInput() {
        return todInput;
    }

    /**
     * Frequency of the CPU clock in Hz.
     */
    public int cpuFrequency() {
        return todInput.cpuClock() / todInput.cpuDivider();
    }
}
