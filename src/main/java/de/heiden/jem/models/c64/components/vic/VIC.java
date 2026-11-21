package de.heiden.jem.models.c64.components.vic;

import de.heiden.jem.components.bus.BusDevice;
import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.components.ports.OutputPort;
import de.heiden.jem.components.ports.OutputPortImpl;
import de.heiden.jem.models.c64.components.memory.ColorRAM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.IntSupplier;

/**
 * VIC.
 */
public abstract class VIC implements BusDevice {
    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * VIC parameters.
     */
    protected final int linesPerScreen;
    protected final int firstLine_25 = 51;
    protected final int firstLine_24 = 55;
    protected final int lastLine_25 = 250 + 1;
    protected final int lastLine_24 = 246 + 1;
    protected final int firstVBlank;
    protected final int lastVBlank;

    protected final int cyclesPerLine;
    protected final int firstX;
    protected final int firstX_25 = 24;
    protected final int firstX_24 = 31;
    protected final int lastX_25 = 343 + 1;
    protected final int lastX_24 = 334 + 1;
    protected final int firstVisibleX;
    protected final int lastVisibleX;
    protected final int lastX;


    // display unit
    public final AbstractDisplayUnit displayUnit;

    // address mask
    private final int mask;

    // components
    protected final Clock clock;
    public final VICBus bus;
    public final ColorRAM colorRam;

    private final OutputPortImpl irqPort;

    /**
     * Byte of the last access of the CPU to the bus.
     */
    private IntSupplier cpuData = () -> 0xFF;

    /**
     * BA output, connected to RDY of the CPU. Low halts the CPU, e.g. on bad lines.
     */
    private final OutputPortImpl baPort;

    /**
     * Has the display been enabled in raster line $30? Precondition for bad lines.
     */
    private boolean denInLine30 = false;

    /**
     * Is the current raster line a bad line?
     */
    private boolean badLine = false;

    /**
     * Current cycle of the raster line, starting with 1.
     */
    private int cycle;

    /**
     * Refresh counter: Low byte of the address of the next refresh access.
     */
    private int refresh = 0xFF;

    // sprite register
    protected Sprite[] sprites;

    private int regSpritesEnable; // 0x15
    private int regSpritesMSBX; // 0x10
    private int regSpritesExpandY; // 0x17
    private int regSpritesExpandX; // 0x1D
    private int regSpritesMulticolorMode; // 0x1C
    protected byte regSpritesMulticolor0; // 0x25
    protected byte regSpritesMulticolor1; // 0x26

    private int regSpritesSpriteCollision; // 0x1E
    private int regSpritesBackgroundCollision; // 0x1F
    protected int regSpritesBackgroundPriority; // 0x1B

    // control
    protected int regControl1; // 0x11
    protected static final int CONTROL1_BITMAP = 1 << 5;
    protected static final int CONTROL1_EXT_COLOR = 1 << 6;
    protected static final int CONTROL1_25_ROWS = 1 << 3;
    protected static final int CONTROL1_DEN = 1 << 4;
    protected static final int CONTROL1_SCROLL_Y = 0x07;
    protected int regRaster; // read: 0x11 bit 7, 0x12
    protected int regRasterIRQ; // write: 0x11 bit 7, 0x12

    /**
     * Has the raster interrupt been requested, since the raster line matches the raster line of the interrupt?
     */
    private boolean rasterIRQTriggered = false;
    protected int regControl2; // 0x16
    protected static final int CONTROL2_SCROLL_X = 0x07;
    protected static final int CONTROL2_40_COLUMNS = 1 << 3;
    protected static final int CONTROL2_MULTI_COLOR = 1 << 4;

    // base addresses
    private int regBase; // 0x18
    protected int baseCharacterMode; // base address of text screen
    protected int baseBitmapMode; // base address of bitmap screen
    protected int baseCharacterSet; // base address of character set

    // light pen
    private int regStrobeX; // 0x13
    private int regStrobeY; // 0x14

    /**
     * Is the light pen input low?
     */
    private boolean lightPenLow = false;

    /**
     * Has the light pen input gone low, so that the light pen triggers in the next cycle?
     */
    private boolean lightPenTrigger = false;

