package de.heiden.jem.models.c64.components;

import de.heiden.jem.models.c64.components.cia.TodInput;
import de.heiden.jem.models.c64.components.vic.VicModel;

/**
 * Video system of a C64: Defines its VIC, the frequency of its CPU clock and of its power line.
 */
public enum VideoSystem {
    /**
     * PAL-B: VIC 6569 with 63 cycles per line and 312 lines, power line 50 Hz.
     */
    PAL(VicModel.MOS6569, TodInput.PAL),

    /**
     * NTSC-M: VIC 6567R8 with 65 cycles per line and 263 lines, power line 60 Hz.
     */
    NTSC(VicModel.MOS6567R8, TodInput.NTSC),

    /**
     * NTSC-M of early C64s: VIC 6567R56A with 64 cycles per line and 262 lines, power line 60 Hz.
     */
    NTSC_OLD(VicModel.MOS6567R56A, TodInput.NTSC);

    /**
     * System property to select the video system of the emulator: "pal" (default), "ntsc" or "ntsc_old".
     */
    public static final String PROPERTY = "jem.video";

    /**
     * Model of the VIC.
     */
    private final VicModel vicModel;

    /**
     * TOD input of the CIAs.
     */
    private final TodInput todInput;

    VideoSystem(VicModel vicModel, TodInput todInput) {
        this.vicModel = vicModel;
        this.todInput = todInput;
    }

    /**
     * Video system selected by the system property {@link #PROPERTY}, PAL by default.
     */
    public static VideoSystem selected() {
        return valueOf(System.getProperty(PROPERTY, PAL.name()).toUpperCase());
    }

    /**
     * Model of the VIC.
     */
    public VicModel vicModel() {
        return vicModel;
    }

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
