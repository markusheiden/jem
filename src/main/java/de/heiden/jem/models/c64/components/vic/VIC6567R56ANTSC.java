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
   * @param clock system clock (1 MHz).
   * @param bus vic memory bus.
   * @param colorRam color ram.
   * @require clock != null
   * @require bus != null
   * @require colorRam != null
   */
  public VIC6567R56ANTSC(Clock clock, VICBus bus, ColorRAM colorRam) {
    super(clock, bus, colorRam,
      64, 262, 13, 40, 488, 388, 512);

    // 6569R56A
    // _cyclesPerLine = 64;
    // _linesPerScreen = 262;
    // _firstVBlank = 13;
    // _lastVBlank = 40;
    // _firstVisibleX = (488 + 0x0007) & 0x01F8;
    // _lastVisibleX = 388 & 0x01F8;
    // _lastX = 512;
  }
}