    /**
     * Has the light pen been triggered in the current frame? It triggers just once per frame.
     */
    private boolean lightPenTriggered = false;

    // irq register
    protected int regInterruptRequest; // 0x19
    protected int regInterruptMask; // 0x1A
    protected static final int INTERRUPT_MASK = 0x0F;
    protected static final int INTERRUPT_RASTER = 1 << 0;
    protected static final int INTERRUPT_SPRITE_BACKGROUND = 1 << 1;
    protected static final int INTERRUPT_SPRITE_SPRITE = 1 << 2;
    protected static final int INTERRUPT_LIGHT_PEN = 1 << 3;
    protected static final int INTERRUPT_ANY = 1 << 7;
    // color register
    protected byte regExteriorColor; // 0x20
    protected byte regBackgroundColor0; // 0x21
    protected byte regBackgroundColor1; // 0x22
    protected byte regBackgroundColor2; // 0x23
    protected byte regBackgroundColor3; // 0x24

    // C128 register
    private int regKeyboard; // 0x2F
    private int regFastMode; // 0x30

    /**
     * Constructor.
     *
     * @param clock
     *         system clock (1 MHz).
     * @param bus
     *         vic memory bus.
     * @param colorRam
     *         color ram.
     * @param cyclesPerLine
     *         system clock cycles per line
     * @param linesPerScreen
     *         lines per screen
     * @param firstVBlank
     * @param lastVBlank
     * @param firstVisibleX
     * @param lastVisibleX
     *         last x value
     * @param lastX
     * @param firstX
     *         X coordinate of the first pixel of cycle 1
     * @require clock != null
     * @require bus != null
     * @require colorRam != null
     */
    public VIC(Clock clock, VICBus bus, ColorRAM colorRam,
            int cyclesPerLine, int linesPerScreen, int firstVBlank, int lastVBlank,
            int firstVisibleX, int lastVisibleX, int lastX, int firstX) {
        assert clock != null : "clock != null";
        assert bus != null : "bus != null";
        assert colorRam != null : "colorRam != null";

        this.clock = clock;
        this.bus = bus;
        this.colorRam = colorRam;

        this.cyclesPerLine = cyclesPerLine;
        this.linesPerScreen = linesPerScreen;
        this.firstVBlank = firstVBlank;
        this.lastVBlank = lastVBlank;
        this.firstVisibleX = (firstVisibleX + 0x007) & 0x01F8;
        this.lastVisibleX = lastVisibleX & 0x01F8;
        this.lastX = lastX;
        this.firstX = firstX;

        // address mask
        mask = 0x3F;

        // prepare sprites
        sprites = new Sprite[8];
        for (var i = 0; i < sprites.length; i++) {
            sprites[i] = new Sprite(i);
        }

        displayUnit = new DisplayUnit(this);

        irqPort = new OutputPortImpl();
        irqPort.setOutputMask(0x01);
        irqPort.setOutputData(0x01);

        baPort = new OutputPortImpl();
        baPort.setOutputMask(0x01);
        baPort.setOutputData(0x01);
    }

    /**
     * Reset VIC.
     */
    public void reset() {
        // TODO what to init?
    }

    /**
     * Address mask.
     *
     * @ensure result >= 0 && result < 0x10000
     */
    public int mask() {
        assert mask >= 0 && mask < 0x10000 : "result >= 0 && result < 0x10000";
        return mask;
    }

    /**
     * IRQ output signal.
     */
    public OutputPort getIRQ() {
        return irqPort;
    }

    /**
     * BA output signal.
     */
    public OutputPort getBA() {
        return baPort;
    }

    /**
     * Is the current raster line a bad line?
     */
    public boolean isBadLine() {
        return badLine;
    }

