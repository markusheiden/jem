package de.heiden.jem.models.c64.components;

import de.heiden.jem.components.bus.BusDevice;
import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.components.clock.ClockEvent;
import de.heiden.jem.components.clock.serialthreads.SerialClock;
import de.heiden.jem.models.c64.components.cia.CIA6526;
import de.heiden.jem.models.c64.components.cpu.C64Bus;
import de.heiden.jem.models.c64.components.cpu.CPU6510Debugger;
import de.heiden.jem.models.c64.components.cpu.Patch;
import de.heiden.jem.models.c64.components.keyboard.Keyboard;
import de.heiden.jem.models.c64.components.memory.ColorRAM;
import de.heiden.jem.models.c64.components.memory.RAM;
import de.heiden.jem.models.c64.components.memory.ROMLoader;
import de.heiden.jem.models.c64.components.patch.BrkDetector;
import de.heiden.jem.models.c64.components.patch.ProgramEndDetector;
import de.heiden.jem.models.c64.components.patch.Return;
import de.heiden.jem.models.c64.components.patch.StopAtSystemIn;
import de.heiden.jem.models.c64.components.patch.SystemOut;
import de.heiden.jem.models.c64.components.vic.VIC6569PAL;
import de.heiden.jem.models.c64.components.vic.VICBus;
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
    private final Clock clock;

    /**
     * CPU bus.
     */
    private C64Bus cpuBus;

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
    private final VIC6569PAL vic;

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
        clock = new SerialClock();

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

        cpu = clock.addClockedComponent(Clock.CPU, new CPU6510Debugger());
        cpuBus = new C64Bus(ram, basic, vic, colorRam, cia1, cia2, charset, kernel);
        cpuBus.connect(cpu.getPort());
        cpu.connect(cpuBus);
        cpu.getIRQ().connect(cia1.getIRQ());
        cpu.getIRQ().connect(vic.getIRQ());
        cpu.getNMI().connect(cia2.getIRQ());
        cpu.getNMI().connect(keyboard.getNMI());

        // init RAM with 0x02 (crash) to easier detect wrong behaviour
        for (int addr = 0; addr < 0x10000; addr++) {
            cpuBus.write(0x02, addr);
        }

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
     * Get bus.
     */
    public BusDevice getBus() {
        return cpuBus;
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
