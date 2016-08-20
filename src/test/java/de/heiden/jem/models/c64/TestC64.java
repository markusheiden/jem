package de.heiden.jem.models.c64;

import java.io.OutputStream;

import de.heiden.jem.components.bus.BusDevice;
import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.components.clock.ClockEvent;
import de.heiden.jem.components.clock.serialthreads.SerialClock;
import de.heiden.jem.models.c64.components.CIA6526;
import de.heiden.jem.models.c64.components.cpu.C64Bus;
import de.heiden.jem.models.c64.components.cpu.CPU6510Debugger;
import de.heiden.jem.models.c64.components.cpu.Patch;
import de.heiden.jem.models.c64.components.keyboard.Keyboard;
import de.heiden.jem.models.c64.components.memory.ColorRAM;
import de.heiden.jem.models.c64.components.memory.RAM;
import de.heiden.jem.models.c64.components.memory.ROM;
import de.heiden.jem.models.c64.components.patch.*;
import de.heiden.jem.models.c64.components.vic.VIC6569PAL;
import de.heiden.jem.models.c64.components.vic.VICBus;
import de.heiden.jem.models.c64.gui.KeyListener;
import de.heiden.jem.models.c64.gui.PCMapping;
import de.heiden.jem.models.c64.util.ROMLoader;

/**
 * Modified C64 for better testability.
 */
public class TestC64 {
  private final Clock _clock;

  private C64Bus _cpuBus;
  private final CPU6510Debugger _cpu;

  private Keyboard _keyboard;
  private final VIC6569PAL _vic;

  /**
   * Buffer for capturing console output.
   */
  private final SystemOut systemOut = new SystemOut();

  /**
   * Detects when a (basic) program ends.
   */
  private final ProgramEndDetector programEnd = new ProgramEndDetector();

  /**
   * Detects when a (basic) program ends.
   */
  private final BrkDetector brk = new BrkDetector();

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

    _cpu = _clock.addClockedComponent(Clock.CPU, new CPU6510Debugger());
    _cpuBus = new C64Bus(_ram, basic, _vic, colorRam, cia1, cia2, charset, kernel);
    _cpuBus.connect(_cpu.getPort());
    _cpu.connect(_cpuBus);
    _cpu.getIRQ().connect(cia1.getIRQ());
    _cpu.getIRQ().connect(_vic.getIRQ());
    _cpu.getNMI().connect(cia2.getIRQ());
    _cpu.getNMI().connect(_keyboard.getNMI());

    // init RAM with 0x02 (crash) to easier detect wrong behaviour
    for (int addr = 0; addr < 0x10000; addr++) {
      _cpuBus.write(0x02, addr);
    }

    //
    // ROM patches
    //

    _cpu.add(systemOut);
    _cpu.add(new StopAtSystemIn());
    _cpu.add(programEnd);

    _clock.addClockEvent(100000, new ClockEvent("Interrupt check") {
      @Override
      public void execute(long tick) {
        if (Thread.interrupted()) {
          throw new IllegalArgumentException("Thread has been interrupted");
        }

        _clock.addClockEvent(tick + 100000, this);
      }
    });
  }

  /**
   * Add patch.
   */
  public void add(Patch patch) {
    _cpu.add(patch);
  }

  /**
   * Clock.
   */
  public Clock getClock() {
    return _clock;
  }

  /**
   * Get bus.
   */
  public BusDevice getBus() {
    return _cpuBus;
  }

  /**
   * Set stream for screen output.
   */
  public void setSystemOut(OutputStream systemOut) {
    this.systemOut.setStream(systemOut);
  }

  /**
   * Get stream for keyboard input.
   */
  public java.awt.event.KeyListener getSystemIn() {
    return new KeyListener(_keyboard, new PCMapping());
  }

  /**
   * Add a patch to the cpu, to insert a RTS at the given address.
   *
   * @param addr Address to write RTS to
   */
  public void rts(int addr) {
    _cpu.add(new Return(addr));
  }

  /**
   * Start emulation.
   */
  public void start() throws Exception {
    _clock.run();
    _clock.close();
  }

  /**
   * Has the program ended?.
   */
  public boolean hasEnded() {
    return programEnd.hasEnded();
  }

  /**
   * Has the program run into a BRK?.
   */
  public boolean hasBrk() {
    return brk.hasBrk();
  }
}
