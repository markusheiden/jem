package de.heiden.jem.models.c64.components.vic;

/**
 * Model of the VIC: Defines its timing.
 */
public enum VicModel {
    /**
     * VIC 6569 (PAL).
     */
    MOS6569(63, 312, 300, 15, 480, 380, 504, 0x194, 58, 0xD1, false),

    /**
     * VIC 6567R8 (NTSC).
     */
    MOS6567R8(65, 263, 13, 40, 489, 396, 512, 0x19C, 59, 0xD5, false),

    /**
     * VIC 6567R56A (NTSC of early C64s).
     */
    MOS6567R56A(64, 262, 13, 40, 488, 388, 512, 0x19C, 59, 0xD1, true);

    /**
     * System clock cycles per raster line.
     */
    final int cyclesPerLine;

    /**
     * Raster lines per screen.
     */
    final int linesPerScreen;

    /**
     * First and last raster line of the vertical blanking.
     */
    final int firstVBlank;
    final int lastVBlank;

    /**
     * First and last visible X coordinate.
     */
    final int firstVisibleX;
    final int lastVisibleX;

    /**
     * Number of X coordinates of a raster line.
     */
    final int lastX;

    /**
     * X coordinate of the first pixel of cycle 1.
     */
    final int firstX;

    /**
     * First of the 2 cycles in which the VIC reads the pointer and the data of sprite 0.
     */
    final int firstSpriteCycle;

    /**
     * X coordinate latched by the light pen, if it triggers at the start of a frame, because its input is still low.
     */
    final int lightPenXAtStartOfFrame;

    /**
     * Does the light pen request an interrupt just at the start of a frame, if its input is low?
     * That is the case for the old VICs 6569R1 and 6567R56A.
     */
    final boolean lightPenIrqAtStartOfFrameOnly;

    VicModel(int cyclesPerLine, int linesPerScreen, int firstVBlank, int lastVBlank,
            int firstVisibleX, int lastVisibleX, int lastX, int firstX,
            int firstSpriteCycle, int lightPenXAtStartOfFrame, boolean lightPenIrqAtStartOfFrameOnly) {
        this.cyclesPerLine = cyclesPerLine;
        this.linesPerScreen = linesPerScreen;
        this.firstVBlank = firstVBlank;
        this.lastVBlank = lastVBlank;
        this.firstVisibleX = firstVisibleX;
        this.lastVisibleX = lastVisibleX;
        this.lastX = lastX;
        this.firstX = firstX;
        this.firstSpriteCycle = firstSpriteCycle;
        this.lightPenXAtStartOfFrame = lightPenXAtStartOfFrame;
        this.lightPenIrqAtStartOfFrameOnly = lightPenIrqAtStartOfFrameOnly;
    }
}
