package de.heiden.jem.models.c64.gui.swing.emulator;

import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.models.c64.components.C64;
import de.heiden.jem.models.c64.components.VideoSystem;

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
    private JFrame frame;

    /**
     * Constructor.
     *
     * @param clock
     *         Clock.
     */
    public C64Application(Clock clock) {
        this.clock = clock;
    }

    /**
     * Start the application.
     */
    public void start() throws Exception {
        c64 = C64.builder(clock).videoSystem(VideoSystem.selected()).build();

        frame = new JFrame("C64");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        var screen = new VICScreen(c64.getVIC().displayUnit);
        frame.addKeyListener(new KeyListener(c64.getKeyboard(), new PCMapping()));
        frame.getContentPane().add(screen);

        // pack
        frame.pack();
        // frame.setResizable(false);
        frame.setVisible(true);

        c64.start();
    }

    /**
     * Stop the application.
     */
    public void stop() {
        c64.stop();

        frame.setVisible(false);
        frame.dispose();
        frame = null;
    }
}
