package de.heiden.jem.models.c64.components.vic;

import de.heiden.jem.components.clock.ClockedComponent;
import de.heiden.jem.components.clock.Tick;
import org.serialthreads.Interruptible;

/**
 * Memory access unit of vic.
 * <p>
 * TODO refactor dependencies
 */
class MemAccessUnit implements ClockedComponent {
    private Tick tick;
    private final VIC vic;

    protected int x;
    protected int y;
    protected final int[] characters;
    protected final int[] colors;
    protected final boolean[] spritesDMA;
    protected final int[][] sprites;

    /**
     * Constructor.
     *
     * @param vic
     *         vic this mem access unit belongs to
     */
    MemAccessUnit(VIC vic) {
        this.vic = vic;

        x = 0;
        y = 0;
        characters = new int[40];
        colors = new int[40];
        spritesDMA = new boolean[8];
        sprites = new int[][] {
                new int[3], new int[3], new int[3], new int[3], new int[3], new int[3], new int[3], new int[3]
        };
    }

    @Override
    public void setTick(Tick tick) {
        this.tick = tick;
    }

    @Override
    @Interruptible
    public final void run() {
        vic.reset();

        //noinspection InfiniteLoopStatement
        while (true) {
            loadScreen();
        }
    }

    @Override
    public String getName() {
        return vic.getClass().getSimpleName() + " mem access";
    }

    /**
     * Start unit.
     * Implementation: CPU cycles belong to next VIC cycle.
     */
    @Interruptible
    public final void loadScreen() {
        final var linesPerScreen = vic.linesPerScreen;
        for (var line = 0; line < linesPerScreen; line++) {
            var badline = false;
            var row = 0; // 0-7

            readSprite(3); // cycle 1-2
            readSprite(4); // cycle 3-4
            readSprite(5); // cycle 5-6
            readSprite(6); // cycle 7-8
            readSprite(7); // cycle 9-10
            refresh(); // cycle 11-15

            // cycle 16-55
            var characterAddress = 0; // TODO
            var colorAddress = 0; // TODO
            for (var column = 0; column < 40; column++) {
                if (badline) {
                    var character = vic.bus.read(characterAddress++);
                    var color = vic.colorRam.read(colorAddress++);
                }
                var bitmapAddress = 0; // TODO
                var bitmap = vic.bus.read(bitmapAddress + row);
                waitForTick();
            }

            // 2-4 idle cycles
            idle();

            // PAL +0, NTSC + 1, NTSC + 2
            readSprite(0); // cycle 58-59
            readSprite(1); // cycle 60-61
            readSprite(2); // cycle 62-63
        }
    }

    /**
     * Read sprite data.
     *
     * @param number
     *         number of sprite.
     * @require number >= 0 && number < 8
     */
    @Interruptible
    protected final void readSprite(int number) {
        // TODO BA for next sprites
        var pointer = vic.bus.read(0); // VIC cycle part
        var address = 0 + pointer << 6 + 0; // TODO base and row
        waitForTick();
        if (vic.sprites[number].enabled) {
            // fetch sprite data
            var data = vic.bus.read(address++) & 0xFF; // CPU cycle part
            data <<= 8;
            data |= vic.bus.read(address++) & 0xFF; // VIC cycle part
            data <<= 8;
            waitForTick();
            data |= vic.bus.read(address++) & 0xFF; // CPU cycle part
        } else {
            // idle access
            waitForTick();
        }
    }

    /**
     * DRAM refresh.
     */
    @Interruptible
    protected final void refresh() {
        // TODO BA for badline
        // TODO speed up? (5)
        waitForTick();
        waitForTick();
        waitForTick();
        waitForTick();
        waitForTick();
    }

    /**
     * Idle cycles.
     */
    @Interruptible
    protected final void idle() {
        // TODO BA for next sprites
        // TODO speed up? (65 - cycles per line + 2)
        waitForTick();
        waitForTick();
        if (vic.cyclesPerLine > 63) {
            waitForTick();
        }
        if (vic.cyclesPerLine > 64) {
            waitForTick();
        }
    }

    /**
     * Wait for 1 clock tick.
     */
    @Interruptible
    protected final void waitForTick() {
        tick.waitForTick();
        x += 8;
    }
}
