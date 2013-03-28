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
  }

  /**
   * Wait for a string to appear on screen.
   *
   * @param strings Strings
   * @return Index of string that appeared on screen
   */
  public int waitFor(String... strings) throws Exception {
    while (true) {
      for (int i = 0; i < strings.length; i++) {
        if (contains(strings[i])) {
          return i;
        }
      }

      synchronized (screen) {
        screen.wait();
      }
    }
  }
}
