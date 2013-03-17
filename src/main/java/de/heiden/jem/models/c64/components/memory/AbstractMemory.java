package de.heiden.jem.models.c64.components.memory;

import de.heiden.jem.components.bus.BusDevice;

/**
 * Byte memory based bus device.
 */
public abstract class AbstractMemory implements BusDevice {
  /**
   * Address mask.
   */
  protected final int _mask;

  /**
   * Memory content.
   */
  protected final int[] _memory;

  /**
   * Constructor.
   *
   * @param size size in bytes
   * @require size >= 0 && size <= 0x10000
   */
  protected AbstractMemory(int size) {
    assert size >= 0 && size <= 0x10000;
    // assert that size is a power of 2
    assert Integer.bitCount(size) == 1;

    _mask = size - 1;
    _memory = new int[size];
  }

  /**
   * Constructor.
   *
   * @param content ROM content
   * @require content.length >= 0 && content.length <= 0x10000
   */
  protected AbstractMemory(byte[] content) {
    this(content.length);

    for (int i = 0; i < content.length; i++) {
      _memory[i] = content[i] & 0xFF;
    }
  }

  /**
   * Address mask.
   *
   * @ensure result >= 0 && result < 0x10000
   */
  public final int mask() {
    assert _mask >= 0 && _mask < 0x10000 : "result >= 0 && result < 0x10000";
    return _mask;
  }
}
