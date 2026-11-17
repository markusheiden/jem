package de.heiden.jem.models.c64.components;

import de.heiden.jem.components.bus.BusDevice;
import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.components.clock.RealTimeSlowDown;
import de.heiden.jem.models.c64.components.cia.AbstractCIA;
import de.heiden.jem.models.c64.components.cpu.C64Bus;
import de.heiden.jem.models.c64.components.cpu.CPU6510;
import de.heiden.jem.models.c64.components.joystick.Joystick;
import de.heiden.jem.models.c64.components.keyboard.Keyboard;
import de.heiden.jem.models.c64.components.keyboard.KeyboardBuffer;
import de.heiden.jem.models.c64.components.memory.ColorRAM;
import de.heiden.jem.models.c64.components.memory.RAM;
import de.heiden.jem.models.c64.components.memory.ROM;
import de.heiden.jem.models.c64.components.patch.LoadFromDirectory;
import de.heiden.jem.models.c64.components.vic.VIC;
import de.heiden.jem.models.c64.components.vic.VICBus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
    private final VIC vic;

    /**
     * Slow down to real time, measures the time needed for each simulated second.
     * Null, if the emulation runs as fast as possible.
     */
    private final RealTimeSlowDown realTimeSlowDown;

    /**
     * Builder for a C64, by default a PAL C64 with old CIAs.
     *
     * @param clock
     *         Clock.
     */
    public static C64Builder builder(Clock clock) {
        return new C64Builder(clock);
    }

    /**
     * Constructor: Wires up the components.
     *
     * @param clock
     *         Clock.
     * @param ram
     *         RAM
     * @param colorRam
     *         Color RAM
     * @param basic
     *         Basic ROM
     * @param kernel
     *         Kernel ROM
     * @param charset
     *         Charset ROM
     * @param cia1
     *         CIA1
     * @param cia2
     *         CIA2
     * @param vicBus
     *         Bus of the VIC
     * @param vic
     *         VIC
     * @param sid
     *         Device for the SID area $D400-$D7FF
     * @param cpu
     *         CPU
     * @param realTimeSlowDown
     *         Slow down to real time, null to run as fast as possible
     */
    C64(
            @Nonnull Clock clock,
            @Nonnull RAM ram,
            @Nonnull ColorRAM colorRam,
            @Nonnull ROM basic,
            @Nonnull ROM kernel,
            @Nonnull ROM charset,
            @Nonnull AbstractCIA cia1,
            @Nonnull AbstractCIA cia2,
            @Nonnull VICBus vicBus,
            @Nonnull VIC vic,
            @Nonnull BusDevice sid,
            @Nonnull CPU6510 cpu,
            @Nullable RealTimeSlowDown realTimeSlowDown) throws IOException {
        this.clock = clock;
        this.vic = vic;
        this.cpu = cpu;
        this.realTimeSlowDown = realTimeSlowDown;

        clock.addClockedComponent(Clock.CIA1, cia1);
        clock.addClockedComponent(Clock.CIA2, cia2);
        clock.addClockedComponent(Clock.VIC_DISPLAY, vic.displayUnit);
        clock.addClockedComponent(Clock.CPU, cpu);

        keyboard = new Keyboard(clock, cia1.portA(), cia1.portB());
        // Joystick port 1 is connected to CIA 1 port B, joystick port 2 to CIA 1 port A.
        joystick1 = new Joystick(clock, cia1.portB());
        joystick2 = new Joystick(clock, cia1.portA());

        cpuBus = new C64Bus(ram, basic, vic, sid, colorRam, cia1, cia2, charset, kernel, vicBus.openBus());
        cpuBus.connect(cpu.getPort());
        cpu.connect(cpuBus);
        cpu.getIRQ().connect(cia1.getIRQ());
        cpu.getIRQ().connect(vic.getIRQ());
        cpu.getNMI().connect(cia2.getIRQ());
        cpu.getNMI().connect(keyboard.getNMI());
        cpu.getRDY().connect(vic.getBA());

        keyboardBuffer = new KeyboardBuffer(clock, cpuBus);

        init(ram, colorRam);
    }

    /**
     * Additional init for test purposes.
     */
    private void init(RAM ram, ColorRAM colorRam) throws IOException {
        // Init RAM with 0x02 (crash) to easier detect wrong behaviour.
        // Not via the bus, because that would write to the registers of the I/O chips too.
        // TODO 2010-06-22 mh: realistic init?
        for (var addr = 0; addr < 0x10000; addr++) {
            ram.write(0x02, addr);
        }

        // Fill the screen with the complete character set.
        for (var i = 0; i < 0x400; i++) {
            ram.write(i & 0xFF, 0x400 + i);
            colorRam.write(i & 0x0F, i);
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
    public VIC getVIC() {
        return vic;
    }

    /**
     * Clock.
     */
    public Clock getClock() {
        return clock;
    }

    /**
     * Slow down to real time, measures the time needed for each simulated second.
     * Null, if the emulation runs as fast as possible.
     */
    public RealTimeSlowDown getRealTimeSlowDown() {
        return realTimeSlowDown;
    }
}
