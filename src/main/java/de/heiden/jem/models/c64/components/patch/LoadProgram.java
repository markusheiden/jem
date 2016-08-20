package de.heiden.jem.models.c64.components.patch;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import de.heiden.c64dt.util.ByteUtil;
import de.heiden.jem.components.bus.BusDevice;
import de.heiden.jem.components.bus.WordBus;
import de.heiden.jem.models.c64.components.cpu.CPU6510State;
import de.heiden.jem.models.c64.components.cpu.Patch;
import de.heiden.jem.models.c64.util.FileUtil;

/**
 * Replaces standard C64 load routine at $F4A5.
 * Intercepts load routine at $F4C4 directly after printing "SEARCHING FOR".
 */
public class LoadProgram extends Patch {
  /**
   * File content.
   */
  private final byte[] content;

  /**
   * Constructor.
   *
   * @param content File content.
   */
  public LoadProgram(byte[] content) {
    super(0xF4C4);

    this.content = content;
  }

  @Override
  protected int execute(CPU6510State state, BusDevice bus) {
    WordBus wordBus = new WordBus(bus);

    try {
      int endAddress = bus.read(0xB9) == 0 ?
        FileUtil.read(new ByteArrayInputStream(content), wordBus.readWord(0xC3), bus) :
        FileUtil.read(new ByteArrayInputStream(content), bus);

      wordBus.writeWord(0xAE, endAddress);

      state.C = false; // OK
      state.X = ByteUtil.lo(endAddress);
      state.Y = ByteUtil.hi(endAddress);

    } catch (FileNotFoundException e) {
      state.PC = 0xF704;
      return -1;

    } catch (IOException e) {
      logger.error("Failed to load program.", e);
    }

    return 0x60; // rts
  }
}
