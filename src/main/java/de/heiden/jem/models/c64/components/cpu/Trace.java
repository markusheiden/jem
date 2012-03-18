package de.heiden.jem.models.c64.components.cpu;

import de.heiden.c64dt.assembler.Opcode;
import de.heiden.jem.components.bus.BusDevice;
import de.heiden.jem.models.c64.monitor.Monitor;

import static de.heiden.c64dt.assembler.Opcode.OPCODES;

/**
 * Data for a single exceuted opcode for execution tracing.
 * Simple data bean, which can be easily reused
 */
public class Trace {
  public int address;
  public int[] bytes = new int[3];
  public Opcode opcode = Opcode.OPCODE_02;
  public int argument;

  public void read(int pc, BusDevice bus) {
    address = pc;

    opcode = OPCODES[bus.read(pc++)];
    bytes[0] = opcode.getOpcode();

    argument = 0;
    for (int i = 0; i < opcode.getMode().getSize(); i++) {
      int b = bus.read(pc++);
      bytes[i + 1] = b;
      argument += b << (i * 8);
    }
  }

  public String toString() {
    return Monitor.disassemble(address, bytes, opcode, argument);
  }
}
