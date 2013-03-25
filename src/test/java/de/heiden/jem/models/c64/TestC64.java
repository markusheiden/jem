package de.heiden.jem.models.c64;

import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.components.clock.serialthreads.SerialClock;
import de.heiden.jem.models.c64.components.CIA6526;
import de.heiden.jem.models.c64.components.cpu.C64Bus;
import de.heiden.jem.models.c64.components.cpu.CPU6510;
import de.heiden.jem.models.c64.components.keyboard.Keyboard;
import de.heiden.jem.models.c64.components.memory.ColorRAM;
import de.heiden.jem.models.c64.components.memory.RAM;
import de.heiden.jem.models.c64.components.memory.ROM;
import de.heiden.jem.models.c64.components.patch.LoadFile;
import de.heiden.jem.models.c64.components.patch.SystemOut;
import de.heiden.jem.models.c64.components.vic.VIC6569PAL;
import de.heiden.jem.models.c64.components.vic.VICBus;
import de.heiden.jem.models.c64.util.ROMLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Modified C64 for better testability.
 */
public class TestC64 {
  /**
   * Logger.
   */
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final Clock _clock;

  private C64Bus _cpuBus;
  private final CPU6510 _cpu;

  private Keyboard _keyboard;
  private final VIC6569PAL _vic;

  /**
   * Constructor.
   */
  public TestC64() throws Exception {
    _clock = new SerialClock();

    RAM _ram = new RAM(0x10000);
    ColorRAM colorRam = new ColorRAM(0x400);
    ROM basic = ROMLoader.basic(ROMLoader.DEFAULT_BASIC);
    ROM kernel = ROMLoader.kernel(ROMLoader.DEFAULT_KERNEL);
    ROM charset = ROMLoader.character(ROMLoader.DEFAULT_CHARACTER);

    CIA6526 cia1 = new CIA6526(_clock);
    CIA6526 cia2 = new CIA6526(_clock);

    VICBus vicBus = new VICBus(cia2.portA(), _ram, charset);
    _vic = new VIC6569PAL(_clock, vicBus, colorRam);

    _keyboard = new Keyboard(cia1.portA(), cia1.portB());

    _cpu = new CPU6510(_clock);
    _cpuBus = new C64Bus(_cpu.getPort(), _ram, basic, _vic, colorRam, cia1, cia2, charset, kernel);
    _cpu.connect(_cpuBus);
    _cpu.getIRQ().connect(cia1.getIRQ());
    _cpu.getIRQ().connect(_vic.getIRQ());
    _cpu.getNMI().connect(cia2.getIRQ());
    _cpu.getNMI().connect(_keyboard.getNMI());

    //
    // ROM patches
    //

    _cpu.add(new SystemOut());
    _cpu.add(new LoadFile("testsuite2.15"));
  }

  /**
   * Start emulation.
   */
  public void start() throws Exception {
    // init RAM with 0x02 (crash) to easier detect wrong behaviour
    for (int addr = 0; addr < 0x10000; addr++) {
      _cpuBus.write(0x02, addr);
    }

    _clock.run();
    _clock.dispose();
  }
}
