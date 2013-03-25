package de.heiden.jem.models.c64.components.patch;

import de.heiden.jem.components.bus.BusDevice;
import de.heiden.jem.models.c64.components.cpu.CPU6510State;
import de.heiden.jem.models.c64.components.cpu.Patch;

/**
 * Replaces standard C64 "System.out" at $E716 (part of $FFD2)
 */
public class SystemOut extends Patch {
  /**
   * Constructor.
   */
  public SystemOut() {
    super(0xE716);
  }

  @Override
  protected int execute(CPU6510State state, BusDevice bus) {
    System.out.print(Character.valueOf((char) state.A));
    state.C = false; // success
    return replaced;
  }
}
