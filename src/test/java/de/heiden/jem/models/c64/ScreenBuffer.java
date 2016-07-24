package de.heiden.jem.models.c64;

import java.io.IOException;
import java.io.OutputStream;

import de.heiden.c64dt.charset.PetSCIICharset;

/**
 * Buffer for C64 screen output
 */
public class ScreenBuffer extends OutputStream {
  /**
   * Charset.
   */
  private final PetSCIICharset charset = new PetSCIICharset();

  /**
   * Buffer for screen output.
   */
  private final StringBuilder screen = new StringBuilder(1024);

  /**
   * Upper and lower case chars?.
   */
  public synchronized void setLower(boolean lower) {
    charset.toChar((byte) (lower ? 0x0E : 0x8E));
  }

  @Override
  public synchronized void write(int b) throws IOException {
    char c = charset.toChar((byte) b);
    if (c > 0) {
      System.out.print(c);
      screen.append(c);
    }
  }

  /**
   * Is string s on the screen?.
   *
   * @param s String
   */
  public synchronized boolean contains(String s) {
    return screen.indexOf(s) >= 0;
  }

  /**
   * Clear screen buffer.
   */
  public synchronized void clear() {
    System.out.println();
    System.out.flush();
    screen.setLength(0);
  }

  @Override
  public String toString() {
    return screen.toString();
  }
}
