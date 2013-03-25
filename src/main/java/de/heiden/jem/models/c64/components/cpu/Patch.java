package de.heiden.jem.models.c64.components.cpu;

import de.heiden.jem.components.bus.BusDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Patch for escaping emulation.
 */
public abstract class Patch {
  /**
   * Logger.
   */
  protected final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * Address to patch.
   */
  private final int addr;

  /**
   * Constructor.
   *
   * @param addr Address to patch
   */
  protected Patch(int addr) {
    this.addr = addr;
  }

  /**
   * Address to patch.
   */
  public int getAddress() {
    return addr;
  }

  /**
   * Execute patch.
   *
   * @param state CPU state
   * @param bus C64 bus
   * @return Execute rts after execution of patch
   */
  protected abstract boolean execute(CPU6510State state, BusDevice bus);
}
