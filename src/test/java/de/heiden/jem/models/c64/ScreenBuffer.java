package de.heiden.jem.models.c64;

import de.heiden.c64dt.charset.AbstractDecoder;
import de.heiden.c64dt.charset.C64Charset;
import de.heiden.jem.components.bus.BusDevice;

import java.nio.ByteBuffer;
import java.nio.charset.UnmappableCharacterException;

import static org.apache.commons.lang3.StringUtils.stripEnd;

/**
 * Screen capture.
 */
public class ScreenBuffer {
    /**
     * Decoder.
     */
    private final AbstractDecoder decoder = C64Charset.LOWER.newDecoder();

    /**
     * Bus to read from.
     */
    private final BusDevice bus;

    /**
     * Screen address.
     */
    private final int addr;

    /**
     * Constructor for screen capture at $0400.
     */
    public ScreenBuffer(BusDevice bus) {
        this(bus, 0x0400);
    }

    /**
     * Constructor for screen capture at a given address.
     */
    public ScreenBuffer(BusDevice bus, int addr) {
        this.bus = bus;
        this.addr = addr;
    }

    /**
     * Capture screen at $0400.
     */
    public String capture() throws Exception {
        var screen = new StringBuilder(41 * 25);

        int addr = this.addr;
        var bytes = new byte[1];
        for (int r = 0; r < 25; r++) {
            for (int c = 0; c < 40; c++) {
                bytes[0] = (byte) bus.read(addr++);
                try {
                    screen.append(decoder.decode(ByteBuffer.wrap(bytes)));
                } catch (UnmappableCharacterException e) {
                    screen.append("#");
                }
            }
            screen.append('\n');
        }

        return stripEnd(screen.toString(), " \n");
    }
}
