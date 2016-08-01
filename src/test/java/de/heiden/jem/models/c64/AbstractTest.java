package de.heiden.jem.models.c64;

import org.junit.After;
import org.junit.runner.RunWith;
import org.serialthreads.agent.Transform;
import org.serialthreads.agent.TransformingRunner;
import org.serialthreads.transformer.strategies.frequent3.FrequentInterruptsTransformer3;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;

/**
 * Test support.
 */
@RunWith(TransformingRunner.class)
@Transform(transformer = FrequentInterruptsTransformer3.class, classPrefixes = "de.heiden.jem")
public abstract class AbstractTest {
  /**
   * Return value for wait method in the case that no string matched.
   */
  protected static final int WAIT_NO_MATCH = -1;

  /**
   * Return value for wait method in the case that the program terminated.
   */
  protected static final int WAIT_PROGRAM_END = -2;

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
   * Setup test C64.
   *
   * @param program Program to load
   */
  protected void setUp(File program) throws Exception {
    console = new ConsoleBuffer();

    c64 = new TestC64(program.getParentFile());
    c64.setSystemOut(console);
    systemIn = c64.getSystemIn();

    thread = new Thread(() -> {
      try {
        c64.start();
      } catch (Exception e) {
        AbstractTest.this.exception = e;
      }
    }, program.getName());
  }

  @After
  @SuppressWarnings("deprecation")
  public void tearDown() throws Exception {
    c64.setSystemOut(null);
    thread.interrupt();
    thread.join(1000);
    thread.stop();
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
   * Capture screen at $0400.
   */
  public String captureScreen() throws Exception {
    return new ScreenBuffer(c64.getBus()).capture();
  }

  /**
   * Wait for a string to appear on screen.
   *
   * @param maxCycles Max cycles to wait
   * @param strings Strings
   * @return Index of string that appeared on screen or -1, if timeout
   */
  protected int waitFor(long maxCycles, String... strings) throws Exception {
    Long start = getTick();
    long end = start + maxCycles;

    for (;;) {
      for (int i = 0; i < strings.length; i++) {
        if (console.contains(strings[i])) {
          System.out.flush();
          return i;
        }
      }

      if (c64.hasEnded()) {
        System.out.println("Program end after " + (getTick() - start) + " ticks");
        System.out.flush();
        return WAIT_PROGRAM_END;
      }

      if (getTick() >= end) {
        System.out.println("No match after " + (getTick() - start) + " ticks");
        System.out.flush();
        return WAIT_NO_MATCH;
      }

      if (exception != null) {
        System.out.println("Exception after " + (getTick() - start) + " ticks");
        System.out.flush();
        // Abort on exceptions
        throw exception;
      }

      waitCycles(10000);
    }
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
}
