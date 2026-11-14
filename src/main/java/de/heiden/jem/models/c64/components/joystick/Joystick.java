package de.heiden.jem.models.c64.components.joystick;

import de.heiden.jem.components.ports.InputOutputPort;
import de.heiden.jem.components.ports.OutputPortImpl;
import jakarta.annotation.Nonnull;

/**
 * Digital joystick connected to a port of a CIA.
 * Pressed directions and the fire button pull their port bit low.
 */
public class Joystick {
    /**
     * Port bits driven by the joystick.
     */
    private static final int MASK = 0x1F;

    /**
     * Port the joystick drives.
     */
    private final OutputPortImpl port = new OutputPortImpl();

    /**
     * Pressed directions as bits.
     */
    private int pressed = 0;

    /**
     * Constructor.
     *
     * @param ciaPort
     *         CIA port to connect to
     */
    public Joystick(@Nonnull InputOutputPort ciaPort) {
        port.setOutputData(0xFF);
        port.setOutputMask(MASK);
        ciaPort.connect(port);
    }

    /**
     * Press a direction or the fire button.
     */
    public void press(@Nonnull Direction direction) {
        pressed |= direction.bit;
        updatePort();
    }

    /**
     * Release a direction or the fire button.
     */
    public void release(@Nonnull Direction direction) {
        pressed &= ~direction.bit;
        updatePort();
    }

    /**
     * Release all directions and the fire button.
     */
    public void releaseAll() {
        pressed = 0;
        updatePort();
    }

    /**
     * Drive port: Pressed bits low.
     */
    private void updatePort() {
        port.setOutputData(~pressed & 0xFF);
    }
}
