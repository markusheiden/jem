package de.heiden.jem.models.c64;

import de.heiden.c64dt.charset.C64Charset;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Buffer for C64 screen output
 */
public class ScreenBuffer extends OutputStream {
  /**
   * Charset.
   */
  private C64Charset charset = C64Charset.LOWER;

  /**
   * Buffer for screen output.
   */
  private final StringBuffer screen = new StringBuffer(1024);

  @Override
  public void write(int b) throws IOException {
    char c = convert(b);
    if (c > 0) {
      synchronized (screen) {
        screen.append(c);
        screen.notifyAll();
      }

      System.out.print(c);
    }
  }

  /**
   * Convert C64 encoded char to Java character.
   *
   * @param b C64 encoded char
   */
  private char convert(int b) {
    String result;
    switch (b) {
      case 0x0a:
        return '\r';
      case 0x0d:
        return '\n';
      case 0x91:
        return 0; // cursor up -> ignore
      case 0x93:
        return 0; // clr/home -> ignore
      default:
        return charset.toChar((byte) b);
    }
  }

  /**
   * Is string s on the screen?.
   *
   * @param s String
   */
  public boolean contains(String s) {
    return screen.indexOf(s) >= 0;
  }

  /**
   * Clear screen buffer.
   */
  public void clear() {
    screen.setLength(0);

    System.out.println();
    System.out.flush();
  }

  /**
   * Wait for a string to appear on screen.
   *
   * @param maxWait Max milliseconds to wait
   * @param strings Strings
   * @return Index of string that appeared on screen or -1, if timeout
   */
  public int waitFor(long maxWait, String... strings) throws Exception {
    long end = System.currentTimeMillis() + maxWait;

    while (true) {
      for (int i = 0; i < strings.length; i++) {
        if (contains(strings[i])) {
          System.out.flush();
          return i;
        }
      }

      long toWait = end - System.currentTimeMillis();
      if (toWait <= 0) {
        // Timeout -> exit with -1
        System.out.flush();
        return -1;
      }

      synchronized (screen) {
        screen.wait(toWait);
      }
    }
  }
}
