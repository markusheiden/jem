package de.heiden.jem.models.c64.components.patch;

import de.heiden.jem.components.bus.BusDevice;
import de.heiden.jem.components.bus.WordBus;
import de.heiden.jem.models.c64.components.cpu.CPU6510State;
import de.heiden.jem.models.c64.components.cpu.Patch;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Replaces standard C64 load routine at $F4A5.
 * Intercepts load routine at $F4C4 directly after printing "SEARCHING FOR".
 * Loads a fixed file.
 */
public class LoadFile extends Patch {
  /**
   * File content.
   */
  private final byte[] content;

  /**
   * Constructor.
   *
   * @param content File content.
   */
  public LoadFile(byte[] content) {
    super(0xF4C4);

    this.content = content;
  }

  @Override
  protected int execute(CPU6510State state, BusDevice bus) {
    WordBus wordBus = new WordBus(bus);

    try {
      ByteArrayInputStream file = new ByteArrayInputStream(content);
      int endAddress = bus.read(0xB9) == 0 ?
        FileUtil.read(file, wordBus.readWord(0xC3), bus) :
        FileUtil.read(file, bus);

      wordBus.writeWord(0xAE, endAddress);

      // Continue at $F5A9.
      state.PC = 0xF5A9;
      return DO_NOT_EXECUTE;

    } catch (IOException e) {
      logger.error("Failed to load program.", e);
      return RTS;
    }
  }
}
