package de.heiden.jem.models.c64;

import org.apache.log4j.Logger;

import javax.swing.JFrame;
import java.awt.BorderLayout;
import java.awt.Component;

/**
 * C64 Startup.
 */
public class DebuggerStartup extends Startup
{
  public void start() throws Exception
  {
    Class<?> clazz = loadClass("de.heiden.jem.models.c64.monitor.gui.DebuggerGUI");
    Object debugger = clazz.newInstance();

    JFrame frame = new JFrame("C64 Debugger");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    frame.add((Component) debugger, BorderLayout.CENTER);

    frame.pack();
    frame.setVisible(true);
  }

  public static void main(String[] args)
  {
    try
    {
      new DebuggerStartup().start();
    }
    catch (Exception e)
    {
      _logger.error("Unable to startup", e);
    }
  }

  //
  // private attributes
  //

  /**
   * Logger.
   */
  private static final Logger _logger = Logger.getLogger(DebuggerStartup.class);
}
