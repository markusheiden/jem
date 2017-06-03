package de.heiden.jem.models.c64.components.patch;

import de.heiden.jem.components.bus.BusDevice;
import de.heiden.jem.components.bus.WordBus;
import de.heiden.jem.models.c64.components.cpu.CPU6510State;
import de.heiden.jem.models.c64.components.cpu.Patch;
import de.heiden.jem.models.c64.components.util.FileUtil;
import de.heiden.jem.models.c64.components.util.StringUtil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Replaces standard C64 load routine at $F4A5.
 * Intercepts load routine at $F4C4 directly after printing "SEARCHING FOR".
 * Loads files from a given directory.
 */
public class LoadFromDirectory extends Patch {
  /**
   * Base directory to load files from.
   */
  private final Path baseDir;

  /**
   * Constructor.
   *
   * @param baseDirectory Base directory to load files from
   */
  public LoadFromDirectory(Path baseDirectory) {
    super(0xF4C4);

    this.baseDir = baseDirectory;
  }

  @Override
  protected int execute(CPU6510State state, BusDevice bus) {
    // Read filename from ($BB), length ($B7)
    WordBus wordBus = new WordBus(bus);
    String filename = StringUtil.read(bus, wordBus.readWord(0xBB), bus.read(0xB7));
    if (!filename.contains(".")) {
      filename = filename.toLowerCase() + ".prg";
    }

    try {
      InputStream file = Files.newInputStream(baseDir.resolve(filename));
      int endAddress = bus.read(0xB9) == 0 ?
        FileUtil.read(file, wordBus.readWord(0xC3), bus) :
        FileUtil.read(file, bus);

      wordBus.writeWord(0xAE, endAddress);

      // Continue at $F5A9: Successful load.
      state.PC = 0xF5A9;
      return DO_NOT_EXECUTE;

    } catch (FileNotFoundException e) {
      logger.warn("File not found {}.", filename, e);
      state.PC = 0xF704;
      return DO_NOT_EXECUTE;

    } catch (IOException e) {
      logger.error("Failed to load {}.", filename, e);
      return RTS;
    }

  }
}
