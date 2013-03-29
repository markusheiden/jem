package de.heiden.jem.models.c64.util;

import de.heiden.jem.models.c64.components.memory.ROM;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Loader for ROM contents.
 */
public class ROMLoader {
  /**
   * Basic ROM.
   *
   * @param filename filename of rom image.
   * @ensure result != null
   */
  public static ROM basic(String filename) throws Exception {
    byte[] content = load(0x2000, filename);
    ROM result = new ROM(content);

    assert result != null : "result != null";
    return result;
  }

  /**
   * Kernel ROM.
   *
   * @param filename filename of rom image.
   * @ensure result != null
   */
  public static ROM kernel(String filename) throws Exception {
    byte[] content = load(0x2000, filename);
    ROM result = new ROM(content);

    assert result != null : "result != null";
    return result;
  }

  /**
   * Charset ROM.
   *
   * @param filename filename of rom image.
   * @ensure result != null
   */
  public static ROM character(String filename) throws Exception {
    byte[] content = load(0x1000, filename);
    ROM result = new ROM(content); // TODO correct?

    assert result != null : "result != null";
    return result;
  }

  /**
   * Charset ROM.
   *
   * @param filename filename of rom image.
   * @ensure result != null
   */
  public static ROM pla(String filename) throws Exception {
    byte[] content = load(0x1000, filename);
    ROM result = new ROM(content);

    assert result != null : "result != null";
    return result;
  }

  /**
   * Load ROM content from File
   *
   * @param length of expected content
   * @param filename filename of content
   * @exception Exception
   */
  protected static byte[] load(int length, String filename) throws Exception {
    try {
      InputStream stream = ROMLoader.class.getResourceAsStream(filename);
      byte[] result = new byte[length];
      int size = stream.read(result);
      stream.close();

      if (size != length) {
        throw new Exception("ROM image '" + filename + "' is too short");
      }

      assert result != null : "result != null";
      return result;
    } catch (FileNotFoundException e) {
      throw new Exception("ROM image '" + filename + "' not found", e);
    } catch (IOException e) {
      throw new Exception("Unable to read ROM image '" + filename + "'", e);
    }
  }

  //
  // public constants
  //

  public static final String DEFAULT_BASIC = "/roms/basic/901226-01.bin";
  public static final String DEFAULT_KERNEL = "/roms/kernel/901227-03.bin";
  public static final String DEFAULT_CHARACTER = "/roms/character/901225-01.bin";
  public static final String DEFAULT_PLA = "/roms/pla/pla.bin";
}
