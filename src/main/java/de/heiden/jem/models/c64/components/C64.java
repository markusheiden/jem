package de.heiden.jem.models.c64.components;

import de.heiden.jem.components.bus.BusDevice;
import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.components.clock.RealTimeSlowDown;
import de.heiden.jem.models.c64.components.cia.AbstractCIA;
import de.heiden.jem.models.c64.components.cia.CIA6526;
import de.heiden.jem.models.c64.components.cia.CIA6526A;
import de.heiden.jem.models.c64.components.cia.TodInput;
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
import de.heiden.jem.models.c64.components.sid.SIDStub;
import de.heiden.jem.models.c64.components.vic.VIC;
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
    public static Builder builder(Clock clock) {
        return new Builder(clock);
    }

    /**
     * Constructor.
     *
     * @param builder
     *         configuration
     */
    private C64(Builder builder) throws Exception {
        clock = builder.clock;
        var videoSystem = builder.videoSystem;
        var todInput = videoSystem.todInput().withFrequency(builder.powerLineFrequency());

        var ram = new RAM(0x10000);
        var colorRam = new ColorRAM(0x400);
        var basic = ROMLoader.basic(ROMLoader.DEFAULT_BASIC);
        var kernel = ROMLoader.kernel(ROMLoader.DEFAULT_KERNEL);
        var charset = ROMLoader.character(ROMLoader.DEFAULT_CHARACTER);

        var cia1 = clock.addClockedComponent(Clock.CIA1, builder.createCia(todInput));
        var cia2 = clock.addClockedComponent(Clock.CIA2, builder.createCia(todInput));

        var vicBus = new VICBus(cia2.portA(), ram, charset);
        vic = videoSystem.createVic(clock, vicBus, colorRam);
        clock.addClockedComponent(Clock.VIC_DISPLAY, vic.displayUnit);

        keyboard = new Keyboard(clock, cia1.portA(), cia1.portB());
        // Joystick port 1 is connected to CIA 1 port B, joystick port 2 to CIA 1 port A.
        joystick1 = new Joystick(clock, cia1.portB());
        joystick2 = new Joystick(clock, cia1.portA());

        cpu = clock.addClockedComponent(Clock.CPU, builder.debug ? new CPU6510Debugger() : new CPU6510());
        cpuBus = new C64Bus(ram, basic, vic, builder.sid, colorRam, cia1, cia2, charset, kernel, vicBus.openBus());
        cpuBus.connect(cpu.getPort());
        cpu.connect(cpuBus);
        cpu.getIRQ().connect(cia1.getIRQ());
        cpu.getIRQ().connect(vic.getIRQ());
        cpu.getNMI().connect(cia2.getIRQ());
        cpu.getNMI().connect(keyboard.getNMI());
        cpu.getRDY().connect(vic.getBA());

        keyboardBuffer = new KeyboardBuffer(clock, cpuBus);

        // real time measurement
        realTimeSlowDown = builder.realTime ? new RealTimeSlowDown(clock, videoSystem.cpuFrequency(), 100) : null;

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

    /**
     * Builder for a C64.
     */
    public static class Builder {
        /**
         * Main clock.
         */
        private final Clock clock;

        /**
         * Use debugger cpu?
         */
        private boolean debug = false;

        /**
         * Video system.
         */
        private VideoSystem videoSystem = VideoSystem.PAL;

        /**
         * Use new CIAs (6526A) instead of old CIAs (6526)?
         */
        private boolean newCias = false;

        /**
         * Frequency of the power line in Hz, 0 for the frequency of the video system.
         */
        private int powerLineFrequency = 0;

        /**
         * Device in the SID area $D400-$D7FF.
         */
        private BusDevice sid = new SIDStub();

        /**
         * Slow down the emulation to real time?
         */
        private boolean realTime = true;

        /**
         * Constructor.
         *
         * @param clock
         *         Clock.
         */
        private Builder(Clock clock) {
            this.clock = clock;
        }

        /**
         * Use debugger cpu? Default: no.
         */
        public Builder debug(boolean debug) {
            this.debug = debug;
            return this;
        }

        /**
         * Video system. Default: PAL.
         */
        public Builder videoSystem(VideoSystem videoSystem) {
            this.videoSystem = videoSystem;
            return this;
        }

        /**
         * Use old CIAs (6526), as in the breadbin C64. Default.
         */
        public Builder oldCias() {
            return newCias(false);
        }

        /**
         * Use new CIAs (6526A), as in the C64C.
         */
        public Builder newCias() {
            return newCias(true);
        }

        /**
         * Use new CIAs (6526A) instead of old CIAs (6526)? Default: no.
         */
        public Builder newCias(boolean newCias) {
            this.newCias = newCias;
            return this;
        }

        /**
         * Frequency of the power line in Hz, e.g. 50 or 60, which drives the TOD clocks of the CIAs.
         * Default: The frequency of the video system, 50 Hz for PAL and 60 Hz for NTSC.
         */
        public Builder powerLineFrequency(int powerLineFrequency) {
            assert powerLineFrequency > 0 : "powerLineFrequency > 0";
            this.powerLineFrequency = powerLineFrequency;
            return this;
        }

        /**
         * Device in the SID area $D400-$D7FF, e.g. a debug cartridge. Default: SID stub.
         */
        public Builder sid(BusDevice sid) {
            this.sid = sid;
            return this;
        }

        /**
         * Slow down the emulation to real time? Default: yes.
         */
        public Builder realTime(boolean realTime) {
            this.realTime = realTime;
            return this;
        }

        /**
         * Build the C64.
         */
        public C64 build() throws Exception {
            return new C64(this);
        }

        /**
         * Frequency of the power line in Hz.
         */
        private int powerLineFrequency() {
            return powerLineFrequency > 0 ? powerLineFrequency : videoSystem.todInput().frequency();
        }

        /**
         * Create a CIA.
         */
        private AbstractCIA createCia(TodInput todInput) {
            return newCias ? new CIA6526A(todInput) : new CIA6526(todInput);
        }
    }
}
