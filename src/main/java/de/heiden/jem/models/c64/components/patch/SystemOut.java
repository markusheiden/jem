package de.heiden.jem.models.c64.components.patch;

import de.heiden.c64dt.charset.C64Charset;
import de.heiden.jem.components.bus.BusDevice;
import de.heiden.jem.models.c64.components.cpu.CPU6510State;
import de.heiden.jem.models.c64.components.cpu.Patch;

/**
 * Replaces standard C64 "System.out" at $E716 (part of $FFD2)
 */
public class SystemOut extends Patch {
  /**
   * Charset.
   */
  private C64Charset charset = C64Charset.LOWER;

  /**
   * Constructor.
   */
  public SystemOut() {
    super(0xE716);
  }

  @Override
  protected int execute(CPU6510State state, BusDevice bus) {
//    if ("q".equals(charset.toString((byte) state.A))) {
//      System.out.println(">" + Integer.toHexString(state.A) + "<");
//    }
    String result;
    switch (state.A) {
      case 0x0a:
        result = "\r";
        break;
      case 0x0d:
        result = "\n";
        break;
      case 0x91:
        result = "";
        break; // cursor up -> ignore
      case 0x93:
        result = "";
        break; // clr/home -> ignore
      default:
        result = charset.toString((byte) state.A);
    }

    System.out.print(result);
    state.C = false; // success
    return replaced;
  }
}
