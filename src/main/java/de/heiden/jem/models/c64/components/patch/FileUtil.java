package de.heiden.jem.models.c64.components.patch;

import de.heiden.jem.components.bus.BusDevice;
import jakarta.annotation.Nonnull;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import static de.heiden.c64dt.bytes.ByteUtil.toWord;
import static java.nio.file.Files.newInputStream;

/**
 * Some utils to read C64 files.
 */
public class FileUtil {
  /**
   * Read the file and write it to memory.
   * The first word in the file is interpreted as the start address.
   *
   * @param file File.
   * @param bus Bus with memory.
   * @return End address.
   */
  public static int read(@Nonnull Path file, @Nonnull BusDevice bus) throws IOException {
    return read(newInputStream(file), bus);
  }

  /**
   * Read the file and write it to memory.
   * The first word in the file is interpreted as the start address.
   *
   * @param file File.
   * @param bus Bus with memory.
   * @return End address.
   */
  public static int read(@Nonnull InputStream file, @Nonnull BusDevice bus) throws IOException {
    try (var is = new BufferedInputStream(file)) {
      int addr = toWord(is.read(), is.read());
      return readAll(is, addr, bus);
    }
  }

  /**
   * Read the file and write it to memory.
   * Ignores the first word (start address).
   *
   * @param file File.
   * @param addr Start address.
   * @param bus Bus with memory.
   * @return End address.
   */
  public static int read(@Nonnull Path file, int addr, @Nonnull BusDevice bus) throws IOException {
    return read(newInputStream(file), addr, bus);
  }

  /**
   * Read the file and write it to memory.
   * Ignores the first word (start address).
   *
   * @param file File.
   * @param addr Start address.
   * @param bus Bus with memory.
   * @return End address.
   */
  public static int read(@Nonnull InputStream file, int addr, @Nonnull BusDevice bus) throws IOException {
    try (var is = new BufferedInputStream(file)) {
      // Skip address.
      is.read();
      is.read();
      return readAll(is, addr, bus);
    }
  }

  private static int readAll(InputStream is, int addr, BusDevice bus) throws IOException {
    for (int b; (b = is.read()) >= 0; addr++) {
      bus.write(b, addr);
    }
    return addr;
  }
}
