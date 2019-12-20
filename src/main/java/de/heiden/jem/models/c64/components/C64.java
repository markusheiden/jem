package de.heiden.jem.models.c64.components;

import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.components.clock.RealTimeSlowDown;
import de.heiden.jem.models.c64.components.cia.CIA6526;
import de.heiden.jem.models.c64.components.cpu.C64Bus;
import de.heiden.jem.models.c64.components.cpu.CPU6510;
import de.heiden.jem.models.c64.components.cpu.CPU6510Debugger;
import de.heiden.jem.models.c64.components.keyboard.Keyboard;
import de.heiden.jem.models.c64.components.memory.ColorRAM;
import de.heiden.jem.models.c64.components.memory.RAM;
import de.heiden.jem.models.c64.components.memory.ROM;
import de.heiden.jem.models.c64.components.memory.ROMLoader;
import de.heiden.jem.models.c64.components.vic.VIC6569PAL;
import de.heiden.jem.models.c64.components.vic.VICBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * C64.
 */
public class C64 {
  /**
   * Logger.
   */
  private final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * Main clock.
   */
  private final Clock _clock;

  /**
   * CPU bus.
   */
  private C64Bus _cpuBus;

  /**
   * CPU.
   */
  private final CPU6510 _cpu;

  /**
   * Keyboard.
   */
  private Keyboard _keyboard;

  /**
   * VIC.
   */
  private final VIC6569PAL _vic;

  /**
   * Constructor.
   *
   * @param clock Clock.
   */
  public C64(Clock clock) throws Exception {
    this(clock, false);
  }

  /**
   * Constructor.
   *
   * @param clock Clock.
   * @param debug use debugger cpu?
   */
  public C64(Clock clock, boolean debug) throws Exception {
    _clock = clock;

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

    _cpu = _clock.addClockedComponent(Clock.CPU, debug ? new CPU6510Debugger() : new CPU6510());
    _cpuBus = new C64Bus(_ram, basic, _vic, colorRam, cia1, cia2, charset, kernel);
    _cpuBus.connect(_cpu.getPort());
    _cpu.connect(_cpuBus);
    _cpu.getIRQ().connect(cia1.getIRQ());
    _cpu.getIRQ().connect(_vic.getIRQ());
    _cpu.getNMI().connect(cia2.getIRQ());
    _cpu.getNMI().connect(_keyboard.getNMI());

    // real time measurement
    // TODO 2010-03-14 mh: NTSC: 1022700 Hz
    new RealTimeSlowDown(clock, 985248, 100);

    init();
  }

  /**
   * Additional init for test purposes.
   */
  private void init() throws IOException {
    // init RAM with 0x02 (crash) to easier detect wrong behaviour
    // TODO 2010-06-22 mh: realistic init?
    for (int addr = 0; addr < 0x10000; addr++) {
      _cpuBus.write(0x02, addr);
    }

    // fill screen with complete character set
    for (int i = 0; i < 0x400; i++) {
      _cpuBus.write(i & 0xFF, 0x400 + i);
      _cpuBus.write(i & 0x0F, 0xD800 + i);
    }

//    _cpu.add(new LoadFile(new File("/Users/markus/Workspaces/jem-projects/jem/src/test/resources/testsuite2.15")));
//    FileUtil.read(new File("/Users/markus/Workspaces/jem-projects/jem/commando.prg"), _cpuBus);
//    FileUtil.read(new File("/Users/markus/Workspaces/jem-projects/jem/bluemax.prg"), _cpuBus);
//    FileUtil.read(new File("/Users/markus/Workspaces/jem-projects/jem/src/test/resources/testsuite2.15/loadth.prg"), _cpuBus);
  }

  /**
   * Start emulation.
   */
  public void start() throws Exception {
    logger.debug("start");
    _clock.run();

//    final int ticks = 1000000;
//    for (;;)
//    {
//      long time = System.currentTimeMillis();
//      _clock.run(ticks);
//      time = System.currentTimeMillis() - time;
//      _logger.debug("executed {} in {} ms", ticks, time);
//    }
  }

  /**
   * Stop emulation.
   */
  public void stop() {
    _clock.close();
  }

  //
  // Expose components, e.g. for debugger
  //

  /**
   * CPU bus.
   */
  public C64Bus getCpuBus() {
    return _cpuBus;
  }

  /**
   * CPU.
   */
  public CPU6510 getCpu() {
    return _cpu;
  }

  /**
   * Keyboard.
   */
  public Keyboard getKeyboard() {
    return _keyboard;
  }

  /**
   * VIC.
   */
  public VIC6569PAL getVIC() {
    return _vic;
  }
}
