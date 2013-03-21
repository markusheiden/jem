package de.heiden.jem.models.c64.util;

import de.heiden.c64dt.util.ByteUtil;
import de.heiden.jem.components.bus.BusDevice;

/**
 * Utils for easy bus access.
 */
public class BusUtil {
  /**
   * Read word from bus.
   *
   * @param addr Address to read from
   * @param bus Bus
   */
  public static int readWord(int addr, BusDevice bus) {
    assert bus != null : "bus != null";

    return ByteUtil.toWord(bus.read(addr), bus.read(addr + 1));
  }

  /**
   * Write word to bus.
   *
   * @param addr Address to read from
   * @param bus Bus
   */
  public static void writeWord(int addr, int word, BusDevice bus) {
    assert bus != null : "bus != null";

    bus.write(ByteUtil.lo(word), addr);
    bus.write(ByteUtil.hi(word), addr + 1);
  }
}
