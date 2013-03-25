package de.heiden.jem.models.c64.components.cpu.patch;

import de.heiden.c64dt.util.ByteUtil;
import de.heiden.jem.components.bus.BusDevice;
import de.heiden.jem.models.c64.components.cpu.CPU6510State;
import de.heiden.jem.models.c64.components.cpu.Patch;
import de.heiden.jem.models.c64.util.BusUtil;
import de.heiden.jem.models.c64.util.FileUtil;
import de.heiden.jem.models.c64.util.StringUtil;

import java.io.File;
import java.io.IOException;

/**
 * Replaces standard C64 load routine at $F4A5
 */
public class LoadFile extends Patch {
  /**
   * Constructor.
   */
  public LoadFile() {
    super(0xF4A5);
  }

  @Override
  protected boolean execute(CPU6510State state, BusDevice bus) {
    String filename = StringUtil.read(bus, BusUtil.readWord(0xBB, bus), bus.read(0xB7));
    try {
      File file = new File("/Users/markus/Downloads/C64/tsuit215", filename.toLowerCase() + ".prg");

      int endAddress = bus.read(0xB9) == 0 ?
        FileUtil.read(file, BusUtil.readWord(0xC3, bus), bus) :
        FileUtil.read(file, bus);
      BusUtil.writeWord(0xAE, endAddress, bus);
      state.C = false;
      state.X = ByteUtil.lo(endAddress);
      state.Y = ByteUtil.hi(endAddress);
    } catch (IOException e) {
      logger.error("Failed to load " + filename, e);
    }

    state.PC = 2070;
    return false;
  }
}
