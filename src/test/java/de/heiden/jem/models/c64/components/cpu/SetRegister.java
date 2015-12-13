package de.heiden.jem.models.c64.components.cpu;

/**
 * Set register of CPU.
 */
public interface SetRegister {
  /**
   * Set register of CPU.
   */
  void set(CPU6510State state, int value);
}
