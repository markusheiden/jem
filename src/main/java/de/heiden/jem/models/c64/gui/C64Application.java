package de.heiden.jem.models.c64.gui;

import de.heiden.jem.models.c64.C64;

import javax.swing.*;

/**
 * Application for C64 emulation.
 */
public class C64Application {
  /**
   * C64.
   */
  private C64 c64;

  /**
   * Main frame.
   */
  private JFrame _frame;

  /**
   * Start application.
   */
  public void start() throws Exception {
    c64 = new C64();

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
