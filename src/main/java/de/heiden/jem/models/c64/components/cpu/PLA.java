package de.heiden.jem.models.c64.components.cpu;

import jakarta.annotation.Nonnull;

import static de.heiden.jem.models.c64.components.cpu.PLA.Input.AEC;
import static de.heiden.jem.models.c64.components.cpu.PLA.Input.CAS;

/**
 * "MMU"-PLA.
 * <p>
 * TODO 2010-10-11 mh: Use AbstractMemory as super class?
 */
public class PLA {
    /**
     * All inputs of the PLA in the correct order.
     */
    public enum Input {
        CAS, // Low active.
        LORAM, // Low active.
        HIRAM, // Low active.
        CHAREN, // Low active.
        VA14,
        A15,
        A14,
        A13,
        A12,
        BA, // High active.
        AEC, // Low active.
        RW,
        EXROM, // Low active.
        GAME, // Low active.
        VA13,
        VA12
    }

    /**
     * All outputs of the PLA in the correct order.
     */
    public enum Output {
        RAM, // Low active.
        BASIC, // Low active.
        KERNAL, // Low active.
        CHAROM, // Low active.
        COLOR_RW,
        IO, // Low active.
        ROML, // Low active.
        ROMH // Low active.
    }

    private final byte[] content;

    /**
     * Constructor.
     */
    public PLA(@Nonnull byte[] content) {
        assert content.length == 0x10000 : "Precondition: content.length == 0x10000 ";

        this.content = content;
    }

    /**
     * Return the component (output) which is visible to the CPU.
     *
     * @param addr
     *         Address.
     */
    public Output cpu(boolean rw, int addr, boolean loram, boolean hiram, boolean charen, boolean exrom, boolean game) {
        var a = addr >> 12;

        var plaAddress =
                1 << CAS.ordinal() +
                     1 << AEC.ordinal();

        return null;
    }
}
