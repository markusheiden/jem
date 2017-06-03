package de.heiden.jem.models.c64.components.patch;

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

    return read(new FileInputStream(file), bus);
  }

  /**
   * Read file and write it to memory.
   * The first word in the file is interpreted as start address.
   *
   * @param file File
   * @param bus Bus with memory
   * @return End address
   */
  public static int read(InputStream file, BusDevice bus) throws IOException {
    assert file != null : "file != null";
    assert bus != null : "bus != null";

    try (InputStream is = new BufferedInputStream(file)) {
      int addr = ByteUtil.toWord(is.read(), is.read());
      for (int b; (b = is.read()) >= 0; addr++) {
        bus.write(b, addr);
      }

      return addr;
    }
  }

  /**
   * Read file and write it to memory.
   * Ignores the first word.
   *
   * @param file File
   * @param addr Start address
   * @param bus Bus with memory
   * @return End address
   */
  public static int read(File file, int addr, BusDevice bus) throws IOException {
    return read(new FileInputStream(file), addr, bus);
  }

  /**
   * Read file and write it to memory.
   * Ignores the first word.
   *
   * @param file File
   * @param addr Start address
   * @param bus Bus with memory
   * @return End address
   */
  public static int read(InputStream file, int addr, BusDevice bus) throws IOException {
    assert file != null : "file != null";
    assert bus != null : "bus != null";

    try (InputStream is = new BufferedInputStream(file)) {
      is.read();
      is.read();
      for (int b; (b = is.read()) >= 0; addr++) {
        bus.write(b, addr);
      }

      return addr;
    }
  }
}
