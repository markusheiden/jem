package de.heiden.jem.models.c64;

import de.heiden.jem.components.clock.loom.ContinuationClock;
import de.heiden.jem.models.c64.gui.javafx.emulator.C64Application;

/**
 * C64 Startup with continuations.
 */
public class C64Continuation {
  /**
   * Start application.
   */
  public static void main(String[] args) {
    try {
      C64Application.start("--clock=" + ContinuationClock.class.getName());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
