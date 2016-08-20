package de.heiden.jem.models.c64.util;

import de.heiden.c64dt.charset.PetSCIICharset;
import de.heiden.jem.components.bus.BusDevice;

/**
 * Util method for handling C64 strings.
 */
public class StringUtil {
  /**
   * Charset.
   */
  private static final PetSCIICharset charset = new PetSCIICharset(false);

  /**
   * Read string from bus.
   *
   * @param bus Bus.
   * @param addr Start address.
   * @param len Length of string.
   * @return String.
   */
  public static String read(BusDevice bus, int addr, int len) {
    assert bus != null : "bus != null";

    StringBuilder result = new StringBuilder(len);
    for (int i = 0; i < len; i++) {
      result.append(charset.toChar((byte) bus.read(addr + i)));
    }

    return result.toString();
  }
}