    /**
     * Update the bad line condition and BA at the start of a cycle of the current raster line.
     * A bad line is a raster line in $30-$F7 whose lower 3 bits match the vertical scroll,
     * if the display has been enabled in raster line $30.
     * BA is low on bad lines and for the sprite data accesses, see {@link #isBALow(int, int, boolean)}.
     *
     * @param cycle
     *         number of the current cycle, starting with 1
     */
    protected final void updateBA(int cycle) {
        var raster = regRaster;
        if (raster == 0x30 && (regControl1 & CONTROL1_DEN) != 0) {
            denInLine30 = true;
        } else if (raster == 0) {
            denInLine30 = false;
        }
        badLine = denInLine30 && raster >= 0x30 && raster <= 0xF7 && (raster & 0x07) == (regControl1 & CONTROL1_SCROLL_Y);

        var ba = isBALow(raster, cycle, badLine);
        var value = ba ? 0x00 : 0x01;
        if (value != baPort.outputData()) {
            baPort.setOutputData(value);
        }
    }

    /**
     * Is BA low in the given cycle?
     * BA is low in cycles 12-54 of bad lines, because the VIC reads the video matrix,
     * and from 3 cycles before up to the 2 cycles in which the VIC reads the data of a sprite with DMA on,
     * see {@link #spriteAccessCycle(int)}.
     *
     * @param raster
     *         raster line
     * @param cycle
     *         cycle, starting with 1
     * @param badLine
     *         is the raster line a bad line?
     */
    private boolean isBALow(int raster, int cycle, boolean badLine) {
        if (badLine && cycle >= 12 && cycle <= 54) {
            return true;
        }

        for (var i = 0; i < 8; i++) {
            if (sprites[i].dma) {
                // The 5 cycles may span the end of the line.
                var first = spriteAccessCycle(i);
                if ((cycle - first + 3 + cyclesPerLine) % cyclesPerLine <= 4) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * First of the 2 cycles in which the VIC reads the pointer and the data of a sprite.
     * The sprites are read one after another at the end of a line and at the start of the next line:
     * Sprite 0 from cycle 58 of the PAL 6569 and from cycle 59 of the NTSC 6567s.
     * So sprite 3 is read from cycle 1 (6569, 6567R56A) or from the last cycle 65 of the line (6567R8).
     */
    final int spriteAccessCycle(int number) {
        var cycle = (cyclesPerLine == 63 ? 58 : 59) + 2 * number;
        return cycle > cyclesPerLine ? cycle - cyclesPerLine : cycle;
    }

    /**
     * X coordinate of the first pixel of a cycle.
     * The X coordinates of the cycles 13 and up are the same for all VICs.
     * The 6567R8 has 8 pixels more per line than X coordinates, it repeats the X coordinates of cycle 62 in cycle 63.
     *
     * @param cycle
     *         cycle, starting with 1. Cycles below 1 are cycles of the previous line.
     */
    final int x(int cycle) {
        if (cycle < 1) {
            cycle += cyclesPerLine;
        }
        var x = firstX + (cycle - 1) * 8;
        if (cycle > REPEATED_X_CYCLE) {
            x -= cyclesPerLine * 8 - lastX;
        }
        return x % lastX;
    }

    /**
     * Cycle, whose X coordinates are repeated in the next cycle, if a line has more pixels than X coordinates.
     */
    private static final int REPEATED_X_CYCLE = 62;

    /**
     * Execute the sprite sequencer for a cycle of the current raster line, as described by Christian Bauer:
     * DMA is turned on in cycles 55 and 56, if the Y coordinate matches the raster line.
     * The expansion flip-flop of y expanded sprites is toggled in cycle 56.
     * The data counters are loaded in cycle 58, the display is turned on in cycle 58, if the Y coordinate matches.
     * In the first access cycle of a sprite, the VIC reads its pointer.
     * In the 2 access cycles of a sprite with DMA on, the VIC reads the 3 bytes of its next line.
     * In cycle 16, the data counter base is loaded from the data counter, unless the expansion flip-flop doubles the line.
     * DMA is turned off, when all 21 lines have been read, the display is turned off in the following cycle 58.
     * <p>
     * In the first phase of cycles without sprite or graphics access, the VIC refreshes the RAM (cycles 11 to 15)
     * or does an idle access to $3FFF. The CPU reads the byte of the first phase from the open bus.
     *
     * @param raster
     *         raster line
     * @param cycle
     *         cycle, starting with 1
     */
    protected final void spriteCycle(int raster, int cycle) {
        this.cycle = cycle;
        completeSpriteData();

        switch (cycle) {
            case 16 -> {
                for (var sprite : sprites) {
                    if (sprite.expansion) {
                        sprite.mcBase = sprite.mc;
                        if (sprite.mcBase == 63) {
                            sprite.dma = false;
                        }
                    }
                }
            }
            case 55 -> startSpriteDMA(raster);
            case 56 -> {
                startSpriteDMA(raster);
                for (var sprite : sprites) {
                    if (sprite.dma && sprite.expandY) {
                        sprite.expansion = !sprite.expansion;
                    }
                }
            }
            case 58 -> {
                for (var sprite : sprites) {
                    sprite.mc = sprite.mcBase;
                    if (!sprite.dma) {
                        sprite.display = false;
                    } else if (sprite.enabled && sprite.y == (raster & 0xFF)) {
                        sprite.display = true;
                    }
                }
            }
            default -> {
                // nothing to do
            }
        }

        // Graphics are accessed in cycles 16 to 55.
        var idle = cycle < 16 || cycle > 55;

        if (cycle >= 11 && cycle <= 15) {
            if (raster == 0 && cycle == 11) {
                refresh = 0xFF;
            }
            bus.read(0x3F00 | refresh);
            refresh = (refresh - 1) & 0xFF;
            idle = false;
        }

        // The pointer of a sprite is read in the first of its 2 access cycles, its data in the second.
        for (var i = 0; i < 8; i++) {
            var sprite = sprites[i];
            var access = spriteAccessCycle(i);
            if (cycle == access) {
                sprite.pointer = bus.read(baseCharacterMode + 0x03F8 + i);
                idle = false;
            }
            if (cycle != access % cyclesPerLine + 1) {
                continue;
            }
            sprite.armed = true;
            if (sprite.dma) {
                // The CPU is halted, so it does not matter, that the last byte is not read in the first phase.
                var address = sprite.pointer << 6;
                var data = 0;
                for (var b = 0; b < 3; b++) {
                    data = data << 8 | bus.read(address + sprite.mc);
                    sprite.mc = (sprite.mc + 1) & 0x3F;
                }
                sprite.data = data;
                idle = false;
            } else {
                // Without DMA the data is loaded nevertheless: With the bytes on the CPU bus in the second phases
                // of both access cycles and the idle access of the VIC in between.
                // Such a sprite is visible, if its display is turned on in the line of its Y coordinate.
                sprite.data = cpuData.getAsInt() << 16 | bus.read(0x3FFF) << 8 | 0xFF;
                sprite.dataPending = true;
                idle = false;
            }
        }

        if (idle) {
            bus.read(0x3FFF);
        }
    }

    /**
     * Complete the data of sprites without DMA with the byte on the CPU bus in their second access cycle.
     * To be called at the start of the following cycle, before the sprites are displayed.
     */
    final void completeSpriteData() {
        for (var sprite : sprites) {
            if (sprite.dataPending) {
                sprite.dataPending = false;
                sprite.data = sprite.data & 0xFFFF00 | cpuData.getAsInt();
            }
        }
    }

    /**
     * Connect the CPU bus.
     *
     * @param cpuData
     *         byte of the last access of the CPU to the bus
     */
    public void connectCpuData(IntSupplier cpuData) {
        this.cpuData = cpuData;
    }

    /**
     * Turn on the DMA of enabled sprites whose Y coordinate matches the raster line.
     */
    private void startSpriteDMA(int raster) {
        for (var sprite : sprites) {
            if (sprite.enabled && !sprite.dma && sprite.y == (raster & 0xFF)) {
                sprite.dma = true;
                sprite.mcBase = 0;
                sprite.expansion = true;
            }
        }
    }

    /**
     * Write byte to bus device.
     *
     * @param value
     *         byte to write
     * @param address
     *         address to write byte to
     * @require value >= 0 && value < 0x100
     */
    @Override
    public void write(int value, int address) {
        assert value >= 0 && value < 0x100 : "value >= 0 && value < 0x100";

        var a = address & mask;
        switch (a) {
            case 0x00, 0x02, 0x04, 0x06, 0x08, 0x0A, 0x0C, 0x0E -> sprites[a >> 1].setXLSB(value);
            case 0x01, 0x03, 0x05, 0x07, 0x09, 0x0B, 0x0D, 0x0F -> sprites[a >> 1].y = value;
            case 0x10 -> {
                regSpritesMSBX = value;
                for (var sprite : sprites) {
                    sprite.setXMSB(value);
                }
            }
            case 0x11 -> {
                regControl1 = value;
                // bit 7 is high bit of raster irq line
                regRasterIRQ = regRasterIRQ & 0xFF | (value & 0x80) << 1;
                displayUnit.graphicsChanged();

                if (logger.isDebugEnabled()) {
                    var bitmapMode = (regControl1 & VIC.CONTROL1_BITMAP) != 0;
                    var extColorMode = (regControl1 & VIC.CONTROL1_EXT_COLOR) != 0;
                    var multiColorMode = (regControl2 & VIC.CONTROL2_MULTI_COLOR) != 0;
                    logger.debug("bitmap: {}, extended color: {}, multi color: {}", bitmapMode, extColorMode, multiColorMode);
                }
            }
            case 0x12 -> regRasterIRQ = regRasterIRQ & 0x0100 | value;
            case 0x13, 0x14 -> {
                // The light pen registers are read only.
            }
            case 0x15 -> {
                regSpritesEnable = value;
                for (var sprite : sprites) {
                    sprite.enable(value);
                }
            }
            case 0x16 -> {
                regControl2 = value;
                displayUnit.graphicsChanged();

                if (logger.isDebugEnabled()) {
                    var bitmapMode = (regControl1 & VIC.CONTROL1_BITMAP) != 0;
                    var extColorMode = (regControl1 & VIC.CONTROL1_EXT_COLOR) != 0;
                    var multiColorMode = (regControl2 & VIC.CONTROL2_MULTI_COLOR) != 0;
                    logger.debug("bitmap: {}, extended color: {}, multi color: {}", bitmapMode, extColorMode, multiColorMode);
                }
            }
            case 0x17 -> {
                regSpritesExpandY = value;
                for (var sprite : sprites) {
                    sprite.setExpandY(value);
                    if (!sprite.expandY && !sprite.expansion) {
                        if (cycle == 15) {
                            // Sprite crunch: Clearing the y expansion in cycle 15 scrambles the data counter,
                            // which is loaded into the data counter base in cycle 16.
                            sprite.mc = 0x2A & (sprite.mcBase & sprite.mc) | 0x15 & (sprite.mcBase | sprite.mc);
                        }
                        // The expansion flip-flop is set as long as the sprite is not y expanded.
                        sprite.expansion = true;
                    }
                }
            }
            case 0x18 -> {
                regBase = value | 0x01; // set unused bits to 1;
                updateBaseAddresses();
            }
            case 0x19 -> {
                // 1-bits in value clear the corresponding interrupt bits in the irq register
                regInterruptRequest &= ~(value & INTERRUPT_MASK);
                updateIRQ();
            }
            case 0x1A -> {
                regInterruptMask = value & INTERRUPT_MASK;
                updateIRQ();
            }
            case 0x1B -> regSpritesBackgroundPriority = value;
            case 0x1C -> {
                regSpritesMulticolorMode = value;
                for (var sprite : sprites) {
                    sprite.setMulticolor(value);
                }
            }
            case 0x1D -> {
                regSpritesExpandX = value;
                for (var sprite : sprites) {
                    sprite.setExpandX(value);
                }
            }
            case 0x1E, 0x1F -> {
                // The collision registers are read only.
            }
            case 0x20 -> regExteriorColor = (byte) (value & 0x0F);
            case 0x21 -> {
                regBackgroundColor0 = (byte) (value & 0x0F);
                displayUnit.graphicsChanged();
            }
            case 0x22 -> {
                regBackgroundColor1 = (byte) (value & 0x0F);
                displayUnit.graphicsChanged();
            }
            case 0x23 -> {
                regBackgroundColor2 = (byte) (value & 0x0F);
                displayUnit.graphicsChanged();
            }
            case 0x24 -> {
                regBackgroundColor3 = (byte) (value & 0x0F);
                displayUnit.graphicsChanged();
            }
            case 0x25 -> {
                var color = (byte) (value & 0x0F);
                regSpritesMulticolor0 = color;
                for (var sprite : sprites) {
                    sprite.multicolor1 = color;
                }
            }
            case 0x26 -> {
                var color = (byte) (value & 0x0F);
                regSpritesMulticolor1 = color;
                for (var sprite : sprites) {
                    sprite.multicolor2 = color;
                }
            }
            case 0x27, 0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2D, 0x2E -> sprites[a - 0x27].color = (byte) (value & 0x0F);
            case 0x2F -> regKeyboard = value;
            case 0x30 -> regFastMode = value;
            default -> {
                // ignore
            }
        }
    }

    /**
     * Read byte from bus device.
     *
     * @param address
     *         address to read byte from
     * @ensure result >= 0 && result < 0x100
     */
    @Override
    public int read(int address) {
        var a = address & mask;
        var result = switch (a) {
            case 0x00, 0x02, 0x04, 0x06, 0x08, 0x0A, 0x0C, 0x0E -> sprites[a >> 1].getXLSB();
            case 0x01, 0x03, 0x05, 0x07, 0x09, 0x0B, 0x0D, 0x0F -> sprites[a >> 1].y;
            case 0x10 -> regSpritesMSBX;
            // TODO 2010-03-17 mh: correct?
            // bit 7 is high bit of current raster line
            case 0x11 -> regControl1 & 0x7F | (regRaster & 0x0100) >> 1;
            case 0x12 -> regRaster & 0xFF;
            case 0x13 -> regStrobeX;
            case 0x14 -> regStrobeY;
            case 0x15 -> regSpritesEnable;
            case 0x16 -> regControl2 | 0xC0; // unused bits are 1
            case 0x17 -> regSpritesExpandY;
            case 0x18 -> regBase;
            case 0x19 -> {
                var irq = regInterruptRequest | 0x70; // unused bits are 1
                if ((regInterruptRequest & regInterruptMask) != 0) {
                    irq |= INTERRUPT_ANY;
                }
                yield irq;
            }
            case 0x1A -> regInterruptMask | 0xF0; // unused bits are 1
            case 0x1B -> regSpritesBackgroundPriority;
            case 0x1C -> regSpritesMulticolorMode;
            case 0x1D -> regSpritesExpandX;
            case 0x1E -> {
                // Reading clears the collisions.
                var collisions = regSpritesSpriteCollision;
                regSpritesSpriteCollision = 0;
                displayUnit.spriteCollisionsRead();
                yield collisions;
            }
            case 0x1F -> {
                // Reading clears the collisions.
                var collisions = regSpritesBackgroundCollision;
                regSpritesBackgroundCollision = 0;
                displayUnit.backgroundCollisionsRead();
                yield collisions;
            }
            // unused bits are 1
            case 0x20 -> regExteriorColor | 0xF0;
            case 0x21 -> regBackgroundColor0 | 0xF0;
            case 0x22 -> regBackgroundColor1 | 0xF0;
            case 0x23 -> regBackgroundColor2 | 0xF0;
            case 0x24 -> regBackgroundColor3 | 0xF0;
            case 0x25 -> regSpritesMulticolor0 | 0xF0;
            case 0x26 -> regSpritesMulticolor1 | 0xF0;
            case 0x27, 0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2D, 0x2E -> sprites[a - 0x27].color | 0xF0;
            case 0x2F -> regKeyboard;
            case 0x30 -> regFastMode;
            default -> 0xFF;
        };

        assert result >= 0 && result < 0x100 : "result >= 0 && result < 0x100";
        return result;
    }

    //
    // protected
    //

    /**
     * Sets to current raster line.
     *
     * @param line
     *         raster line
     */
    protected void setRasterLine(int line) {
        regRaster = line;
    }

    /**
     * Compare the current raster line with the raster line of the raster interrupt in a cycle, as VICE does:
     * The interrupt is requested, when the comparison changes from no match to match.
     * So changing the raster line of the interrupt to the current raster line requests it in the following cycle.
     * To be called in each cycle after {@link #setRasterLine(int)}.
     */
    final void rasterCompareCycle() {
        if (regRaster != regRasterIRQ) {
            rasterIRQTriggered = false;
            return;
        }
        if (rasterIRQTriggered) {
            return;
        }
        rasterIRQTriggered = true;
        regInterruptRequest |= INTERRUPT_RASTER;
        updateIRQ();

        logger.debug("Raster irq at line {} at tick {}", regRaster, clock.getTick());
    }

    /**
     * Set the light pen input, connected to PB4 of CIA 1 and the fire button of joystick port 1.
     * A falling edge triggers the light pen in the next cycle.
     *
     * @param low
     *         is the light pen input low?
     */
    public final void setLightPen(boolean low) {
        if (low && !lightPenLow) {
            lightPenTrigger = true;
        }
        lightPenLow = low;
    }

    /**
     * Execute the light pen for a cycle of the current raster line.
     * At the start of a frame, the light pen triggers again, if its input is still low.
     *
     * @param raster
     *         raster line
     * @param cycle
     *         cycle, starting with 1
     */
    final void lightPenCycle(int raster, int cycle) {
        if (raster == 0 && cycle == 2) {
            lightPenTriggered = false;
            if (lightPenLow) {
                triggerLightPen(cycle, true);
            }
        }
        if (lightPenTrigger) {
            triggerLightPen(cycle, false);
        }
    }

    /**
     * Trigger the light pen: Latch the current position and request an interrupt, as VICE does.
     *
     * @param cycle
     *         cycle, starting with 1
     * @param startOfFrame
     *         is the light pen triggered at the start of the frame, because its input is still low?
     */
    private void triggerLightPen(int cycle, boolean startOfFrame) {
        lightPenTrigger = false;
        if (lightPenTriggered) {
            return;
        }
        lightPenTriggered = true;

        // The light pen does not trigger in the last line, except in its first cycle.
        if (regRaster == linesPerScreen - 1 && cycle > 1) {
            return;
        }

        if (startOfFrame) {
            regStrobeX = cyclesPerLine == 65 ? 0xD5 : 0xD1;
        } else {
            // The X coordinate of the cycle in units of 8 pixels, divided by 2,
            // plus 2 because of the color latency of the 6569 and 6567.
            regStrobeX = ((x(cycle) & ~0x07) / 2 + 2) & 0xFF;
        }
        regStrobeY = regRaster & 0xFF;

        if (!lightPenIrqAtStartOfFrameOnly() || startOfFrame) {
            regInterruptRequest |= INTERRUPT_LIGHT_PEN;
            updateIRQ();
        }
    }

    /**
     * Does the light pen request an interrupt just at the start of a frame, if its input is low?
     * That is the case for the old VICs 6569R1 and 6567R56A.
     */
    protected boolean lightPenIrqAtStartOfFrameOnly() {
        return false;
    }

    /**
     * Sprites collided with other sprites and / or with the foreground of the graphics.
     * The first collision after reading the collision register requests an interrupt.
     *
     * @param sprites
     *         bit mask of the sprites which collided with other sprites
     * @param background
     *         bit mask of the sprites which collided with the foreground of the graphics
     */
    protected final void spriteCollisions(int sprites, int background) {
        if (sprites != 0) {
            if (regSpritesSpriteCollision == 0) {
                regInterruptRequest |= INTERRUPT_SPRITE_SPRITE;
                updateIRQ();
            }
            regSpritesSpriteCollision |= sprites;
        }
        if (background != 0) {
            if (regSpritesBackgroundCollision == 0) {
                regInterruptRequest |= INTERRUPT_SPRITE_BACKGROUND;
                updateIRQ();
            }
            regSpritesBackgroundCollision |= background;
        }
    }

    /**
     * Update the IRQ output: It is low, as long as an enabled interrupt is requested.
     */
    private void updateIRQ() {
        var value = (regInterruptRequest & regInterruptMask) != 0 ? 0x0 : 0x1;
        if (value != irqPort.outputData()) {
            irqPort.setOutputData(value);
        }
    }

    /**
     * Set base register values.
     */
    protected void updateBaseAddresses() {
        baseCharacterMode = (regBase & 0xF0) << 6;
        baseBitmapMode = (regBase & 0x08) << 10;
        baseCharacterSet = (regBase & 0x0E) << 10; // TODO what about bit 1?
    }
}
