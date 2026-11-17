package de.heiden.jem.models.c64.components;

import de.heiden.jem.components.bus.BusDevice;
import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.components.clock.RealTimeSlowDown;
import de.heiden.jem.models.c64.components.cia.AbstractCIA;
import de.heiden.jem.models.c64.components.cia.CIA6526;
import de.heiden.jem.models.c64.components.cia.CIA6526A;
import de.heiden.jem.models.c64.components.cia.TodInput;
import de.heiden.jem.models.c64.components.cpu.CPU6510;
import de.heiden.jem.models.c64.components.cpu.CPU6510Debugger;
import de.heiden.jem.models.c64.components.memory.ColorRAM;
import de.heiden.jem.models.c64.components.memory.RAM;
import de.heiden.jem.models.c64.components.memory.ROMLoader;
import de.heiden.jem.models.c64.components.sid.SIDStub;
import de.heiden.jem.models.c64.components.vic.VICBus;

/**
 * Builder for a C64, by default a PAL C64 with old CIAs.
 */
public class C64Builder {
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
    C64Builder(Clock clock) {
        this.clock = clock;
    }

    /**
     * Use debugger cpu? Default: no.
     */
    public C64Builder debug(boolean debug) {
        this.debug = debug;
        return this;
    }

    /**
     * Video system. Default: PAL.
     */
    public C64Builder videoSystem(VideoSystem videoSystem) {
        this.videoSystem = videoSystem;
        return this;
    }

    /**
     * Use old CIAs (6526), as in the breadbin C64. Default.
     */
    public C64Builder oldCias() {
        return newCias(false);
    }

    /**
     * Use new CIAs (6526A), as in the C64C.
     */
    public C64Builder newCias() {
        return newCias(true);
    }

    /**
     * Use new CIAs (6526A) instead of old CIAs (6526)? Default: no.
     */
    public C64Builder newCias(boolean newCias) {
        this.newCias = newCias;
        return this;
    }

    /**
     * Frequency of the power line in Hz, e.g. 50 or 60, which drives the TOD clocks of the CIAs.
     * Default: The frequency of the video system, 50 Hz for PAL and 60 Hz for NTSC.
     */
    public C64Builder powerLineFrequency(int powerLineFrequency) {
        assert powerLineFrequency > 0 : "powerLineFrequency > 0";
        this.powerLineFrequency = powerLineFrequency;
        return this;
    }

    /**
     * Device in the SID area $D400-$D7FF, e.g. a debug cartridge. Default: SID stub.
     */
    public C64Builder sid(BusDevice sid) {
        this.sid = sid;
        return this;
    }

    /**
     * Slow down the emulation to real time? Default: yes.
     */
    public C64Builder realTime(boolean realTime) {
        this.realTime = realTime;
        return this;
    }

    /**
     * Build the C64.
     */
    public C64 build() throws Exception {
        var todInput = videoSystem.todInput().withFrequency(powerLineFrequency());

        var ram = new RAM(0x10000);
        var colorRam = new ColorRAM(0x400);
        var basic = ROMLoader.basic(ROMLoader.DEFAULT_BASIC);
        var kernel = ROMLoader.kernel(ROMLoader.DEFAULT_KERNEL);
        var charset = ROMLoader.character(ROMLoader.DEFAULT_CHARACTER);

        var cia1 = createCia(todInput);
        var cia2 = createCia(todInput);

        var vicBus = new VICBus(cia2.portA(), ram, charset);
        var vic = videoSystem.createVic(clock, vicBus, colorRam);

        var cpu = debug ? new CPU6510Debugger() : new CPU6510();

        // real time measurement
        var realTimeSlowDown = realTime ? new RealTimeSlowDown(clock, videoSystem.cpuFrequency(), 100) : null;

        return new C64(clock, ram, colorRam, basic, kernel, charset, cia1, cia2, vicBus, vic, sid, cpu, realTimeSlowDown);
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
