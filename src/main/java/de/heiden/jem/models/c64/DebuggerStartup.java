package de.heiden.jem.models.c64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * C64 Startup.
 */
public class DebuggerStartup extends Startup {
  /**
   * Logger.
   */
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  public void start() {
    try {
      Class<?> clazz = loadClass("de.heiden.jem.models.c64.gui.swing.monitor.DebuggerGUI");
      Object debugger = clazz.getDeclaredConstructor().newInstance();

      JFrame frame = new JFrame("C64 Debugger");
      frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
      frame.setLayout(new BorderLayout());

      frame.add((Component) debugger, BorderLayout.CENTER);

      frame.pack();
      frame.setVisible(true);
    } catch (Exception e) {
      logger.error("Unable to startup", e);
    }
  }

  public static void main(String[] args) {
    new DebuggerStartup().start();
  }
}
