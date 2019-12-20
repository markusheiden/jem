package de.heiden.jem.models.c64;

import de.heiden.jem.components.clock.serialthreads.SerialClock;
import de.heiden.jem.models.c64.gui.javafx.emulator.C64Application;

/**
 * C64 Startup with byte code transformation by an agent.
 */
public class C64SerialAgent {
  /**
   * Start application.
   */
  public static void main(String[] args) {
    try {
      C64Application.start("--clock=" + SerialClock.class.getName());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
