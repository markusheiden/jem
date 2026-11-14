package de.heiden.jem.models.c64.components.vic;

import org.serialthreads.Interruptible;

/**
 * Display unit of vic.
 * <p>
 * TODO refactor dependencies
 */
public class DisplayUnit extends AbstractDisplayUnit {
    /**
     * Hidden constructor.
     *
     * @param vic
     *         vic this display unit belongs to
     */
    DisplayUnit(VIC vic) {
        super(vic,
                (vic.lastVBlank + 1) * vic.cyclesPerLine * 8,
                vic.cyclesPerLine * 8, vic.linesPerScreen,
                vic.lastX - vic.firstVisibleX + vic.lastVisibleX, vic.firstVBlank - (vic.lastVBlank + 1));
    }

    @Override
    @Interruptible
    public final void run() {
        vic.reset();

        //noinspection InfiniteLoopStatement
        while (true) {
            var screen = screenRender;

            var linesPerScreen = vic.linesPerScreen;
            var cyclesPerLine = vic.cyclesPerLine;

            var borderY = true;
            var borderX = true;

            // first left part of first line is not available, because there is no previous line
            var ptr = vic.lastX - vic.firstVisibleX;
            for (var line = 0; line < linesPerScreen; line++) {
                // turn y border on / off
                if (line == vic.firstLine_25) {
                    borderY = false;
                } else if (line == vic.lastLine_25) {
                    borderY = true;
                }

                for (var column = 0; column < cyclesPerLine * 8; column++) {
                    if (column == vic.firstX_25) {
                        borderX = false;
                    } else if (column == vic.lastX_25) {
                        borderX = true;
                    }

                    if (borderY || borderX) {
                        screen[ptr++] = vic.regExteriorColor;
                    } else {
                        screen[ptr++] = 1;
                    }

                    tick.waitForTick();
                }

/*
        readSprite(3);
        readSprite(4);
        readSprite(5);
        readSprite(6);
        readSprite(7);
        for (var refresh = 0; refresh < 5; refresh++)
        {
          tick.waitForTick();
        }
        readLine();
        // TODO 2010-03-15 mh: correct number of idle cycles for ntsc
        for (var idle = 0; idle < 2; idle++)
        {
          tick.waitForTick();
        }
        readSprite(0);
        readSprite(1);
        readSprite(2);
*/
            }

            rendered(screen);
        }
    }

    @Interruptible
    private void readLine() {

    }

    @Interruptible
    private void readSprite(int i) {
        // TODO 2010-03-15 mh: sprite pointer access
        tick.waitForTick();
        // TODO 2010-03-15 mh: 3 sprite data access / idle access
        tick.waitForTick();
    }
}
