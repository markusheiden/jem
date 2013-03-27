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
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Testsuite 2.15.
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
   * Screen output of test C64.
   */
  private StringBuffer systemOut;

  /**
   * Keyboard input for test C64.
   */
  private KeyListener systemIn;

  /**
   * Thread to run test C64.
   */
  private Thread thread;

  @BeforeClass
  public static void setUpClass() throws Exception {
    ClassLoader classLoader = new TransformingClassLoader(Testsuite2_15.class.getClassLoader(), Strategies.DEFAULT, "de.heiden.jem");
    c64Class = classLoader.loadClass("de.heiden.jem.models.c64.TestC64");
  }

  @Before
  public void setUp() throws Exception {
    c64 = c64Class.getConstructor(File.class).newInstance(program);
    systemOut = (StringBuffer) c64Class.getMethod("getSystemOut").invoke(c64);
    systemIn = (KeyListener) c64Class.getMethod("getSystemIn").invoke(c64);

    thread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          c64Class.getMethod("start").invoke(c64);
        } catch (ReflectiveOperationException e) {
          e.printStackTrace();
        }
      }
    }, filename);
  }

  @Parameters(name = "{1}")
  public static Collection<Object[]> parameters() throws Exception {
    URL start = Testsuite2_15.class.getResource("/testsuite2.15/ start.prg");
    File testDir = new File(start.toURI()).getParentFile();
    File[] programs = testDir.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return !name.startsWith(" ") && name.endsWith(".prg");
      }
    });

    Collection<Object[]> result = new ArrayList<>(programs.length);
    for (File program : programs) {
      result.add(new Object[]{program, program.getName()});
    }

    return result;
  }

  @After
  public void tearDown() {
    thread.interrupt();
  }

  @Test
  public void test() throws Exception {
    thread.start();

    waitFor("READY.");

    type("SYS2070\n");

    Thread.sleep(2000);

    System.out.println(systemOut.toString());
  }

  /**
   * Wait for string in output.
   *
   * @param s String
   */
  private void waitFor(String s) throws Exception {
    while (systemOut.indexOf(s) < 0) {
      Thread.yield();
    }
    Thread.sleep(100);

//    systemOut.setLength(0);
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
      Thread.sleep(100);
      systemIn.keyReleased(event);
      Thread.sleep(100);
    }
  }
}
