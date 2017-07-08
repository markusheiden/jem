package de.heiden.jem.components.bus;

import de.heiden.c64dt.bytes.ByteUtil;

/**
 * Wrapper to enable word access to bus.
 * Uses little endian.
 */
public class WordBus extends BusDeviceAdapter {
  /**
   * Constructor.
   *
   * @param bus Bus to delegate to
   * @require bus != null
   */
  public WordBus(BusDevice bus) {
    super(bus);
  }

  /**
   * Read word from bus.
   *
   * @param addr Address to read from
   */
  public int readWord(int addr) {
    return ByteUtil.toWord(read(addr), read(addr + 1));
  }

  /**
   * Write word to bus.
   *
   * @param addr Address to read from
   */
  public void writeWord(int addr, int word) {
    write(ByteUtil.lo(word), addr);
    write(ByteUtil.hi(word), addr + 1);
  }
}
