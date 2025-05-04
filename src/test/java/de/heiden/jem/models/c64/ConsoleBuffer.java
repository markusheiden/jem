package de.heiden.jem.models.c64;

import de.heiden.c64dt.charset.PetSCIICharset;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Buffer for C64 screen output
 */
public class ConsoleBuffer extends OutputStream {
  /**
   * Charset.
   */
  private final PetSCIICharset charset = new PetSCIICharset(false);

  /**
   * Buffer for screen output.
   * Synchronized access.
   */
  private final StringBuilder screen = new StringBuilder(1024);

  private final AtomicInteger column = new AtomicInteger(0);

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
      writeChar(c);
    }
  }

  /**
   * Write character to screen.
   */
  public synchronized void writeChar(char c) {
    if (c == '\n') {
      column.set(0);
    } else {
      column.incrementAndGet();
    }

    System.out.print(c);
    screen.append(c);

    if (column.get() >= 40) {
      writeChar('\n');
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
