package de.heiden.jem.models.c64.components.patch;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import de.heiden.c64dt.util.ByteUtil;
import de.heiden.jem.components.bus.BusDevice;
import de.heiden.jem.components.bus.WordBus;
import de.heiden.jem.models.c64.components.cpu.CPU6510State;
import de.heiden.jem.models.c64.components.cpu.Patch;
import de.heiden.jem.models.c64.util.FileUtil;
import de.heiden.jem.models.c64.util.StringUtil;

/**
 * Replaces standard C64 load routine at $F4A5.
 * Intercepts load routine at $F4C4 directly after printing "SEARCHING FOR".
 * Loads files from a given directory.
 */
public class LoadFromDirectory extends Patch {
  /**
   * Base package to load files from.
   */
  private final String basePackage;

  /**
   * Base directory to load files from.
   */
  private final Path baseDir;

  /**
   * Constructor.
   *
   * @param basePackage Base package to load files from
   */
  public LoadFromDirectory(String basePackage) {
    super(0xF4C4);

    this.basePackage = basePackage;
    this.baseDir = null;
  }

  /**
   * Constructor.
   *
   * @param baseDirectory Base directory to load files from
   */
  public LoadFromDirectory(Path baseDirectory) {
    super(0xF4C4);

    this.basePackage = null;
    this.baseDir = baseDirectory;
  }

  @Override
  protected int execute(CPU6510State state, BusDevice bus) {
    // Read filename from ($BB), length ($B7)
    WordBus wordBus = new WordBus(bus);
    String filename = StringUtil.read(bus, wordBus.readWord(0xBB), bus.read(0xB7));
    filename = filename.toLowerCase() + ".prg";

    try {
      InputStream file = baseDir != null ?
        Files.newInputStream(baseDir.resolve(filename)) :
        getClass().getResourceAsStream(basePackage + "/" + filename);
      assert file != null : "file " + filename + " exists";

      int endAddress = bus.read(0xB9) == 0 ?
        FileUtil.read(file, wordBus.readWord(0xC3), bus) :
        FileUtil.read(file, bus);

      wordBus.writeWord(0xAE, endAddress);

      state.C = false; // OK
      state.X = ByteUtil.lo(endAddress);
      state.Y = ByteUtil.hi(endAddress);

    } catch (FileNotFoundException e) {
      state.PC = 0xF704;
      return -1;

    } catch (IOException e) {
      logger.error("Failed to load {}", filename, e);
    }

    return RTS;
  }
}
