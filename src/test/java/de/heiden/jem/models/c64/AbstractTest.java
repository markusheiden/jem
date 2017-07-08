package de.heiden.jem.models.c64;

import de.heiden.c64dt.assembler.Disassembler;
import de.heiden.c64dt.assembler.Dumper;
import de.heiden.c64dt.bytes.HexUtil;
import de.heiden.c64dt.reassembler.CodeBuffer;
import de.heiden.jem.models.c64.components.TestC64;
import de.heiden.jem.models.c64.components.patch.LoadFile;
import de.heiden.jem.models.c64.components.patch.LoadFromDirectory;
import org.junit.After;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.serialthreads.agent.Transform;
import org.serialthreads.agent.TransformingRunner;
import org.serialthreads.transformer.strategies.frequent3.FrequentInterruptsTransformer3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

/**
 * Test support.
 */
@RunWith(TransformingRunner.class)
@Transform(transformer = FrequentInterruptsTransformer3.class, classPrefixes = "de.heiden.jem")
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
   * Transformed test C64.
   */
  protected TestC64 c64;

  /**
   * Console output of test C64.
   */
  protected final ConsoleBuffer console = new ConsoleBuffer();

  /**
   * Keyboard input for test C64.
   */
  private KeyListener systemIn;

  /**
   * Thread to run test C64.
   */
  protected Thread thread;

  /**
   * Throwable from C64 thread, if any.
   */
  private volatile Throwable throwable;

  /**
   * Program.
   */
  private byte[] bytes;

  /**
   * Create test C64.
   *
   * @param threadName Name C64 thread.
   */
  protected void createC64(String threadName) throws Exception {
    c64 = new TestC64();
    c64.setSystemOut(console);
    systemIn = c64.getSystemIn();

    thread = new Thread(() -> {
      try {
        c64.start();
      } catch (Throwable t) {
        AbstractTest.this.throwable = t;
      }
    }, threadName);
    thread.start();

    // Reset program end flag.
    c64.hasEnded();

    // Wait for boot to finish.
    assertSame(programEnd, waitSecondsFor(3, programEnd));

    // Reset program end flag.
    c64.hasEnded();

    console.clear();
  }

  /**
   * Convert classpath resource to {@link Path}.
   */
  protected Path path(String classpath) throws Exception {
    URL url = getClass().getResource(classpath);
    assertNotNull("Classpath resource " + classpath + " exists", url);
    return Paths.get(url.toURI());
  }

  /**
   * Load program relative.
   *
   * @param programName Name of program, without suffix.
   * @param device Device to load from.
   */
  protected void load(String programName, int device) throws Exception {
    load(programName, device, 0);
  }

  /**
   * Load program absolute.
   *
   * @param programName Name of program, without suffix.
   * @param device Device address to load from.
   * @param secondary Secondary address.
   */
  protected void load(String programName, int device, int secondary) throws Exception {
    // Reset program end flag.
    c64.hasEnded();

    console.clear();
    type("load\"" + programName + "\"," + device + "," + secondary + "\n");
    assertSame(ready, waitSecondsFor(10, ready));
  }

  /**
   * Run program.
   */
  protected void run() throws Exception {
    // Reset program end flag.
    c64.hasEnded();

    console.clear();
    type("run\n");
  }

  /**
   * Load program from classpath and start it via "run".
   *
   * @param program Where to find program in classpath.
   */
  protected void loadAndRun(String program) throws Exception {
    loadAndRun(path(program));
  }

  /**
   * Load program from {@link Path} and start it via "run".
   *
   * @param program Path to program file.
   */
  protected void loadAndRun(Path program) throws Exception {
    // Extract pure file name.
    String programName = program.getFileName().toString();
    programName = programName.substring(0, programName.indexOf(".prg"));

    createC64(programName);
    c64.add(new LoadFromDirectory(program.getParent()));
    doLoadAndRun(programName, Files.readAllBytes(program));
  }

  /**
   * Load the given program and start it via "run".
   *
   * @param programName Name of program.
   * @param program Program.
   */
  protected void loadAndRun(String programName, byte[] program) throws Exception {
    createC64(programName);
    c64.add(new LoadFile(program));
    doLoadAndRun(programName, program);
  }

  /**
   * Load program and start it via "run".
   *
   * @param programName Name of program.
   * @param bytes Program.
   */
  private void doLoadAndRun(String programName, byte[] bytes) throws Exception {
    this.bytes = bytes;

    // Load program.
    load(programName, 8);
    // Skip further loads
    c64.rts(0xE16F);

    run();
  }

  /**
   * Evaluate border color to determine test result.
   *
   * @param maxSeconds Max seconds to wait. Assumes 1 MHz clock.
   * @param screenCapture Capture screen and print it to {@link System#out}?.
   */
  protected final void doTestBorderResult(int maxSeconds, boolean screenCapture) throws Exception {
    Condition passed = greenBorder;
    Condition failed1 = lightRedBorder;
    Condition failed2 = redBorder;
    Condition result = waitSecondsFor(maxSeconds, passed, failed1, failed2);
    if (screenCapture) {
      printScreen();
    }

    assertSame("Test failed", passed, result);
  }

  @Rule
  public final TestWatcher dumpOnFail = new TestWatcher() {
    @Override
    protected void failed(Throwable e, Description description) {
      try {
        dumpProgram();
      } catch (Exception io) {
        // ignore
      }
    }
  };

  /**
   * For debugging purposes disassemble test program.
   */
  protected void dumpProgram() throws IOException {
    System.out.flush();
    System.out.println();
    System.out.println();
    new Disassembler().listAndDisassemble(CodeBuffer.fromProgram(bytes), new PrintWriter(System.out));
    System.out.println();
    new Dumper().dump(CodeBuffer.fromProgram(bytes), new PrintWriter(System.out));
    System.out.println();
    System.out.flush();
  }

  @After
  @SuppressWarnings("deprecation")
  public void tearDown() throws Exception {
    if (c64 != null) {
      c64.setSystemOut(null);
      thread.interrupt();
      thread.join(1000);
      thread.stop();
    }
  }

  /**
   * Type string to keyboard.
   *
   * @param s String
   */
  protected void type(String s) throws Exception {
    Button dummySource = new Button();
    for (char c : s.toCharArray()) {
      KeyEvent event = new KeyEvent(dummySource, 0, 0, 0, 0, c, 0);
      systemIn.keyPressed(event);
      waitCycles(21000); // wait at least one interrupt
      systemIn.keyReleased(event);
      waitCycles(21000); // wait at least one interrupt
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
   * @param maxSeconds Max seconds to wait. Assumes 1 MHz clock.
   * @param conditions Conditions.
   * @return Condition that met or null, if timeout.
   */
  protected Condition waitSecondsFor(int maxSeconds, Condition... conditions) throws Exception {
    return waitCyclesFor(maxSeconds * 1000000L, conditions);
  }

  /**
   * Wait for a string to appear on screen.
   *
   * @param maxCycles Max cycles to wait.
   * @param conditions Conditions.
   * @return Condition that met or null, if timeout.
   */
  protected Condition waitCyclesFor(long maxCycles, Condition... conditions) throws Exception {
    for (long start = getTick(), end = start + maxCycles;;) {
      for (Condition condition : conditions) {
        if (condition.test()) {
          messageTimeSince(condition.toString(), start);
          return condition;
        }
      }

      if (programEnd.test()) {
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
    long ticks = getTick() - start;
    log.debug("{} after ~ {} seconds ({} ticks)", message, ticks / 1000000, ticks);
  }

  /**
   * Wait the given number of clock cycles.
   *
   * @param seconds Seconds to wait. Assumes 1 MHz clock.
   */
  protected void waitSeconds(int seconds) throws Exception {
    waitCycles(seconds * 1000000L);
  }

  /**
   * Wait the given number of clock cycles.
   *
   * @param cycles Cycles
   */
  protected void waitCycles(long cycles) throws Exception {
    for (long end = getTick() + cycles; getTick() < end; ) {
      if (throwable != null) {
        // Early exit on exceptions, because the emulation has been halted
        return;
      }

      Thread.sleep(10);
    }
  }

  /**
   * Get current clock tick.
   */
  private Long getTick() throws Exception {
    return c64.getClock().getTick();
  }

  //
  // Conditions
  //

  /**
   * Program end.
   */
  protected final Condition programEnd = new Condition() {
    @Override
    public boolean test() throws Exception {
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
    public boolean test() throws Exception {
      return c64.hasBrk();
    }

    @Override
    public String toString() {
      return "BRK";
    }
  };

  /**
   * Wait for "READY." on console.
   */
  private final Condition ready = onConsole("ready.\n");

  /**
   * Condition "text on console".
   */
  protected Condition onConsole(String... texts) {
    return new OnConsole(texts);
  }

  /**
   * Search for text in console.
   */
  private class OnConsole implements Condition {
    /**
     * Text.
     */
    private final String[] texts;

    /**
     * Constructor.
     *
     * @param texts Texts.
     */
    public OnConsole(String... texts) {
      this.texts = texts;
    }

    @Override
    public boolean test() {
      return Arrays.stream(texts).anyMatch(console::contains);
    }

    @Override
    public String toString() {
      return "Console contains " + StringUtils.arrayToDelimitedString(texts, " or ");
    }
  }

  /**
   * Condition "text on screen".
   */
  protected Condition onScreen(String... texts) {
    return new OnScreen(texts);
  }

  /**
   * Search for texts in console.
   */
  private class OnScreen implements Condition {
    /**
     * Text.
     */
    private final String[] texts;

    /**
     * Constructor.
     *
     * @param texts Texts.
     */
    public OnScreen(String... texts) {
      this.texts = texts;
    }

    @Override
    public boolean test() throws Exception {
      String screen = captureScreen();
      return Arrays.stream(texts).anyMatch(screen::contains);
    }

    @Override
    public String toString() {
      return "Screen contains " + StringUtils.arrayToDelimitedString(texts, " or ");
    }
  }

  /**
   * Green border. Usually "test passed".
   */
  protected final Condition greenBorder = inMemory(BORDER, GREEN);

  /**
   * Light red border. Usually "test failed".
   */
  protected final Condition lightRedBorder = inMemory(BORDER, LIGHT_RED);

  /**
   * Red border.  Usually "test failed".
   */
  protected final Condition redBorder = inMemory(BORDER, RED);

  /**
   * Condition "value in memory".
   */
  protected Condition inMemory(int addr, int value) {
    return new InMemory(addr, value);
  }

  /**
   * Wait for value in memory
   */
  private class InMemory implements Condition {
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
     * @param addr Address to monitor.
     * @param value Value to wait for.
     */
    public InMemory(int addr, int value) {
      this.addr = addr;
      this.value = value;
    }

    @Override
    public boolean test() throws Exception {
      return c64.getBus().read(addr) == value;
    }

    @Override
    public String toString() {
      return "Memory " + HexUtil.hexWord(addr) + " == " + HexUtil.hexByte(value);
    }
  }
}
