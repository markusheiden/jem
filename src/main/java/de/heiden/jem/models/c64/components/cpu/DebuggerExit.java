package de.heiden.jem.models.c64.components.cpu;

/**
 * Easy exit of execution of C64 debug cpu.
 */
public class DebuggerExit extends RuntimeException {
  /**
   * Constructor.
   *
   * @param message message
   */
  public DebuggerExit(String message) {
    super(message);
  }
}
