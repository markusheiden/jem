package de.heiden.jem.models.c64.components.cia;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test for the timing of the TOD of {@link AbstractCIA}.
 */
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
     * PAL CPU clock with 60 Hz input in 50 Hz mode: The TOD runs 6/5 too fast.
     */
    @Test
    void palWith60Hz() {
        var todInput = TodInput.PAL.withFrequency(60);
        var cia = start(todInput, AbstractCIA.CRA_TOD_50HZ);

        run(cia, todInput.cpuClock() - 1);
        assertEquals(215, tenths(cia));
        run(cia, 1);
        assertEquals(216, tenths(cia));
    }

    /**
     * NTSC CPU clock with 50 Hz input in 60 Hz mode: The TOD runs 5/6 too slow.
     */
    @Test
    void ntscWith50Hz() {
        var todInput = TodInput.NTSC.withFrequency(50);
        var cia = start(todInput, 0);

        run(cia, todInput.cpuClock() - 1);
        assertEquals(2566, tenths(cia));
        run(cia, 1);
        assertEquals(2566, tenths(cia));
        // 308 s * 50 Hz / 6 = 2566.67 tenths: The next tenth follows after 2 more ticks of the input.
        run(cia, 2 * todInput.cpuClock() / todInput.increment() + 1);
        assertEquals(2567, tenths(cia));
    }

    /**
     * Create a CIA and start its TOD at 00:00:00.0.
     *
     * @param todInput
     *         TOD input
     * @param cra
     *         control register A
     */
    private static AbstractCIA start(TodInput todInput, int cra) {
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
    private static void run(AbstractCIA cia, int cycles) {
        for (var i = 0; i < cycles; i++) {
            cia.cycle();
        }
    }

    /**
     * Time of day in tenths of seconds. Hours are expected to be zero.
     */
    private static int tenths(AbstractCIA cia) {
        assertEquals(0x00, cia.read(0x0B));
        var minutes = bcd(cia.read(0x0A));
        var seconds = bcd(cia.read(0x09));
        var tenths = cia.read(0x08);
        return (minutes * 60 + seconds) * 10 + tenths;
    }

    private static int bcd(int value) {
        return (value >> 4) * 10 + (value & 0x0F);
    }
}
