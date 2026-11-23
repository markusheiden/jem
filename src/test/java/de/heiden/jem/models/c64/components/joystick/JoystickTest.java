package de.heiden.jem.models.c64.components.joystick;

import de.heiden.jem.components.clock.serialthreads.SerialClock;
import de.heiden.jem.models.c64.components.cia.CIA6526;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link Joystick} connected to a {@link CIA6526}.
 */
class JoystickTest {
    private final SerialClock clock = new SerialClock();
    private final CIA6526 cia = new CIA6526();
    private final Joystick joystick1 = new Joystick(clock, cia.portB());
    private final Joystick joystick2 = new Joystick(clock, cia.portA());

    /**
     * Directions are not visible before the C64 thread updates the port.
     */
    @Test
    void deferred() {
        cia.write(0x00, 0x03); // DDRB: All input.

        joystick1.press(Direction.UP);
        assertThat(cia.read(0x01)).isEqualTo(0xFF);
        joystick1.updatePort();
        assertThat(cia.read(0x01)).isEqualTo(0xFE);
    }

    /**
     * Joystick 2 on port A: Visible even though port A is an output, like set up by the KERNAL.
     */
    @Test
    void joystick2() {
        cia.write(0xFF, 0x02); // DDRA: All output.
        cia.write(0x7F, 0x00); // Keyboard column 7.
        assertThat(cia.read(0x00)).isEqualTo(0x7F);

        joystick2.press(Direction.UP);
        joystick2.updatePort();
        assertThat(cia.read(0x00)).isEqualTo(0x7E);
        joystick2.press(Direction.FIRE);
        joystick2.updatePort();
        assertThat(cia.read(0x00)).isEqualTo(0x6E);

        joystick2.release(Direction.UP);
        joystick2.updatePort();
        assertThat(cia.read(0x00)).isEqualTo(0x6F);
        joystick2.releaseAll();
        joystick2.updatePort();
        assertThat(cia.read(0x00)).isEqualTo(0x7F);

        // Port B is not affected.
        joystick2.press(Direction.LEFT);
        joystick2.updatePort();
        assertThat(cia.read(0x01)).isEqualTo(0xFF);
    }

    /**
     * Joystick 1 on port B: Visible on the input port.
     */
    @Test
    void joystick1() {
        cia.write(0x00, 0x03); // DDRB: All input.
        assertThat(cia.read(0x01)).isEqualTo(0xFF);

        joystick1.press(Direction.FIRE);
        joystick1.updatePort();
        assertThat(cia.read(0x01)).isEqualTo(0xEF);
        joystick1.press(Direction.RIGHT);
        joystick1.updatePort();
        assertThat(cia.read(0x01)).isEqualTo(0xE7);

        joystick1.release(Direction.FIRE);
        joystick1.release(Direction.RIGHT);
        joystick1.updatePort();
        assertThat(cia.read(0x01)).isEqualTo(0xFF);
    }
}
