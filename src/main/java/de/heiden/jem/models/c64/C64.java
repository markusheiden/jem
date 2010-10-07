package de.heiden.jem.models.c64;

import de.heiden.c64dt.util.ResourceLoader;
import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.components.clock.RealTimeSlowDown;
import de.heiden.jem.components.clock.serialthreads.SerialClock;
import de.heiden.jem.models.c64.components.CIA6526;
import de.heiden.jem.models.c64.components.ColorRAM;
import de.heiden.jem.models.c64.components.RAM;
import de.heiden.jem.models.c64.components.ROM;
import de.heiden.jem.components.bus.C64Bus;
import de.heiden.jem.models.c64.components.cpu.CPU6510;
import de.heiden.jem.models.c64.components.cpu.CPU6510Debugger;
import de.heiden.jem.models.c64.components.keyboard.Keyboard;
import de.heiden.jem.models.c64.components.vic.VIC6569PAL;
import de.heiden.jem.models.c64.components.vic.VICBus;
import de.heiden.jem.models.c64.gui.VICScreen;
import de.heiden.jem.models.c64.util.ROMLoader;
import org.apache.log4j.Logger;

import javax.swing.JFrame;
import java.awt.event.KeyListener;

/**
 * C64.
 */
public class C64
{
  private JFrame _frame;

  private final Clock _clock;

  private C64Bus _cpuBus;
  private final CPU6510 _cpu;

  private Keyboard _keyboard;
  private final VIC6569PAL _vic;

  /**
   * Constructor.
   *
   * @param debug use debugger cpu?
   */
  public C64(boolean debug) throws Exception
  {
    this(new SerialClock(), debug);
  }

  /**
   * Constructor.
   *
   * @param clock clock
   * @param debug use debugger cpu?
   */
  public C64(Clock clock, boolean debug) throws Exception
  {
    _clock = clock;

    RAM _ram = new RAM(0x10000);
    ColorRAM colorRam = new ColorRAM(0x400);
    ROM basic = ROMLoader.basic(ROMLoader.DEFAULT_BASIC);
    ROM kernel = ROMLoader.kernel(ROMLoader.DEFAULT_KERNEL);
    ROM charset = ROMLoader.character(ROMLoader.DEFAULT_CHARACTER);

    CIA6526 cia1 = new CIA6526(clock);
    CIA6526 cia2 = new CIA6526(clock);

    VICBus vicBus = new VICBus(cia2.portA(), _ram, charset);
    _vic = new VIC6569PAL(clock, vicBus, colorRam);

    _keyboard = new Keyboard(cia1.portA(), cia1.portB());

    _cpu = debug ? new CPU6510Debugger(clock) : new CPU6510(clock);
    _cpuBus = new C64Bus(_cpu.getPort(), _ram, basic, _vic, colorRam, cia1, cia2, charset, kernel);
    _cpu.connect(_cpuBus);
    _cpu.getIRQ().connect(cia1.getIRQ());
    _cpu.getNMI().connect(cia2.getIRQ());
    _cpu.getNMI().connect(_keyboard.getNMI());

    // real time measurement
    // TODO 2010-03-14 mh: NTSC: 1022700 Hz
    new RealTimeSlowDown(clock, 985248, 1000);
  }

  public void start() throws Exception
  {
    // init RAM with 0x02 (crash) to easier detect wrong behaviour
    // TODO 2010-06-22 mh: realistic init?
    for (int addr = 0; addr < 0x10000; addr++)
    {
      _cpuBus.write(0x02, addr);
    }

    // fill screen with complete character set
    for (int i = 0; i < 0x400; i++)
    {
      _cpuBus.write(i & 0xFF, 0x400 + i);
      _cpuBus.write(i & 0x0F, 0xD800 + i);
    }

    int[] autoload = ResourceLoader.load("commando.prg");
    int start = autoload[0] + (autoload[1] << 8);
    for (int i = 2; i < autoload.length; i++)
    {
      _cpuBus.write(autoload[i], start + i - 2);
    }

    show(_vic, _keyboard.getKeyListener());

    _logger.debug("start");
    final int ticks = 1000000;
    while (true)
    {
      long time = System.currentTimeMillis();
      _clock.run(ticks);
      time = System.currentTimeMillis() - time;
      _logger.debug("executed " + ticks + " in " + time + " ms");
    }

//    clock.dispose();
  }

  public void stop()
  {
    _frame.setVisible(false);
    _frame.dispose();
    _frame = null;
  }

  /**
   * For testing purposes only!
   */
  public JFrame show(VIC6569PAL vic, KeyListener keyListener)
  {
    _frame = new JFrame("C64");
    _frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    final VICScreen screen = new VICScreen(vic._displayUnit);
    _frame.addKeyListener(keyListener);
    _frame.getContentPane().add(screen);

    // pack
    _frame.pack();
    // _frame.setResizable(false);
    _frame.setVisible(true);

    return _frame;
  }

  //
  // Expose components, e.g. for debugger
  //

  public C64Bus getCpuBus()
  {
    return _cpuBus;
  }

  /**
   * Get cpu.
   */
  public CPU6510 getCpu()
  {
    return _cpu;
  }

  //
  // private attributes
  //

  /**
   * Logger.
   */
  private final Logger _logger = Logger.getLogger(getClass());
}
