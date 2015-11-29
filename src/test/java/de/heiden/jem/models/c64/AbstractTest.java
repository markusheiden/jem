package de.heiden.jem.models.c64;

import org.junit.After;
import org.junit.BeforeClass;
import org.serialthreads.agent.TransformingClassLoader;
import org.serialthreads.transformer.Strategies;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.OutputStream;

/**
 * Test support.
 */
public class AbstractTest {
  /**
   * Constructor for transformed test C64.
   */
  private static Class<?> c64Class;

  /**
   * Transformed test C64.
   */
  private Object c64;

  /**
   * C64 clock.
   */
  private Object clock;

  /**
   * Screen output of test C64.
   */
  protected ScreenBuffer screen;

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

  @BeforeClass
  public static void setUpClass() throws Exception {
    ClassLoader classLoader = new TransformingClassLoader(AbstractTest.class.getClassLoader(), Strategies.DEFAULT, "de.heiden.jem");
    c64Class = classLoader.loadClass("de.heiden.jem.models.c64.TestC64");
  }

  /**
   * Setup test C64.
   *
   * @param program Program to load
   */
  protected void setUp(File program) throws Exception {
    screen = new ScreenBuffer();

    c64 = c64Class.getConstructor(File.class).newInstance(program.getParentFile());
    clock = c64Class.getMethod("getClock").invoke(c64);
    c64Class.getMethod("setSystemOut", OutputStream.class).invoke(c64, screen);
    systemIn = (KeyListener) c64Class.getMethod("getSystemIn").invoke(c64);

    thread = new Thread(() -> {
      try {
        c64Class.getMethod("start").invoke(c64);
      } catch (Exception e) {
        AbstractTest.this.exception = e;
      }
    }, program.getName());
  }

  /**
   * Add a patch to the cpu, to insert a RTS at the given address.
   *
   * @param addr Address to write RTS to
   */
  protected void rts(int addr) throws Exception {
    c64Class.getMethod("rts", int.class).invoke(c64, addr);
  }

  @After
  @SuppressWarnings("deprecation")
  public void tearDown() throws Exception {
    c64Class.getMethod("setSystemOut", OutputStream.class).invoke(c64, (Object) null);
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
   * Wait for a string to appear on screen.
   *
   * @param maxCycles Max cycles to wait
   * @param strings Strings
   * @return Index of string that appeared on screen or -1, if timeout
   */
  protected int waitFor(long maxCycles, String... strings) throws Exception {
    long end = getTick() + maxCycles;

    for (;;) {
      for (int i = 0; i < strings.length; i++) {
        if (screen.contains(strings[i])) {
          System.out.flush();
          return i;
        }
      }

      if (getTick() >= end) {
        // Timeout -> exit with -1
        System.out.flush();
        return -1;
      }

      if (exception != null) {
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
    return (Long) clock.getClass().getMethod("getTick").invoke(clock);
  }
}
