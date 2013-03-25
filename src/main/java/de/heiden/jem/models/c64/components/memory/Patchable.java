package de.heiden.jem.models.c64.components.memory;

/**
 * Patchable device.
 */
public interface Patchable {
  /**
   * Patch byte in ROM.
   *
   * @param value byte to write
   * @param address address to write byte to
   * @require value >= 0x00 && value < 0x100
   */
  public void patch(int value, int address);
}
