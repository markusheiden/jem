package de.heiden.jem.models.c64.gui.swing.emulator;

import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.models.c64.components.C64;

import javax.swing.*;

/**
 * Application for C64 emulation.
 */
public class C64Application {
  /**
   * Clock.
   */
  private Clock clock;

  /**
   * C64.
   */
  private C64 c64;

  /**
   * Main frame.
   */
  private JFrame _frame;

  /**
   * Constructor.
   *
   * @param clock Clock.
   */
  public C64Application(Clock clock) {
    this.clock = clock;
  }

  /**
   * Start application.
   */
  public void start() throws Exception {
    c64 = new C64(clock);

    _frame = new JFrame("C64");
    _frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

    VICScreen screen = new VICScreen(c64.getVIC()._displayUnit);
    _frame.addKeyListener(new KeyListener(c64.getKeyboard(), new PCMapping()));
    _frame.getContentPane().add(screen);

    // pack
    _frame.pack();
    // _frame.setResizable(false);
    _frame.setVisible(true);

    c64.start();
  }

  /**
   * Stop application.
   */
  public void stop() {
    c64.stop();

    _frame.setVisible(false);
    _frame.dispose();
    _frame = null;
  }
}
