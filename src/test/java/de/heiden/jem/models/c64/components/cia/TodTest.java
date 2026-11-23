package de.heiden.jem.models.c64.components.cia;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test for the timing of the TOD of {@link AbstractCIA}.
 */
@Tag("cia")
class TodTest {
    /**
     * PAL: 50 Hz input, 50 Hz mode. 17734475 cycles are exactly 18 seconds.
     */
    @Test
    void pal() {
        var cia = start(TodInput.PAL, AbstractCIA.CRA_TOD_50HZ);

        run(cia, TodInput.PAL.cpuClock() - 1);
        assertEquals(179, tenths(cia));
        run(cia, 1);
        assertEquals(180, tenths(cia));
    }

    /**
     * NTSC: 60 Hz input, 60 Hz mode. 315000000 cycles are exactly 308 seconds.
     */
    @Test
    void ntsc() {
        var cia = start(TodInput.NTSC, 0);

        run(cia, TodInput.NTSC.cpuClock() - 1);
        assertEquals(3079, tenths(cia));
        run(cia, 1);
        assertEquals(3080, tenths(cia));
    }

    /**
     * PAL: 50 Hz input, 60 Hz mode (the default after reset). The TOD runs 5/6 too slow.
     * <p>
     * This is the state of a PAL C64, as long as the KERNAL swtiches to the 50 Hz mode.
     */
    @Test
    void palIn60HzMode() {
        var cia = start(TodInput.PAL, 0);

        run(cia, TodInput.PAL.cpuClock() - 1);
        assertEquals(149, tenths(cia));
        run(cia, 1);
        assertEquals(150, tenths(cia));
    }

    //
    // Helper.
    //

    /**
     * Create a CIA and start its TOD at 00:00:00.0.
     *
     * @param todInput
     *         TOD input.
     * @param cra
     *         Control register A.
     */
    AbstractCIA start(TodInput todInput, int cra) {
        var cia = new CIA6526(todInput);
        cia.write(cra, 0x0E);
        cia.write(0x00, 0x0B);
        cia.write(0x00, 0x0A);
        cia.write(0x00, 0x09);
        cia.write(0x00, 0x08);
        return cia;
    }

    /**
     * Execute cycles.
     */
    void run(AbstractCIA cia, int cycles) {
        for (var i = 0; i < cycles; i++) {
            cia.cycle();
        }
    }

    /**
     * Time of day in tenths of seconds. Hours are expected to be zero.
     */
    int tenths(AbstractCIA cia) {
        assertEquals(0x00, cia.read(0x0B));
        var minutes = bcd(cia.read(0x0A));
        var seconds = bcd(cia.read(0x09));
        var tenths = cia.read(0x08);
        return (minutes * 60 + seconds) * 10 + tenths;
    }

    int bcd(int value) {
        return (value >> 4) * 10 + (value & 0x0F);
    }
}
