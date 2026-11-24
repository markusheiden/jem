package de.heiden.jem.models.c64;

import de.heiden.c64dt.assembler.CodeBuffer;
import de.heiden.c64dt.assembler.Disassembler;
import de.heiden.c64dt.assembler.Dumper;
import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.components.clock.serialthreads.SerialClock;
import de.heiden.jem.models.c64.AbstractTest.TestWatcher;
import de.heiden.jem.models.c64.components.TestC64;
import de.heiden.jem.models.c64.components.VideoSystem;
import de.heiden.jem.models.c64.components.cpu.CPU6510;
import de.heiden.jem.models.c64.components.memory.RAM;
import de.heiden.jem.models.c64.components.patch.LoadFile;
import de.heiden.jem.models.c64.components.patch.LoadFromDirectory;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.serialthreads.agent.Transform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static de.heiden.c64dt.bytes.HexUtil.hexByte;
import static de.heiden.c64dt.bytes.HexUtil.hexWord;
import static java.lang.Thread.sleep;
import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.join;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test support.
 */
@Timeout(120)
@ExtendWith(TestWatcher.class)
@Transform(classPrefixes = "de.heiden.jem")
public abstract class AbstractTest {
    /**
     * Logger.
     */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * VIC border color.
     */
    protected static final int BORDER = 0xD020;

    /**
     * Debug cartridge register, test programs write their result to.
     */
    protected static final int DEBUG_CARTRIDGE = 0xD7FF;

    /**
     * Color green.
     */
    protected static final int RED = 0x02;

    /**
     * Color green.
     */
    protected static final int GREEN = 0x05;

    /**
     * Color red.
     */
    protected static final int LIGHT_RED = 0x0A;

    /**
     * Tick at which the load command is typed. The boot has finished by then.
     * The load and the run command are typed at fixed ticks, so that a test is reproducible:
     * Its result does not depend on the speed of the test thread.
     */
    private static final long LOAD_TICK = 3000000;

    /**
     * Tick at which the run command is typed.
     */
    private static final long RUN_TICK = 4000000;

    /**
     * Cycles to delay the run command, 0 types it at the regular position of the raster beam.
     * Changes the phase of the start of the program relative to the raster beam.
     */
    protected int runDelay = 0;

    /**
     * Video system of the test C64.
     */
    protected VideoSystem videoSystem = VideoSystem.PAL;

    /**
     * Transformed test C64.
     */
    @SuppressWarnings("NullAway.Init")
    protected TestC64 c64;

    /**
     * Use new CIAs (6526A) instead of old CIAs (6526)?
     */
    protected boolean newCias = false;

    /**
     * Console output of test C64.
     */
    protected final ConsoleBuffer console = new ConsoleBuffer();

    /**
     * Keyboard input for test C64.
     */
    @SuppressWarnings("NullAway.Init")
    private KeyListener systemIn;

    /**
     * Thread to run test C64.
     */
    @SuppressWarnings("NullAway.Init")
    protected Thread thread;

    /**
     * Throwable from C64 thread, if any.
     */
    private volatile @Nullable Throwable throwable;

    /**
     * Program.
     */
    @SuppressWarnings("NullAway.Init")
    private byte[] bytes;

    /**
     * Video system a test program is made for, by the naming convention of the VICE test programs:
     * "ntscold" for the old NTSC VIC 6567R56A, "ntsc" for the NTSC VIC 6567R8, PAL otherwise.
     */
    protected static VideoSystem videoSystemOf(String programName) {
        return programName.contains("ntscold") ? VideoSystem.NTSC_OLD :
                programName.contains("ntsc") ? VideoSystem.NTSC : VideoSystem.PAL;
    }

    /**
     * Create test C64.
     *
     * @param threadName
     *         Name C64 thread.
     */
    protected void createC64(String threadName) throws Exception {
        c64 = new TestC64(newCias, videoSystem);
        c64.setSystemOut(console);
        systemIn = c64.getSystemIn();

        thread = new Thread(() -> {
            try {
                c64.start();
            } catch (Throwable t) {
                AbstractTest.this.throwable = t;
            }
        }, threadName);
        // Never block JVM termination, if the emulation does not stop.
        thread.setDaemon(true);
        thread.start();

        // Reset the program end flag.
        c64.hasEnded();

        // Wait for the boot to finish.
        assertThat(waitSecondsFor(3, programEnd)).isSameAs(programEnd);
        // The program end is detected before "READY." is printed.
        // Wait for it, so that it is not mistaken for the "READY." after the load.
        assertThat(waitSecondsFor(1, ready)).isSameAs(ready);

        // Reset the program end flag.
        c64.hasEnded();

        console.clear();
    }

