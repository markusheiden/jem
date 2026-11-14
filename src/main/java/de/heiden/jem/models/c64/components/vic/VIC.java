package de.heiden.jem.models.c64.components.vic;

import de.heiden.jem.components.bus.BusDevice;
import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.components.ports.OutputPort;
import de.heiden.jem.components.ports.OutputPortImpl;
import de.heiden.jem.models.c64.components.memory.ColorRAM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    protected final int firstX_25 = 24;
    protected final int firstX_24 = 31;
    protected final int lastX_25 = 343 + 1;
    protected final int lastX_24 = 334 + 1;
    protected final int firstVisibleX;
    protected final int lastVisibleX;
    protected final int lastX;


    // mem access unit
    private final MemAccessUnit memAccessUnit;

    // display unit
    public final AbstractDisplayUnit displayUnit;

    // address mask
    private final int mask;

    // components
    protected final Clock clock;
    public final VICBus bus;
    public final ColorRAM colorRam;

    private final OutputPortImpl irqPort;

    // sprite register
    protected Sprite[] sprites;

    private int regSpritesEnable; // 0x15
    private int regSpritesMSBX; // 0x11
    private int regSpritesExpandX; // 0x17
    private int regSpritesExpandY; // 0x1D
    private int regSpritesMulticolorMode; // 0x1C
    protected byte regSpritesMulticolor0; // 0x15
    protected byte regSpritesMulticolor1; // 0x16

    private int regSpritesSpriteCollision; // 0x1E
    private int regSpritesBackgroundCollision; // 0x1F
    protected int regSpritesBackgroundPriority; // 0x1B

    // control
    protected int regControl1; // 0x11
    protected static final int CONTROL1_BITMAP = 1 << 5;
    protected static final int CONTROL1_EXT_COLOR = 1 << 6;
    protected static final int CONTROL1_25_ROWS = 1 << 3;
    protected static final int CONTROL1_SCROLL_Y = 0x07;
    protected int regRaster; // read: 0x11 bit 7, 0x12
    protected int regRasterIRQ; // write: 0x11 bit 7, 0x12
    protected int regControl2; // 0x16
    protected static final int CONTROL2_SCROLL_X = 0x07;
    protected static final int CONTROL2_40_COLUMNS = 1 << 3;
    protected static final int CONTROL2_MULTI_COLOR = 1 << 4;

    // base addresses
    private int regBase; // 0x18
    protected int baseCharacterMode; // base address of text screen
    protected int baseBitmapMode; // base address of bitmap screen
    protected int baseCharacterSet; // base address of character set

    // strobe
    private int regStrobeX; // 0x13
    private int regStrobeY; // 0x14

    // irq register
    protected int regInterruptRequest; // 0x19
    protected int regInterruptMask; // 0x1A
    protected static final int INTERRUPT_MASK = 0x0F;
    protected static final int INTERRUPT_RASTER = 1 << 0;
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
     * @require clock != null
     * @require bus != null
     * @require colorRam != null
     */
    public VIC(Clock clock, VICBus bus, ColorRAM colorRam,
            int cyclesPerLine, int linesPerScreen, int firstVBlank, int lastVBlank,
            int firstVisibleX, int lastVisibleX, int lastX) {
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

        // address mask
        mask = 0x3F;

        // prepare sprites
        sprites = new Sprite[8];
        for (var i = 0; i < sprites.length; i++) {
            sprites[i] = new Sprite(i);
        }

        logger.debug("start vic mem access unit");
        memAccessUnit = clock.addClockedComponent(Clock.VIC_MEM, new MemAccessUnit(this));

        logger.debug("start vic display unit");
        displayUnit = clock.addClockedComponent(Clock.VIC_DISPLAY, new DisplayUnitSimple(this));

        irqPort = new OutputPortImpl();
        irqPort.setOutputMask(0x01);
        irqPort.setOutputData(0x01);
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

        switch (address & mask) {
            case 0x00: {
                sprites[0].setXLSB(value);
                break;
            }
            case 0x01: {
                sprites[0].y = value;
                break;
            }
            case 0x02: {
                sprites[1].setXLSB(value);
                break;
            }
            case 0x03: {
                sprites[1].y = value;
                break;
            }
            case 0x04: {
                sprites[2].setXLSB(value);
                break;
            }
            case 0x05: {
                sprites[2].y = value;
                break;
            }
            case 0x06: {
                sprites[3].setXLSB(value);
                break;
            }
            case 0x07: {
                sprites[3].y = value;
                break;
            }
            case 0x08: {
                sprites[4].setXLSB(value);
                break;
            }
            case 0x09: {
                sprites[4].y = value;
                break;
            }
            case 0x0A: {
                sprites[5].setXLSB(value);
                break;
            }
            case 0x0B: {
                sprites[5].y = value;
                break;
            }
            case 0x0C: {
                sprites[6].setXLSB(value);
                break;
            }
            case 0x0D: {
                sprites[6].y = value;
                break;
            }
            case 0x0E: {
                sprites[7].setXLSB(value);
                break;
            }
            case 0x0F: {
                sprites[7].y = value;
                break;
            }
            case 0x10: {
                regSpritesMSBX = value;
                for (var sprite : sprites) {
                    sprite.setXMSB(value);
                }
                break;
            }
            case 0x11: {
                regControl1 = value;
                // bit 7 is high bit of raster irq line
                regRasterIRQ = regRasterIRQ & 0xFF | (value & 0x80) << 1;

                if (logger.isDebugEnabled()) {
                    var bitmapMode = (regControl1 & VIC.CONTROL1_BITMAP) != 0;
                    var extColorMode = (regControl1 & VIC.CONTROL1_EXT_COLOR) != 0;
                    var multiColorMode = (regControl2 & VIC.CONTROL2_MULTI_COLOR) != 0;
                    logger.debug("bitmap: {}, extended color: {}, multi color: {}", bitmapMode, extColorMode, multiColorMode);
                }

                break;
            }
            case 0x12: {
                regRasterIRQ = regRasterIRQ & 0x0100 | value;
                break;
            }
            case 0x13: {
                regStrobeX = value;
                break;
            }
            case 0x14: {
                regStrobeY = value;
                break;
            }
            case 0x15: {
                regSpritesEnable = value;
                for (var sprite : sprites) {
                    sprite.enable(value);
                }
                break;
            }
            case 0x16: {
                regControl2 = value;

                if (logger.isDebugEnabled()) {
                    var bitmapMode = (regControl1 & VIC.CONTROL1_BITMAP) != 0;
                    var extColorMode = (regControl1 & VIC.CONTROL1_EXT_COLOR) != 0;
                    var multiColorMode = (regControl2 & VIC.CONTROL2_MULTI_COLOR) != 0;
                    logger.debug("bitmap: {}, extended color: {}, multi color: {}", bitmapMode, extColorMode, multiColorMode);
                }

                break;
            }
            case 0x17: {
                regSpritesExpandX = value;
                for (var sprite : sprites) {
                    sprite.setExpandX(value);
                }
                break;
            }
            case 0x18: {
                regBase = value | 0x01; // set unused bits to 1;
                updateBaseAddresses();
                break;
            }
            case 0x19: {
                // 1-bits in value disable the corresponding interrupt bits in the irq register
                regInterruptRequest &= ~(value & INTERRUPT_MASK);
                if ((regInterruptRequest & INTERRUPT_MASK) == 0) {
                    // no pending irq anymore:

                    // clear "master" interrupt bit
                    regInterruptRequest = 0x00;
                    // reset irq
                    irqPort.setOutputData(0x1);
                }
                break;
            }
            case 0x1A: {
                regInterruptMask = value;
                break;
            }
            case 0x1B: {
                regSpritesBackgroundPriority = value;
                break;
            }
            case 0x1C: {
                regSpritesMulticolorMode = value;
                for (var sprite : sprites) {
                    sprite.setMulticolor(value);
                }
                break;
            }
            case 0x1D: {
                regSpritesExpandY = value;
                for (var sprite : sprites) {
                    sprite.setExpandY(value);
                }
                break;
            }
            case 0x1E: {
                regSpritesSpriteCollision = value;
                break;
            }
            case 0x1F: {
                regSpritesBackgroundCollision = value;
                break;
            }
            case 0x20: {
                regExteriorColor = (byte) (value & 0x0F);
                break;
            }
            case 0x21: {
                regBackgroundColor0 = (byte) (value & 0x0F);
                break;
            }
            case 0x22: {
                regBackgroundColor1 = (byte) (value & 0x0F);
                break;
            }
            case 0x23: {
                regBackgroundColor2 = (byte) (value & 0x0F);
                break;
            }
            case 0x24: {
                regBackgroundColor3 = (byte) (value & 0x0F);
                break;
            }
            case 0x25: {
                var color = (byte) (value & 0x0F);
                regSpritesMulticolor0 = color;
                for (var sprite : sprites) {
                    sprite.multicolor1 = color;
                }
                break;
            }
            case 0x26: {
                var color = (byte) (value & 0x0F);
                regSpritesMulticolor1 = color;
                for (var sprite : sprites) {
                    sprite.multicolor2 = color;
                }
                break;
            }
            case 0x27: {
                sprites[0].color = (byte) (value & 0x0F);
                break;
            }
            case 0x28: {
                sprites[1].color = (byte) (value & 0x0F);
                break;
            }
            case 0x29: {
                sprites[2].color = (byte) (value & 0x0F);
                break;
            }
            case 0x2A: {
                sprites[3].color = (byte) (value & 0x0F);
                break;
            }
            case 0x2B: {
                sprites[4].color = (byte) (value & 0x0F);
                break;
            }
            case 0x2C: {
                sprites[5].color = (byte) (value & 0x0F);
                break;
            }
            case 0x2D: {
                sprites[6].color = (byte) (value & 0x0F);
                break;
            }
            case 0x2E: {
                sprites[7].color = (byte) (value & 0x0F);
                break;
            }
            case 0x2F: {
                regKeyboard = value;
                break;
            }
            case 0x30: {
                regFastMode = value;
                break;
            }
            default: {
                // ignore
                break;
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
        int result;
        switch (address & mask) {
            case 0x00: {
                result = sprites[0].getXLSB();
                break;
            }
            case 0x01: {
                result = sprites[0].y;
                break;
            }
            case 0x02: {
                result = sprites[1].getXLSB();
                break;
            }
            case 0x03: {
                result = sprites[1].y;
                break;
            }
            case 0x04: {
                result = sprites[2].getXLSB();
                break;
            }
            case 0x05: {
                result = sprites[2].y;
                break;
            }
            case 0x06: {
                result = sprites[3].getXLSB();
                break;
            }
            case 0x07: {
                result = sprites[3].y;
                break;
            }
            case 0x08: {
                result = sprites[4].getXLSB();
                break;
            }
            case 0x09: {
                result = sprites[4].y;
                break;
            }
            case 0x0A: {
                result = sprites[5].getXLSB();
                break;
            }
            case 0x0B: {
                result = sprites[5].y;
                break;
            }
            case 0x0C: {
                result = sprites[6].getXLSB();
                break;
            }
            case 0x0D: {
                result = sprites[6].y;
                break;
            }
            case 0x0E: {
                result = sprites[7].getXLSB();
                break;
            }
            case 0x0F: {
                result = sprites[7].y;
                break;
            }
            case 0x10: {
                result = regSpritesMSBX;
                break;
            }
            case 0x11: {
                // TODO 2010-03-17 mh: correct?
                result = regControl1 & 0x7F;
                // bit 7 is high bit of current raster line
                result |= (regRaster & 0x0100) >> 1;
                break;
            }
            case 0x12: {
                result = regRaster & 0xFF;
                break;
            }
            case 0x13: {
                result = regStrobeX;
                break;
            }
            case 0x14: {
                result = regStrobeY;
                break;
            }
            case 0x15: {
                result = regSpritesEnable;
                break;
            }
            case 0x16: {
                result = regControl2;
                break;
            }
            case 0x17: {
                result = regSpritesExpandX;
                break;
            }
            case 0x18: {
                result = regBase;
                break;
            }
            case 0x19: {
                result = regInterruptRequest;
                break;
            }
            case 0x1A: {
                result = regInterruptMask;
                break;
            }
            case 0x1B: {
                result = regSpritesBackgroundPriority;
                break;
            }
            case 0x1C: {
                result = regSpritesMulticolorMode;
                break;
            }
            case 0x1D: {
                result = regSpritesExpandY;
                break;
            }
            case 0x1E: {
                result = regSpritesSpriteCollision;
                break;
            }
            case 0x1F: {
                result = regSpritesBackgroundCollision;
                break;
            }
            case 0x20: {
                result = regExteriorColor;
                break;
            }
            case 0x21: {
                result = regBackgroundColor0;
                break;
            }
            case 0x22: {
                result = regBackgroundColor1;
                break;
            }
            case 0x23: {
                result = regBackgroundColor2;
                break;
            }
            case 0x24: {
                result = regBackgroundColor3;
                break;
            }
            case 0x25: {
                result = regSpritesMulticolor0;
                break;
            }
            case 0x26: {
                result = regSpritesMulticolor1;
                break;
            }
            case 0x27: {
                result = sprites[0].color;
                break;
            }
            case 0x28: {
                result = sprites[1].color;
                break;
            }
            case 0x29: {
                result = sprites[2].color;
                break;
            }
            case 0x2A: {
                result = sprites[3].color;
                break;
            }
            case 0x2B: {
                result = sprites[4].color;
                break;
            }
            case 0x2C: {
                result = sprites[5].color;
                break;
            }
            case 0x2D: {
                result = sprites[6].color;
                break;
            }
            case 0x2E: {
                result = sprites[7].color;
                break;
            }
            case 0x2F: {
                result = regKeyboard;
                break;
            }
            case 0x30: {
                result = regFastMode;
                break;
            }
            default: {
                result = 0xFF;
                break;
            }
        }

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
        if (line == regRasterIRQ && (regInterruptMask & INTERRUPT_RASTER) != 0) {
            regInterruptRequest |= INTERRUPT_RASTER | INTERRUPT_ANY;
            irqPort.setOutputData(0x0);

            logger.debug("Raster irq at line {} at tick {}", regRaster, clock.getTick());
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
