package de.heiden.jem.models.c64;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.serialthreads.agent.TransformingClassLoader;
import org.serialthreads.transformer.Strategies;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FilenameFilter;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Testsuite 2.15.
 * <p/>
 * TODO time should be simulated time or cycles instead of real time
 */
@RunWith(Parameterized.class)
public class Testsuite2_15 {
  /**
   * Test program.
   */
  @Parameter(0)
  public File program;

  /**
   * Test program filename, just for test naming purposes.
   */
  @Parameter(1)
  public String filename;

  /**
   * Constructor for transformed test C64.
   */
  private static Class<?> c64Class;

  /**
   * Transformed test C64.
   */
  private static Object c64;

  /**
   * C64 clock.
   */
  private Object clock;

  /**
   * Screen output of test C64.
   */
  private ScreenBuffer screen;

  /**
   * Keyboard input for test C64.
   */
  private KeyListener systemIn;

  /**
   * Thread to run test C64.
   */
  private Thread thread;

  /**
   * Exception.
   */
  private volatile Exception exception;

  @BeforeClass
  public static void setUpClass() throws Exception {
    ClassLoader classLoader = new TransformingClassLoader(Testsuite2_15.class.getClassLoader(), Strategies.DEFAULT, "de.heiden.jem");
    c64Class = classLoader.loadClass("de.heiden.jem.models.c64.TestC64");
  }

  @Before
  public void setUp() throws Exception {
    screen = new ScreenBuffer();

    c64 = c64Class.getConstructor(File.class).newInstance(program);
    clock = c64Class.getMethod("getClock").invoke(c64);
    c64Class.getMethod("setSystemOut", OutputStream.class).invoke(c64, screen);
    systemIn = (KeyListener) c64Class.getMethod("getSystemIn").invoke(c64);

    thread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          c64Class.getMethod("start").invoke(c64);
        } catch (ReflectiveOperationException e) {
          Testsuite2_15.this.exception = e;
        }
      }
    }, filename);
  }

  private static final List<String> ignore = Arrays.asList(
    "alr", "anc", "ane", "arr", "aso", "axs",
    "andzx"
  );

  @Parameters(name = "{1}")
  public static Collection<Object[]> parameters() throws Exception {
    URL start = Testsuite2_15.class.getResource("/testsuite2.15/ start.prg");
    File testDir = new File(start.toURI()).getParentFile();
    File[] programs = testDir.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return !name.startsWith(" ") && !ignore(name) && name.endsWith(".prg");
      }
    });

    Collection<Object[]> result = new ArrayList<>(programs.length);
    for (File program : programs) {
      result.add(new Object[]{program, program.getName()});
    }

    return result;
  }

  private static boolean ignore(String name) {
    for (String i : ignore) {
      if (name.startsWith(i)) {
        return true;
      }
    }

    return false;
  }

  @After
  public void tearDown() throws Exception {
    c64Class.getMethod("setSystemOut", OutputStream.class).invoke(c64, new Object[]{null});
    thread.interrupt();
  }

  @Test
  public void test() throws Exception {
    thread.start();
    waitFor(2000000, "READY.");
    screen.clear();

    screen.setLower(true);
    type("poke2,1:sys2070\n");
    int event = waitFor(30000000, "- ok", "right", "error");
    waitCycles(1000);

    // Assert that test program exits with "OK" message.
    // Consider everything else (timeout, error messages) as a test failure.
    assertEquals(0, event);
  }

  /**
   * Type string to keyboard.
   *
   * @param s String
   */
  private void type(String s) throws Exception {
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
  private int waitFor(long maxCycles, String... strings) throws Exception {
    long end = getTick() + maxCycles;

    while (true) {
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

      waitCycles(10000);
    }
  }

  /**
   * Wait the given number of clock cycles.
   *
   * @param cycles Cycles
   */
  private void waitCycles(int cycles) throws Exception {
    for (long end = getTick() + cycles; getTick() < end; ) {
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