    /**
     * Convert classpath resource to {@link Path}.
     */
    protected Path path(String classpath) throws Exception {
        var url = getClass().getResource(classpath);
        assertThat(url).as("Classpath resource " + classpath + " exists").isNotNull();
        return Paths.get(url.toURI());
    }

    /**
     * Load program relative.
     *
     * @param programName
     *         Name of program, without suffix.
     * @param device
     *         Device to load from.
     */
    protected void load(String programName, int device) throws Exception {
        load(programName, device, 0);
    }

    /**
     * Load program absolute.
     *
     * @param programName
     *         Name of program, without suffix.
     * @param device
     *         Device address to load from.
     * @param secondary
     *         Secondary address.
     */
    protected void load(String programName, int device, int secondary) throws Exception {
        // Reset the program end flag.
        c64.hasEnded();

        console.clear();
        c64.getKeyboardBuffer().type("load\"" + programName + "\"," + device + "," + secondary + "\n", LOAD_TICK);
        waitCycles(LOAD_TICK - getTick());
        assertThat(waitSecondsFor(10, ready)).isSameAs(ready);
    }

    /**
     * Run the program.
     */
    protected void run() throws Exception {
        // Reset the program end flag.
        c64.hasEnded();

        console.clear();
        if (runDelay == 0) {
            c64.getKeyboardBuffer().type("run\n", RUN_TICK);
        } else {
            c64.getKeyboardBuffer().typeAt("run\n", RUN_TICK + runDelay);
        }
        waitCycles(RUN_TICK - getTick());
    }

    /**
     * Load and run the program, evaluate border color to determine the test result.
     *
     * @param program
     *         Path to program.
     * @param maxSeconds
     *         Max seconds to wait. Assumes 1 MHz clock.
     * @param screenCapture
     *         Capture screen and print it to {@link System#out}?.
     */
    protected void testBorderResult(Path program, int maxSeconds, boolean screenCapture) throws Exception {
        loadAndRun(program);
        doTestBorderResult(maxSeconds, screenCapture);
    }

    /**
     * Load and run the program, evaluate border color to determine the test result.
     *
     * @param programName
     *         Name of program.
     * @param program
     *         Program.
     * @param maxSeconds
     *         Max seconds to wait. Assumes 1 MHz clock.
     * @param screenCapture
     *         Capture screen and print it to {@link System#out}?.
     */
    protected void testBorderResult(String programName, byte[] program, int maxSeconds, boolean screenCapture)
            throws Exception {
        loadAndRun(programName, program);
        doTestBorderResult(maxSeconds, screenCapture);
    }

    /**
     * Load and run the program, evaluate just the debug cartridge to determine the test result.
     * For test programs, which use the border for other purposes while running.
     *
     * @param program
     *         Path to program.
     * @param maxSeconds
     *         Max seconds to wait. Assumes 1 MHz clock.
     */
    protected void testDebugCartridgeResult(Path program, int maxSeconds) throws Exception {
        loadAndRun(program);
        var result = waitSecondsFor(maxSeconds, debugCartridgePassed, debugCartridgeFailed);
        assertThat(result).as("Test failed").isSameAs(debugCartridgePassed);
    }

