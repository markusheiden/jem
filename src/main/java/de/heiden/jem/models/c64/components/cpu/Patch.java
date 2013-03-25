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
   * Replaced opcode.
   */
  protected int replaced;

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
   * @return Opcode to execute after execution of patch or -1
   */
  protected abstract int execute(CPU6510State state, BusDevice bus);
}
