package de.heiden.jem.models.c64.components.memory;

import de.heiden.jem.components.bus.BusDevice;

/**
 * Byte memory based bus device.
 */
public abstract class AbstractMemory implements BusDevice
{
  /**
   * Address mask.
   */
  protected final int _mask;

  /**
   * Memory content.
   */
  protected final byte[] _memory;

  /**
   * Constructor.
   *
   * @param size size in bytes
   * @require size >= 0 && size <= 0x10000
   */
  protected AbstractMemory(int size)
  {
    assert size >= 0 && size <= 0x10000;
    // TODO 2010-10-11 mh: check that size is 2^something!

    _mask = size - 1;
    _memory = new byte[size];
  }

  /**
   * Constructor.
   *
   * @param content ROM content
   * @require content.length >= 0 && content.length <= 0x10000
   */
  protected AbstractMemory(byte[] content)
  {
    assert content.length >= 0 && content.length <= 0x10000 : "content.length >= 0 && content.length <= 0x10000";
    // TODO 2010-10-11 mh: check that content.length is 2^something!

    _mask = content.length - 1;
    _memory = content;
  }

  /**
   * Address mask.
   *
   * @ensure result >= 0 && result < 0x10000
   */
  public final int mask()
  {
    assert _mask >= 0 && _mask < 0x10000 : "result >= 0 && result < 0x10000";
    return _mask;
  }
}
