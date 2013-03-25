package de.heiden.jem.models.c64.components.patch;

import de.heiden.c64dt.util.ByteUtil;
import de.heiden.jem.components.bus.BusDevice;
import de.heiden.jem.models.c64.components.cpu.CPU6510State;
import de.heiden.jem.models.c64.components.cpu.Patch;
import de.heiden.jem.models.c64.util.BusUtil;
import de.heiden.jem.models.c64.util.FileUtil;
import de.heiden.jem.models.c64.util.StringUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Replaces standard C64 load routine at $F4A5
 */
public class LoadFile extends Patch {
  /**
   * Base package to load files from.
   */
  private final String basePackage;

  /**
   * Base directory to load files from.
   */
  private final File baseDir;

  /**
   * Constructor.
   *
   * @param basePackage Base package to load files from
   */
  public LoadFile(String basePackage) {
    super(0xF4A5);

    this.basePackage = basePackage;
    this.baseDir = null;
  }

  /**
   * Constructor.
   *
   * @param baseDirectory Base directory to load files from
   */
  public LoadFile(File baseDirectory) {
    super(0xF4A5);

    this.basePackage = null;
    this.baseDir = baseDirectory;
  }

  @Override
  protected int execute(CPU6510State state, BusDevice bus) {
    String filename = StringUtil.read(bus, BusUtil.readWord(0xBB, bus), bus.read(0xB7));
    filename = filename.toLowerCase() + ".prg";

    try {

      InputStream file = baseDir != null ?
        new FileInputStream(new File(baseDir, filename)) :
        getClass().getResourceAsStream(basePackage + "/" + filename);
      assert file != null : "file " + filename + " exists";

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

//    state.PC = 2070;
    return 0x60;
  }
}
