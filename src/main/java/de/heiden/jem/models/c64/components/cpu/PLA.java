package de.heiden.jem.models.c64.components.cpu;

import jakarta.annotation.Nonnull;

import static de.heiden.jem.models.c64.components.cpu.PLA.Input.AEC;
import static de.heiden.jem.models.c64.components.cpu.PLA.Input.CAS;

/**
 * "MMU"-PLA.
 * <p>
 * TODO 2010-10-11 mh: use AbstractMemory as super class?
 */
public class PLA {
    /**
     * All inputs of the pla in the correct order.
     */
    public enum Input {
        CAS, // low active
        LORAM, // low active
        HIRAM, // low active
        CHAREN, // low active
        VA14,
        A15,
        A14,
        A13,
        A12,
        BA, // high active
        AEC, // low active
        RW,
        EXROM, // low active
        GAME, // low active
        VA13,
        VA12
    }

    /**
     * All outputs of the pla in the correct order.
     */
    public enum Output {
        RAM, // low active
        BASIC, // low active
        KERNAL, // low active
        CHAROM, // low active
        COLOR_RW,
        IO, // low active
        ROML, // low active
        ROMH // low active
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
     *         address
     */
    public Output cpu(boolean rw, int addr, boolean loram, boolean hiram, boolean charen, boolean exrom, boolean game) {
        int a = addr >> 12;

        int plaAddress =
                1 << CAS.ordinal() +
                     1 << AEC.ordinal();

        return null;
    }
}
