package de.heiden.jem.models.c64.components;

import de.heiden.jem.components.bus.BusDevice;
import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.components.clock.ClockEvent;
import de.heiden.jem.components.clock.serialthreads.SerialClock;
import de.heiden.jem.models.c64.components.cpu.C64Bus;
import de.heiden.jem.models.c64.components.cpu.CPU6510Debugger;
import de.heiden.jem.models.c64.components.cpu.Patch;
import de.heiden.jem.models.c64.components.keyboard.Keyboard;
import de.heiden.jem.models.c64.components.keyboard.KeyboardBuffer;
import de.heiden.jem.models.c64.components.patch.BrkDetector;
import de.heiden.jem.models.c64.components.patch.ProgramEndDetector;
import de.heiden.jem.models.c64.components.patch.Return;
import de.heiden.jem.models.c64.components.patch.StopAtSystemIn;
import de.heiden.jem.models.c64.components.patch.SystemOut;
import de.heiden.jem.models.c64.components.sid.SIDStub;
import de.heiden.jem.models.c64.components.vic.AbstractDisplayUnit;
import de.heiden.jem.models.c64.components.vic.VIC;
import de.heiden.jem.models.c64.gui.swing.emulator.KeyListener;
import de.heiden.jem.models.c64.gui.swing.emulator.PCMapping;

import java.io.OutputStream;

import static java.lang.Thread.interrupted;

/**
 * Modified C64 for better testability.
 */
public class TestC64 {
    /**
     * Main clock.
     */
    private final SerialClock clock;

    /**
     * CPU bus.
     */
    private C64Bus cpuBus;

    /**
     * Keyboard buffer of the KERNAL.
     */
    private final KeyboardBuffer keyboardBuffer;

    /**
     * CPU.
     */
    private final CPU6510Debugger cpu;

    /**
     * Keyboard.
     */
    private Keyboard keyboard;

    /**
     * VIC.
     */
    private final VIC vic;

    /**
     * Debug cartridge.
     */
    private final DebugCartridge debugCartridge;

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
     * Constructor with old CIAs.
     */
    public TestC64() throws Exception {
        this(false);
    }

    /**
     * Constructor.
     *
     * @param newCias
     *         Use new CIAs (6526A) instead of old CIAs (6526)?
     */
    public TestC64(boolean newCias) throws Exception {
        this(newCias, VideoSystem.PAL);
    }

    /**
     * Constructor.
     *
     * @param newCias
     *         Use new CIAs (6526A) instead of old CIAs (6526)?
     * @param videoSystem
     *         video system
     */
    public TestC64(boolean newCias, VideoSystem videoSystem) throws Exception {
        clock = new SerialClock();
        debugCartridge = new DebugCartridge(new SIDStub(clock));
        var c64 = C64.builder(clock)
                .videoSystem(videoSystem)
                .newCias(newCias)
                .debug(true)
                .sid(debugCartridge)
                .realTime(false)
                .build();

        cpuBus = c64.getCpuBus();
        keyboardBuffer = c64.getKeyboardBuffer();
        cpu = (CPU6510Debugger) c64.getCpu();
        keyboard = c64.getKeyboard();
        vic = c64.getVIC();

        //
        // ROM patches
        //

        cpu.add(systemOut);
        cpu.add(new StopAtSystemIn());
        cpu.add(programEnd);

        clock.addClockEvent(100000, new ClockEvent("Interrupt check") {
            @Override
            public void execute(long tick) {
                if (interrupted()) {
                    throw new IllegalArgumentException("Thread has been interrupted");
                }

                clock.addClockEvent(tick + 100000, this);
            }
        });
    }

    /**
     * Add patch.
     */
    public void add(Patch patch) {
        cpu.add(patch);
    }

    /**
     * Clock.
     */
    public Clock getClock() {
        return clock;
    }

    /**
     * Keyboard buffer of the KERNAL.
     */
    public KeyboardBuffer getKeyboardBuffer() {
        return keyboardBuffer;
    }

    /**
     * Get bus.
     */
    public BusDevice getBus() {
        return cpuBus;
    }

    /**
     * VIC, to read its registers independent of the memory configuration.
     */
    public BusDevice getVic() {
        return vic;
    }

    /**
     * Display unit of the VIC, to capture the rendered screen.
     */
    public AbstractDisplayUnit getDisplayUnit() {
        return vic.displayUnit;
    }

    /**
     * CPU.
     */
    public CPU6510Debugger getCpu() {
        return cpu;
    }

    /**
     * Debug cartridge.
     */
    public DebugCartridge getDebugCartridge() {
        return debugCartridge;
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
        return new KeyListener(keyboard, new PCMapping());
    }

    /**
     * Add a patch to the cpu, to insert a RTS at the given address.
     *
     * @param addr
     *         Address to write RTS to
     */
    public void rts(int addr) {
        cpu.add(new Return(addr));
    }

    /**
     * Start emulation.
     */
    public void start() {
        clock.run();
        clock.close();
    }

    /**
     * Request the emulation to stop.
     * May be called from any thread.
     */
    public void stop() {
        clock.stop();
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
