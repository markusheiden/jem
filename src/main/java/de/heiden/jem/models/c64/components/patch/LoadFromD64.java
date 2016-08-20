package de.heiden.jem.models.c64.components.patch;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import de.heiden.c64dt.disk.IFile;
import de.heiden.c64dt.disk.d64.D64;
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
public class LoadFromD64 extends Patch {
  /**
   * D64 image to load files from.
   */
  private final D64 d64;

  /**
   * Constructor.
   *
   * @param d64 D64 image to load files from.
   */
  public LoadFromD64(D64 d64) {
    super(0xF4C4);

    this.d64 = d64;
  }

  @Override
  protected int execute(CPU6510State state, BusDevice bus) {
    // Read filename from ($BB), length ($B7)
    WordBus wordBus = new WordBus(bus);
    int addr = wordBus.readWord(0xBB);
    int len = bus.read(0xB7);
    byte[] name = new byte[len];
    for (int i = 0; i < name.length; i++) {
      name[i] = (byte) bus.read(addr++);
    }

    Optional<IFile> file = d64.getDirectory().getFiles().stream()
      .filter(f -> Arrays.equals(f.getName(), name))
      .findFirst();

    if (!file.isPresent()) {
      state.PC = 0xF704;
      return -1;
    }

    byte[] content = d64.read(file.get());

    try {
      int endAddress = bus.read(0xB9) == 0 ?
        FileUtil.read(new ByteArrayInputStream(content), wordBus.readWord(0xC3), bus) :
        FileUtil.read(new ByteArrayInputStream(content), bus);

      wordBus.writeWord(0xAE, endAddress);

      state.C = false; // OK
      state.X = ByteUtil.lo(endAddress);
      state.Y = ByteUtil.hi(endAddress);

    } catch (IOException e) {
      logger.error("Failed to load {}", file.toString(), e);
    }

    return 0x60; // rts
  }
}