package de.heiden.jem.models.c64.components;

import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.components.clock.RealTimeSlowDown;
import de.heiden.jem.models.c64.components.cia.CIA6526;
import de.heiden.jem.models.c64.components.cpu.C64Bus;
import de.heiden.jem.models.c64.components.cpu.CPU6510;
import de.heiden.jem.models.c64.components.cpu.CPU6510Debugger;
import de.heiden.jem.models.c64.components.joystick.Joystick;
import de.heiden.jem.models.c64.components.keyboard.Keyboard;
import de.heiden.jem.models.c64.components.keyboard.KeyboardBuffer;
import de.heiden.jem.models.c64.components.memory.ColorRAM;
import de.heiden.jem.models.c64.components.memory.RAM;
import de.heiden.jem.models.c64.components.memory.ROMLoader;
import de.heiden.jem.models.c64.components.patch.LoadFromDirectory;
import de.heiden.jem.models.c64.components.vic.VIC6569PAL;
import de.heiden.jem.models.c64.components.vic.VICBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

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
    private final Clock clock;

    /**
     * CPU bus.
     */
    private C64Bus cpuBus;

    /**
     * CPU.
     */
    private final CPU6510 cpu;

    /**
     * Keyboard.
     */
    private Keyboard keyboard;

    /**
     * Keyboard buffer of the KERNAL, e.g. for pasting text.
     */
    private final KeyboardBuffer keyboardBuffer;

    /**
     * Joystick in port 1.
     */
    private final Joystick joystick1;

    /**
     * Joystick in port 2.
     */
    private final Joystick joystick2;

    /**
     * VIC.
     */
    private final VIC6569PAL vic;

    /**
     * Slow down to real time, measures the time needed for each simulated second.
     */
    private final RealTimeSlowDown realTimeSlowDown;

    /**
     * Constructor.
     *
     * @param clock
     *         Clock.
     */
    public C64(Clock clock) throws Exception {
        this(clock, false);
    }

    /**
     * Constructor.
     *
     * @param clock
     *         Clock.
     * @param debug
     *         use debugger cpu?
     */
    public C64(Clock clock, boolean debug) throws Exception {
        this.clock = clock;

        var ram = new RAM(0x10000);
        var colorRam = new ColorRAM(0x400);
        var basic = ROMLoader.basic(ROMLoader.DEFAULT_BASIC);
        var kernel = ROMLoader.kernel(ROMLoader.DEFAULT_KERNEL);
        var charset = ROMLoader.character(ROMLoader.DEFAULT_CHARACTER);

        var cia1 = new CIA6526(clock);
        var cia2 = new CIA6526(clock);

        var vicBus = new VICBus(cia2.portA(), ram, charset);
        vic = new VIC6569PAL(clock, vicBus, colorRam);

        keyboard = new Keyboard(cia1.portA(), cia1.portB());
        // Joystick port 1 is connected to CIA 1 port B, joystick port 2 to CIA 1 port A.
        joystick1 = new Joystick(clock, cia1.portB());
        joystick2 = new Joystick(clock, cia1.portA());

        cpu = clock.addClockedComponent(Clock.CPU, debug ? new CPU6510Debugger() : new CPU6510());
        cpuBus = new C64Bus(ram, basic, vic, colorRam, cia1, cia2, charset, kernel);
        cpuBus.connect(cpu.getPort());
        cpu.connect(cpuBus);
        cpu.getIRQ().connect(cia1.getIRQ());
        cpu.getIRQ().connect(vic.getIRQ());
        cpu.getNMI().connect(cia2.getIRQ());
        cpu.getNMI().connect(keyboard.getNMI());
        cpu.getRDY().connect(vic.getBA());

        keyboardBuffer = new KeyboardBuffer(clock, cpuBus);

        // real time measurement
        // TODO 2010-03-14 mh: NTSC: 1022700 Hz
        realTimeSlowDown = new RealTimeSlowDown(clock, 985248, 100);

        init();
    }

    /**
     * Additional init for test purposes.
     */
    private void init() throws IOException {
        // Init RAM with 0x02 (crash) to easier detect wrong behaviour.
        // TODO 2010-06-22 mh: realistic init?
        for (var addr = 0; addr < 0x10000; addr++) {
            cpuBus.write(0x02, addr);
        }

        // Fill the screen with the complete character set.
        for (var i = 0; i < 0x400; i++) {
            cpuBus.write(i & 0xFF, 0x400 + i);
            cpuBus.write(i & 0x0F, 0xD800 + i);
        }

        // Load files from the disk directory, e.g. LOAD"GAME",8,1.
        cpu.add(new LoadFromDirectory(Path.of("disk").toAbsolutePath()));

//    cpu.add(new LoadFile(new File("/Users/markus/projects/jem/src/test/resources/testsuite2.15")));
//    FileUtil.read(Path.of("/Users/markus/projects/jem/xxx.prg"), cpuBus);
//    FileUtil.read(Path.of("/Users/markus/projects/jem/src/test/resources/testsuite2.15/loadth.prg"), cpuBus);
    }

    /**
     * Start emulation.
     */
    public void start() throws Exception {
        logger.debug("start");
        clock.run();

//    final int ticks = 1000000;
//    //noinspection InfiniteLoopStatement
//    while (true)
//    {
//      long time = System.currentTimeMillis();
//      clock.run(ticks);
//      time = System.currentTimeMillis() - time;
//      logger.debug("executed {} in {} ms", ticks, time);
//    }
    }

    /**
     * Stop emulation.
     */
    public void stop() {
        clock.close();
    }

    //
    // Expose components, e.g. for debugger
    //

    /**
     * CPU bus.
     */
    public C64Bus getCpuBus() {
        return cpuBus;
    }

    /**
     * CPU.
     */
    public CPU6510 getCpu() {
        return cpu;
    }

    /**
     * Keyboard.
     */
    public Keyboard getKeyboard() {
        return keyboard;
    }

    /**
     * Keyboard buffer of the KERNAL, e.g. for pasting text.
     */
    public KeyboardBuffer getKeyboardBuffer() {
        return keyboardBuffer;
    }

    /**
     * Joystick in port 1.
     */
    public Joystick getJoystick1() {
        return joystick1;
    }

    /**
     * Joystick in port 2.
     */
    public Joystick getJoystick2() {
        return joystick2;
    }

    /**
     * VIC.
     */
    public VIC6569PAL getVIC() {
        return vic;
    }

    /**
     * Slow down to real time, measures the time needed for each simulated second.
     */
    public RealTimeSlowDown getRealTimeSlowDown() {
        return realTimeSlowDown;
    }
}
