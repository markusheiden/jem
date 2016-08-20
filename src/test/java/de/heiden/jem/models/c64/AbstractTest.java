package de.heiden.jem.models.c64;

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

import org.junit.After;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.serialthreads.agent.Transform;
import org.serialthreads.agent.TransformingRunner;
import org.serialthreads.transformer.strategies.frequent3.FrequentInterruptsTransformer3;
import org.springframework.util.StringUtils;

import de.heiden.c64dt.assembler.CodeBuffer;
import de.heiden.c64dt.assembler.Disassembler;
import de.heiden.c64dt.assembler.Dumper;
import de.heiden.c64dt.util.HexUtil;

/**
 * Test support.
 */
@RunWith(TransformingRunner.class)
@Transform(transformer = FrequentInterruptsTransformer3.class, classPrefixes = "de.heiden.jem")
public abstract class AbstractTest {
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
  protected ConsoleBuffer console;

  /**
   * Keyboard input for test C64.
   */
  private KeyListener systemIn;

  /**
   * Thread to run test C64.
   */
  protected Thread thread;

  /**
   * Exception.
   */
  private volatile Exception exception;

  /**
   * Program.
   */
  private byte[] bytes;

  /**
   * Load program and start it via "run".
   */
  protected void loadAndRun(String program) throws Exception {
    URL url = getClass().getResource(program);
    loadAndRun(Paths.get(url.toURI()));
  }

  /**
   * Load program and start it via "run".
   */
  protected void loadAndRun(Path program) throws Exception {
    setUp(program);

    bytes = Files.readAllBytes(program);

    // Wait for boot to finish.
    thread.start();
    waitCyclesFor(3000000, onConsole("READY.\n"));
    console.clear();
    console.setLower(true);

    // Extract pure file name.
    String programName = program.getFileName().toString();
    programName = programName.substring(0, programName.indexOf(".prg"));
    // Load program.
    type("load\"" + programName + "\",8\n");
    // Skip further loads
    c64.rts(0xE16F);

    // Reset program end flag.
    c64.hasEnded();

    // Start program.
    type("run\n");
  }

  /**
   * Setup test C64.
   *
   * @param program Program to load
   */
  protected void setUp(Path program) throws Exception {
    console = new ConsoleBuffer();

    c64 = new TestC64(program.getParent());
    c64.setSystemOut(console);
    systemIn = c64.getSystemIn();

    thread = new Thread(() -> {
      try {
        c64.start();
      } catch (Exception e) {
        AbstractTest.this.exception = e;
      }
    }, program.getFileName().toString());
  }

  @Rule
  public final TestWatcher dumpOnFail = new TestWatcher() {
    @Override
    protected void failed(Throwable e, Description description) {
      try {
        dumpProgram();
      } catch (Exception io) {
        // ignore
      } finally {
        super.failed(e, description);
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
    new Disassembler().disassemble(CodeBuffer.fromProgram(bytes), new PrintWriter(System.out));
    System.out.println();
    new Dumper().dump(CodeBuffer.fromProgram(bytes), new PrintWriter(System.out));
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
    for (char c : s.toCharArray()) {
      KeyEvent event = new KeyEvent(new Button(), 0, 0, 0, 0, c, 0);
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
   * @param maxCycles Max cycles to wait
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

      if (exception != null) {
        messageTimeSince("Exception", start);
        // Abort on exceptions
        throw exception;
      }

      waitCycles(10000);
    }
  }

  /**
   * String representation of time since the given start timestamp.
   */
  private void messageTimeSince(String message, long start) throws Exception {
    System.out.println();
    long ticks = getTick() - start;
    System.out.println(String.format("%s after ~ %d seconds (%d ticks)", message, ticks / 1000000, ticks));
    System.out.flush();
  }

  /**
   * Wait the given number of clock cycles.
   *
   * @param cycles Cycles
   */
  protected void waitCycles(int cycles) throws Exception {
    for (long end = getTick() + cycles; getTick() < end; ) {
      if (exception != null) {
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
