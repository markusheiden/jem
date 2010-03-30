package de.heiden.jem.models.c64.components;

/**
 * 8 Bit-ROM.
 */
public class ROM extends AbstractMemory
{
  /**
   * Constructor.
   *
   * @param content ROM content
   * @require content.length >= 0 && content.length <= 0x10000
   */
  public ROM(byte[] content)
  {
    super(content);
  }

  /**
   * Write byte to ROM.
   *
   * @param value byte to write
   * @param address address to write byte to
   * @require value >= 0x00 && value < 0x100
   * @require address >= base() && address - base() < size()
   */
  public final void write(int value, int address)
  {
    assert false : "Write to ROM not possible";
  }

  /**
   * Patch byte in ROM.
   *
   * @param value byte to write
   * @param address address to write byte to
   * @require value >= 0x00 && value < 0x100
   * @require address >= base() && address - base() < size()
   */
  public void patch(int value, int address)
  {
    assert value >= 0 && value < 0x100 : "value >= 0 && value < 0x100";

    _memory[address & _mask] = (byte) value;
  }

  /**
   * Read byte from ROM.
   *
   * @param address address to read byte from
   * @ensure result >= 0x00 && result < 0x100
   */
  public final int read(int address)
  {
    int result = _memory[address & _mask] & 0xFF;
    // assert  result >= 0x00 && result < 0x100: "result >= 0x00 && result < 0x100";
    return result;
  }
}
