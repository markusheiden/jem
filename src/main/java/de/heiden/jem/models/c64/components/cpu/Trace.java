package de.heiden.jem.models.c64.components.cpu;

import de.heiden.c64dt.assembler.CodeBuffer;
import de.heiden.c64dt.assembler.Disassembler;
import de.heiden.c64dt.assembler.ICodeBuffer;
import de.heiden.c64dt.assembler.Opcode;
import de.heiden.jem.components.bus.BusDevice;

import java.io.IOException;
import java.io.StringWriter;

import static de.heiden.c64dt.assembler.Opcode.OPCODES;

/**
 * Data of a single executed opcode for execution tracing.
 * Simple data bean, which can be easily reused.
 */
public class Trace {
  /**
   * Address of opcode.
   */
  public int address;

  /**
   * Byte representation of opcode incl. argument.
   */
  public final byte[] bytes = new byte[3];

  /**
   * Read trace data from bus.
   *
   * @param pc PC, address to read from
   * @param bus Bus
   */
  public final void read(int pc, BusDevice bus) {
    address = pc;

    Opcode opcode = OPCODES[bus.read(pc)];
    for (int i = 0; i < opcode.getSize(); i++) {
      bytes[i] = (byte) bus.read(pc);
      pc = (pc + 1) & 0xFFFF;
    }
  }

  /**
   * Return opcode representation as CodeBuffer.
   */
  public ICodeBuffer toCodeBuffer() {
    return new CodeBuffer(address, bytes);
  }

  @Override
  public String toString() {
    try {
      StringWriter result = new StringWriter(16);
      new Disassembler().disassemble(toCodeBuffer(), result);
      return result.toString();
    } catch (IOException e) {
      return "???";
    }
  }
}