    /**
     * Run a test program on just the CPU with 64 KB RAM, which is much faster than on a complete C64.
     * For test programs, which need just the CPU and run for a long time.
     * Writes to the I/O area end up in the RAM, so the result is read from the RAM of the debug cartridge register.
     * The pointer to the start of BASIC points to the load address, CHROUT just returns.
     *
     * @param program
     *         Path to the program file.
     * @param start
     *         Start address of the program.
     * @param maxSeconds
     *         Max seconds to run. Assumes 1 MHz clock.
     */
    protected static void testCpuResult(Path program, int start, int maxSeconds) throws Exception {
        var bytes = Files.readAllBytes(program);

        try (var clock = new SerialClock()) {
            var ram = new RAM(0x10000);
            var cpu = clock.addClockedComponent(Clock.CPU, new CPU6510());
            cpu.connect(ram);

            // Load the program.
            var loadAddress = bytes[0] & 0xFF | (bytes[1] & 0xFF) << 8;
            for (var i = 2; i < bytes.length; i++) {
                ram.write(bytes[i] & 0xFF, loadAddress + i - 2);
            }
            write(ram, 0x002B, loadAddress & 0xFF, loadAddress >> 8);
            // CHROUT: RTS.
            write(ram, 0xFFD2, 0x60);
            // Reset: Start the program.
            write(ram, 0xFFFC, start & 0xFF, start >> 8);
            // No result yet. The program writes $00 for passed and $FF for failed.
            var noResult = 0x80;
            write(ram, DEBUG_CARTRIDGE, noResult);

            for (var end = maxSeconds * 1000000L; ram.read(DEBUG_CARTRIDGE) == noResult && clock.getTick() < end; ) {
                clock.run(10000000);
            }

            assertThat(ram.read(DEBUG_CARTRIDGE)).as("Debug cartridge").isEqualTo(0x00);
        }
    }

    /**
     * Write bytes to the RAM.
     */
    private static void write(RAM ram, int addr, int... bytes) {
        for (var i = 0; i < bytes.length; i++) {
            ram.write(bytes[i], addr + i);
        }
    }

    /**
     * Load the program from the classpath and start it via "run".
     *
     * @param program
     *         Where to find program in classpath.
     */
    protected void loadAndRun(String program) throws Exception {
        loadAndRun(path(program));
    }

    /**
     * Load program from {@link Path} and start it via "run".
     *
     * @param program
     *         Path to the program file.
     */
    protected void loadAndRun(Path program) throws Exception {
        // Extract pure file name.
        var programName = program.getFileName().toString();
        programName = programName.substring(0, programName.indexOf(".prg"));

        createC64(programName);
        c64.add(new LoadFromDirectory(requireNonNull(program.getParent())));
        doLoadAndRun(programName, Files.readAllBytes(program));
    }

    /**
     * Load the given program and start it via "run".
     *
     * @param programName
     *         Name of program.
     * @param program
     *         Program.
     */
    protected void loadAndRun(String programName, byte[] program) throws Exception {
        createC64(programName);
        c64.add(new LoadFile(program));
        doLoadAndRun(programName, program);
    }

    /**
     * Load the program and start it via "run".
     *
     * @param programName
     *         Name of program.
     * @param bytes
     *         Program.
     */
    private void doLoadAndRun(String programName, byte[] bytes) throws Exception {
        this.bytes = bytes;

        // Load program.
        load(programName, 8);
        // Skip further loads.
        c64.rts(0xE16F);

        run();
    }

    /**
     * Evaluate border color to determine test result.
     *
     * @param maxSeconds
     *         Max seconds to wait. Assumes 1 MHz clock.
     * @param screenCapture
     *         Capture screen and print it to {@link System#out}?.
     */
    protected final void doTestBorderResult(int maxSeconds, boolean screenCapture) throws Exception {
        // Test programs may change the border temporarily while running, so the border color has to be stable.
        var passed = stable(greenBorder);
        var failed1 = stable(lightRedBorder);
        var failed2 = stable(redBorder);
        var result = waitSecondsFor(maxSeconds, debugCartridgePassed, debugCartridgeFailed, passed, failed1, failed2);
        if (result == programEnd && greenBorder.test()) {
            // The border does not change anymore after the end of the program.
            result = passed;
        }
        if (screenCapture) {
            printScreen();
        }

        if (result != debugCartridgePassed) {
            assertThat(result).as("Test failed").isSameAs(passed);
        }
    }

