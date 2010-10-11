package de.heiden.jem.models.c64.components.memory;

/**
 * 4 Bit-Color RAM.
 */
public class ColorRAM extends AbstractMemory
{
  /**
   * Constructor.
   *
   * @param size size in bytes
   * @require size >= 0 && size <= 0x10000
   */
  public ColorRAM(int size)
  {
    super(size);
  }

  /**
   * Constructor.
   *
   * @param content ROM content
   * @require content.length >= 0 && content.length <= 0x10000
   */
  public ColorRAM(byte[] content)
  {
    super(content);
  }

  /**
   * Write byte to RAM.
   *
   * @param value byte to write
   * @param address address to write byte to
   * @require value >= 0 && value < 0x100
   */
  public final void write(int value, int address)
  {
    assert value >= 0 && value < 0x100 : "value >= 0 && value < 0x100";

    _memory[address & _mask] = (byte) value;
  }

  /**
   * Read byte from RAM.
   *
   * @param address address to read byte from
   * @ensure result >= 0 && result < 0x100
   */
  public final int read(int address)
  {
    int result = _memory[address & _mask] & 0x0F; // TODO how to set high nibble?
    // assert result >= 0 && result < 0x100: "result >= 0 && result < 0x100";
    return result;
  }
}
