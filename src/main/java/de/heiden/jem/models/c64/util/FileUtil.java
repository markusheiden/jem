package de.heiden.jem.models.c64.util;

import de.heiden.c64dt.util.ByteUtil;
import de.heiden.jem.components.bus.BusDevice;

import java.io.*;

/**
 * Some utils to read C64 files.
 */
public class FileUtil {
  /**
   * Read file and write it to memory.
   * The first word in the file is interpreted as start address.
   *
   * @param file File
   * @param bus Bus with memory
   * @return End address
   */
  public static int read(File file, BusDevice bus) throws IOException {
    assert file != null : "file != null";
    assert bus != null : "bus != null";

    try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
      int addr = ByteUtil.toWord(is.read(), is.read());
      for (int b; (b = is.read()) >= 0; addr++) {
        bus.write(b, addr);
      }

      return addr;
    }
  }
}