    static class TestWatcher implements TestExecutionExceptionHandler {
        @Override
        public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
            try {
                var test = (AbstractTest) context.getRequiredTestInstance();
                test.dumpProgram();
            } catch (Throwable e) {
                throwable.addSuppressed(e);
            }
            throw throwable;
        }
    }

    /**
     * For debugging purposes, disassemble the test program.
     */
    public void dumpProgram() throws IOException {
        System.out.flush();
        System.out.println();
        System.out.println();
        new Disassembler().listAndDisassemble(CodeBuffer.fromProgram(bytes), new PrintWriter(System.out));
        System.out.println();
        new Dumper().dump(CodeBuffer.fromProgram(bytes), new PrintWriter(System.out));
        System.out.println();
        System.out.flush();
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (c64 != null) {
            c64.setSystemOut(null);
            c64.stop();
            thread.interrupt();
            thread.join(1000);
            assertThat(thread.isAlive()).as("C64 thread has been stopped").isFalse();
        }
    }

    /**
     * Type string to the keyboard.
     *
     * @param s
     *         String.
     */
    protected void type(String s) throws Exception {
        var dummySource = new Button();
        for (var c : s.toCharArray()) {
            var event = new KeyEvent(dummySource, 0, 0, 0, 0, c, 0);
            systemIn.keyPressed(event);
            waitCycles(21000); // Wait at least one interrupt.
            systemIn.keyReleased(event);
            waitCycles(21000); // Wait at least one interrupt.
        }
    }

    /**
     * Capture screen at $0400 and print it to {@link System#out}.
     */
    public void printScreen() throws Exception {
        System.out.println(captureScreen());
    }

    /**
     * Capture screen at $0400.
     */
    public String captureScreen() throws Exception {
        return new ScreenBuffer(c64.getBus()).capture();
    }

    /**
     * Wait for a condition to happen.
     *
     * @param maxSeconds
     *         Max seconds to wait. Assumes 1 MHz clock.
     * @param conditions
     *         Conditions.
     * @return Condition that met or null, if timeout.
     */
    protected @Nullable Condition waitSecondsFor(int maxSeconds, Condition... conditions) throws Exception {
        return waitCyclesFor(maxSeconds * 1000000L, conditions);
    }

    /**
     * Wait for a string to appear on screen.
     *
     * @param maxCycles
     *         Max cycles to wait.
     * @param conditions
     *         Conditions.
     * @return Condition that met or null, if timeout.
     */
    protected @Nullable Condition waitCyclesFor(long maxCycles, Condition... conditions) throws Exception {
        for (long start = getTick(), end = start + maxCycles; ; ) {
            // Test the program end first, because the C64 is still running:
            // Otherwise a condition met just before the program end may be missed.
            var ended = programEnd.test();

            for (var condition : conditions) {
                if (condition.test()) {
                    messageTimeSince(condition.toString(), start);
                    return condition;
                }
            }

            if (ended) {
                messageTimeSince("Program end", start);
                return programEnd;
            }

            if (getTick() >= end) {
                messageTimeSince("No match", start);
                return null;
            }

            if (throwable != null) {
                messageTimeSince("Exception", start);
                // Abort on throwable from C64 thread.
                throw new AssertionError(throwable);
            }

            waitCycles(10000);
        }
    }

    /**
     * Log debug message including the time since the given start timestamp.
     */
    private void messageTimeSince(String message, long start) throws Exception {
        // First flush system out.
        System.out.println();
        System.out.flush();

        // Log debug message.
        var ticks = getTick() - start;
        log.debug("{} after ~ {} seconds ({} ticks)", message, ticks / 1000000, ticks);
    }

    /**
     * Wait the given number of clock cycles.
     *
     * @param seconds
     *         Seconds to wait. Assumes 1 MHz clock.
     */
    protected void waitSeconds(int seconds) throws Exception {
        waitCycles(seconds * 1000000L);
    }

    /**
     * Wait the given number of clock cycles.
     *
     * @param cycles
     *         Cycles.
     */
    protected void waitCycles(long cycles) throws Exception {
        for (var end = getTick() + cycles; getTick() < end; ) {
            if (throwable != null) {
                // Early exit on exceptions, because the emulation has been halted.
                return;
            }

            sleep(10);
        }
    }

    /**
     * Get the current clock tick.
     */
    private Long getTick() throws Exception {
        return c64.getClock().getTick();
    }

    //
    // Conditions.
    //

    /**
     * Program end.
     */
    protected final Condition programEnd = new Condition() {
        @Override
        public boolean test() {
            return c64.hasEnded();
        }

        @Override
        public String toString() {
            return "Program end";
        }
    };

    /**
     * BRK.
     */
    protected final Condition brk = new Condition() {
        @Override
        public boolean test() {
            return c64.hasBrk();
        }

        @Override
        public String toString() {
            return "BRK";
        }
    };

    /**
     * Wait for "READY." on the console.
     */
    private final Condition ready = onConsole("ready.\n");

    /**
     * Condition "text on console".
     */
    protected Condition onConsole(String... texts) {
        return new OnConsole(texts);
    }

    /**
     * Search for text in the console.
     */
    private class OnConsole implements Condition {
        /**
         * Text.
         */
        private final String[] texts;

        /**
         * Constructor.
         *
         * @param texts
         *         Texts.
         */
        public OnConsole(String... texts) {
            this.texts = texts;
        }

        @Override
        public boolean test() {
            return stream(texts).anyMatch(console::contains);
        }

        @Override
        public String toString() {
            return "Console contains " + join(texts, " or ");
        }
    }

    /**
     * Condition "text on screen".
     */
    protected Condition onScreen(String... texts) {
        return new OnScreen(texts);
    }

    /**
     * Search for texts in the console.
     */
    private final class OnScreen implements Condition {
        /**
         * Text.
         */
        private final String[] texts;

        /**
         * Constructor.
         *
         * @param texts
         *         Texts.
         */
        public OnScreen(String... texts) {
            this.texts = texts;
        }

        @Override
        public boolean test() throws Exception {
            var screen = captureScreen();
            return stream(texts).anyMatch(screen::contains);
        }

        @Override
        public String toString() {
            return "Screen contains " + join(texts, " or ");
        }
    }

    /**
     * Green border. Usually "test passed".
     */
    protected final Condition greenBorder = inVic(BORDER, GREEN);

    /**
     * Light-red border. Usually "test failed".
     */
    protected final Condition lightRedBorder = inVic(BORDER, LIGHT_RED);

    /**
     * Red border.  Usually "test failed".
     */
    protected final Condition redBorder = inVic(BORDER, RED);

    /**
     * Debug cartridge reports "test passed".
     */
    protected final Condition debugCartridgePassed = debugCartridge(0x00);

    /**
     * Debug cartridge reports "test failed".
     */
    protected final Condition debugCartridgeFailed = debugCartridge(0xFF);

    /**
     * Condition, which has to be met at two consecutive tests.
     */
    protected Condition stable(Condition condition) {
        return new Stable(condition);
    }

    /**
     * Condition, which has to be met at two consecutive tests.
     */
    private static final class Stable implements Condition {
        /**
         * Condition.
         */
        private final Condition condition;

        /**
         * Has the condition been met at the last test?.
         */
        private boolean met = false;

        /**
         * Constructor.
         *
         * @param condition
         *         Condition.
         */
        public Stable(Condition condition) {
            this.condition = condition;
        }

        @Override
        public boolean test() throws Exception {
            var metBefore = met;
            met = condition.test();
            return metBefore && met;
        }

        @Override
        public String toString() {
            return condition.toString();
        }
    }

    /**
     * Condition "value in memory".
     */
    protected Condition inMemory(int addr, int value) {
        return new InMemory(addr, value);
    }

    /**
     * Condition "color in VIC color register". The unused upper 4 bits of the register are ignored.
     * Independent of the memory configuration, e.g. when I/O is banked out.
     */
    protected Condition inVic(int addr, int value) {
        return new Condition() {
            @Override
            public boolean test() {
                return (c64.getVic().read(addr) & 0x0F) == value;
            }

            @Override
            public String toString() {
                return "VIC " + hexWord(addr) + " == " + hexByte(value);
            }
        };
    }

    /**
     * Condition "result written to the debug cartridge".
     */
    protected Condition debugCartridge(int value) {
        return new Condition() {
            @Override
            public boolean test() {
                return c64.getDebugCartridge().getResult() == value;
            }

            @Override
            public String toString() {
                return "Debug cartridge == " + hexByte(value);
            }
        };
    }

    /**
     * Wait for value in memory.
     */
    private final class InMemory implements Condition {
        /**
         * Address to monitor.
         */
        private final int addr;

        /**
         * Value to wait for.
         */
        private final int value;

        /**
         * Constructor.
         *
         * @param addr
         *         Address to monitor.
         * @param value
         *         Value to wait for.
         */
        public InMemory(int addr, int value) {
            this.addr = addr;
            this.value = value;
        }

        @Override
        public boolean test() {
            return c64.getBus().read(addr) == value;
        }

        @Override
        public String toString() {
            return "Memory " + hexWord(addr) + " == " + hexByte(value);
        }
    }
}
