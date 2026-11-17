package de.heiden.jem.models.c64.components.keyboard;

import de.heiden.jem.components.bus.BusDevice;
import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.components.clock.ClockEvent;
import jakarta.annotation.Nonnull;

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
     *         PETSCII code
     * @param tick
     *         tick, before which the key is not typed
     */
    private record Key(int petscii, long tick) {}

    /**
     * Constructor.
     *
     * @param clock
     *         clock
     * @param bus
     *         CPU bus
     */
    public KeyboardBuffer(@Nonnull Clock clock, @Nonnull BusDevice bus) {
        super("Keyboard buffer");

        this.clock = clock;
        this.bus = bus;

        clock.addClockEvent(INTERVAL, this);
    }

    /**
     * Type the given text. Characters without PETSCII equivalent are skipped.
     * May be called from any thread.
     */
    public void type(@Nonnull String text) {
        type(text, 0);
    }

    /**
     * Type the given text, but not before the given tick. Characters without PETSCII equivalent are skipped.
     * A tick in the future types the text at a reproducible point in time. May be called from any thread.
     */
    public void type(@Nonnull String text, long tick) {
        var previous = ' ';
        for (var c : text.toCharArray()) {
            // Windows line separator "\r\n": Type only one return.
            if (!(c == '\n' && previous == '\r')) {
                var petscii = toPetscii(c);
                if (petscii >= 0) {
                    pending.add(new Key(petscii, tick));
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

        clock.addClockEvent(tick + INTERVAL, this);
    }

    /**
     * PETSCII code of a character for the upper case / graphics mode.
     * Letters of both cases are typed unshifted, so that pasted BASIC commands work in either case.
     *
     * @return PETSCII code or -1, if there is no equivalent
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
