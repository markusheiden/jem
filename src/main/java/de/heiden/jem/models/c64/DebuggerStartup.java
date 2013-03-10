package de.heiden.jem.models.c64;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.swing.*;
import java.awt.*;

/**
 * C64 Startup.
 */
public class DebuggerStartup extends Startup {
  /**
   * Logger.
   */
  private final Log logger = LogFactory.getLog(getClass());

  public void start() {
    try {
      Class<?> clazz = loadClass("de.heiden.jem.models.c64.monitor.gui.DebuggerGUI");
      Object debugger = clazz.newInstance();

      JFrame frame = new JFrame("C64 Debugger");
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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
