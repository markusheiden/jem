package de.heiden.jem.models.c64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

import static java.awt.BorderLayout.CENTER;
import static javax.swing.WindowConstants.EXIT_ON_CLOSE;

/**
 * C64 Startup.
 */
public class DebuggerSerial extends C64Serial {
    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void start() {
        try {
            var clazz = loadClass("de.heiden.jem.models.c64.gui.swing.monitor.DebuggerGUI");
            var debugger = clazz.getDeclaredConstructor().newInstance();

            var frame = new JFrame("C64 Debugger");
            frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            frame.add((Component) debugger, CENTER);

            frame.pack();
            frame.setVisible(true);
        } catch (Exception e) {
            logger.error("Unable to startup", e);
        }
    }

    public static void main(String[] args) {
        new DebuggerSerial().start();
    }
}
