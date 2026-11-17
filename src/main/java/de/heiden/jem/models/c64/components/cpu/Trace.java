package de.heiden.jem.models.c64.components.cpu;

import de.heiden.c64dt.assembler.CodeBuffer;
import de.heiden.c64dt.assembler.Disassembler;
import de.heiden.c64dt.assembler.ICodeBuffer;
import de.heiden.jem.components.bus.BusDevice;

import java.io.IOException;
import java.io.StringWriter;

import static de.heiden.c64dt.assembler.Opcode.OPCODES;
import static java.lang.System.arraycopy;

/**
 * Data of a single executed opcode for execution tracing.
 * Simple data bean, which can be easily reused.
 */
public final class Trace {
    /**
     * Address of opcode.
     */
    public int address;

    /**
     * Size of opcode incl. argument.
     */
    public int size = 0;

    /**
     * Byte representation of opcode incl. argument.
     */
    public final byte[] bytes = new byte[3];

    /**
     * I/O area of the C64. Opcodes in it are traced without their bytes.
     */
    private static final int IO = 0xD000;
    private static final int IO_END = 0xE000;

    /**
     * Read trace data from the bus.
     *
     * @param pc
     *         PC, address to read from.
     * @param bus
     *         Bus.
     */
    public void read(int pc, BusDevice bus) {
        address = pc;

        if (pc >= IO - 2 && pc < IO_END) {
            // Do not read the I/O area: Reading a register may have side effects,
            // e.g. reading the interrupt control register of a CIA clears its interrupt flags.
            size = 0;
            return;
        }

        var opcode = bus.read(pc);
        pc = (pc + 1) & 0xFFFF;
        bytes[0] = (byte) opcode;
        size = OPCODES[opcode].getSize();
        for (var i = 1; i < size; i++) {
            bytes[i] = (byte) bus.read(pc);
            pc = (pc + 1) & 0xFFFF;
        }
    }

    /**
     * Return opcode representation as CodeBuffer.
     */
    public ICodeBuffer toCodeBuffer() {
        var truncated = bytes;
        if (truncated.length > size) {
            truncated = new byte[size];
            arraycopy(bytes, 0, truncated, 0, size);
        }
        return new CodeBuffer(address, truncated);
    }

    /**
     * Disassembler for {@link #toString()}.
     */
    private static final Disassembler disassembler = new Disassembler();

    @Override
    public String toString() {
        try {
            var result = new StringWriter(40);
            disassembler.disassemble(toCodeBuffer(), result);
            return result.toString();
        } catch (IOException e) {
            return "???";
        }
    }
}
