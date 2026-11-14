package de.heiden.jem.models.c64.components.joystick;

import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.components.clock.ClockEvent;
import de.heiden.jem.components.ports.InputOutputPort;
import de.heiden.jem.components.ports.OutputPort;
import de.heiden.jem.components.ports.OutputPortImpl;
import jakarta.annotation.Nonnull;

/**
 * Digital joystick connected to a port of a CIA.
 * Pressed directions and the fire button pull their port bit low.
 * <p>
 * Directions are pressed by the GUI thread, but the port is driven by the C64 thread only,
 * because the ports are not thread safe.
 */
public class Joystick extends ClockEvent {
    /**
     * Port bits driven by the joystick.
     */
    private static final int MASK = 0x1F;

    /**
     * Ticks between port updates: One PAL raster line.
     */
    private static final int INTERVAL = 63;

    /**
     * Clock.
     */
    private final Clock clock;

    /**
     * Port the joystick drives.
     */
    private final OutputPort port = new OutputPortImpl();

    /**
     * Pressed directions as bits. Written by the GUI thread, read by the C64 thread.
     */
    private volatile int pressed = 0;

    /**
     * Pressed directions as bits, as currently driven to the port.
     */
    private int driven = 0;

    /**
     * Constructor.
     *
     * @param clock
     *         clock
     * @param ciaPort
     *         CIA port to connect to
     */
    public Joystick(@Nonnull Clock clock, @Nonnull InputOutputPort ciaPort) {
        super("Joystick");

        this.clock = clock;

        port.setOutputData(0xFF);
        port.setOutputMask(MASK);
        ciaPort.connect(port);

        clock.addClockEvent(INTERVAL, this);
    }

    /**
     * Press a direction or the fire button.
     */
    public void press(@Nonnull Direction direction) {
        pressed |= direction.bit;
    }

    /**
     * Release a direction or the fire button.
     */
    public void release(@Nonnull Direction direction) {
        pressed &= ~direction.bit;
    }

    /**
     * Release all directions and the fire button.
     */
    public void releaseAll() {
        pressed = 0;
    }

    @Override
    public void execute(long tick) {
        updatePort();
        clock.addClockEvent(tick + INTERVAL, this);
    }

    /**
     * Drive port: Pressed bits low.
     */
    void updatePort() {
        var pressed = this.pressed;
        if (pressed != driven) {
            driven = pressed;
            port.setOutputData(~pressed & 0xFF);
        }
    }
}
