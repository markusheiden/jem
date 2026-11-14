package de.heiden.jem.models.c64.components.joystick;

import de.heiden.jem.components.clock.serialthreads.SerialClock;
import de.heiden.jem.models.c64.components.cia.CIA6526;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test for {@link Joystick} connected to a {@link CIA6526}.
 */
class JoystickTest {
    private final CIA6526 cia = new CIA6526(new SerialClock());
    private final Joystick joystick1 = new Joystick(cia.portB());
    private final Joystick joystick2 = new Joystick(cia.portA());

    /**
     * Joystick 2 on port A: Visible even though port A is an output, like set up by the KERNAL.
     */
    @Test
    void joystick2() {
        cia.write(0xFF, 0x02); // DDRA: all output
        cia.write(0x7F, 0x00); // keyboard column 7
        assertEquals(0x7F, cia.read(0x00));

        joystick2.press(Direction.UP);
        assertEquals(0x7E, cia.read(0x00));
        joystick2.press(Direction.FIRE);
        assertEquals(0x6E, cia.read(0x00));

        joystick2.release(Direction.UP);
        assertEquals(0x6F, cia.read(0x00));
        joystick2.releaseAll();
        assertEquals(0x7F, cia.read(0x00));

        // Port B is not affected.
        joystick2.press(Direction.LEFT);
        assertEquals(0xFF, cia.read(0x01));
    }

    /**
     * Joystick 1 on port B: Visible on the input port.
     */
    @Test
    void joystick1() {
        cia.write(0x00, 0x03); // DDRB: all input
        assertEquals(0xFF, cia.read(0x01));

        joystick1.press(Direction.FIRE);
        assertEquals(0xEF, cia.read(0x01));
        joystick1.press(Direction.RIGHT);
        assertEquals(0xE7, cia.read(0x01));

        joystick1.release(Direction.FIRE);
        joystick1.release(Direction.RIGHT);
        assertEquals(0xFF, cia.read(0x01));
    }
}
