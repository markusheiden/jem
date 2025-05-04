package de.heiden.jem.models.c64.components.cpu;

/**
 * Set a register of the CPU.
 */
public interface SetRegister {
  /**
   * Set a register of the CPU.
   */
  void set(CPU6510State state, int value);
}
