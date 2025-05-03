package de.heiden.jem.models.c64.components.patch;

import de.heiden.jem.components.bus.BusDevice;
import de.heiden.jem.models.c64.components.cpu.CPU6510State;
import de.heiden.jem.models.c64.components.cpu.Patch;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Replaces standard C64 "System.out" at $E716 (part of $FFD2)
 */
public class SystemOut extends Patch {
  /**
   * Stream to write output to.
   */
  private OutputStream stream;

  /**
   * Constructor.
   */
  public SystemOut() {
    super(0xE716);
  }

  /**
   * Set stream to write output to.
   *
   * @param stream Stream
   */
  public void setStream(OutputStream stream) {
    this.stream = stream;
  }

  @Override
  protected int execute(CPU6510State state, BusDevice bus) {
    try {
      if (stream != null) {
        stream.write(state.A);
      }
    } catch (IOException e) {
      // Ignore.
    }

    return replaced;
  }
}
