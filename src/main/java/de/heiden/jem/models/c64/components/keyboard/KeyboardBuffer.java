package de.heiden.jem.models.c64.components.keyboard;

import de.heiden.jem.components.bus.BusDevice;
import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.components.clock.ClockEvent;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Types text, e.g. pasted text, via the keyboard buffer of the KERNAL.
 * Works for everything that reads keys via the KERNAL,
 * e.g. the BASIC editor, but not for programs scanning the keyboard themselves.
 */
public class KeyboardBuffer extends ClockEvent {
    /**
     * Address of the keyboard buffer.
     */
    private static final int BUFFER = 0x0277;

    /**
     * Address of the number of characters in the keyboard buffer.
     */
    private static final int COUNT = 0x00C6;

    /**
     * Size of the keyboard buffer.
     */
    private static final int SIZE = 10;

    /**
     * Ticks between checks whether the keyboard buffer is empty: About one PAL frame.
     */
    private static final int INTERVAL = 63 * 312;

    /**
     * Clock.
     */
    private final Clock clock;

    /**
     * Bus to access the keyboard buffer.
     */
    private final BusDevice bus;

    /**
     * Keys still to type. Filled by any thread, consumed by the C64 thread.
     */
    private final Queue<Key> pending = new ConcurrentLinkedQueue<>();

    /**
     * Key to type.
     *
     * @param petscii
     *         PETSCII code.
     * @param tick
     *         Tick, before which the key is not typed.
     * @param exact
     *         Type the key exactly at the tick instead of at the next check?
     */
    private record Key(int petscii, long tick, boolean exact) {}

    /**
     * Tick of the next regular check.
     */
    private long nextCheck = INTERVAL;

    /**
     * Constructor.
     *
     * @param clock
     *         Clock.
     * @param bus
     *         CPU bus.
     */
    public KeyboardBuffer(Clock clock, BusDevice bus) {
        super("Keyboard buffer");

        this.clock = clock;
        this.bus = bus;

        clock.addClockEvent(INTERVAL, this);
    }

    /**
     * Type the given text. Characters without PETSCII equivalent are skipped.
     * May be called from any thread.
     */
    public void type(String text) {
        type(text, 0);
    }

    /**
     * Type the given text, but not before the given tick. Characters without PETSCII equivalent are skipped.
     * A tick in the future types the text at a reproducible point in time. May be called from any thread.
     */
    public void type(String text, long tick) {
        type(text, tick, false);
    }

    /**
     * Type the given text exactly at the given tick, which has to be in the future.
     * This defines the phase of the typing relative to the raster beam, {@link #type(String, long)} types
     * always at the same position of the raster beam. May be called from any thread.
     */
    public void typeAt(String text, long tick) {
        type(text, tick, true);
    }

    private void type(String text, long tick, boolean exact) {
        var previous = ' ';
        for (var c : text.toCharArray()) {
            // Windows line separator "\r\n": Type only one return.
            if (!(c == '\n' && previous == '\r')) {
                var petscii = toPetscii(c);
                if (petscii >= 0) {
                    pending.add(new Key(petscii, tick, exact));
                }
            }
            previous = c;
        }
    }

    @Override
    public void execute(long tick) {
        if (!pending.isEmpty() && bus.read(COUNT) == 0) {
            var count = 0;
            for (Key key; count < SIZE && (key = pending.peek()) != null && key.tick <= tick; count++) {
                pending.remove();
                bus.write(key.petscii, BUFFER + count);
            }
            if (count > 0) {
                bus.write(count, COUNT);
            }
        }

        if (tick >= nextCheck) {
            nextCheck += INTERVAL;
        }
        // Exactly timed keys are typed between the regular checks.
        var key = pending.peek();
        var exact = key != null && key.exact && key.tick > tick && key.tick < nextCheck;
        clock.addClockEvent(exact ? key.tick : nextCheck, this);
    }

    /**
     * PETSCII code of a character for the upper case / graphics mode.
     * Letters of both cases are typed unshifted, so that pasted BASIC commands work in either case.
     *
     * @return PETSCII code or -1, if there is no equivalent.
     */
    static int toPetscii(char c) {
        if (c >= 'a' && c <= 'z') {
            return c - 'a' + 'A';
        }
        if (c >= ' ' && c <= ']') {
            return c;
        }
        return switch (c) {
            case '\n', '\r' -> 0x0D;
            // Commodore + P, the underscore of file names.
            case '_' -> 0xAF;
            case '£' -> 0x5C;
            case '^', '↑' -> 0x5E;
            case '←' -> 0x5F;
            case 'π' -> 0xFF;
            default -> -1;
        };
    }
}
