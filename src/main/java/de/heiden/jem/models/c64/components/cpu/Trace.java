package de.heiden.jem.models.c64.components.cpu;

import de.heiden.c64dt.assembler.Opcode;
import de.heiden.jem.models.c64.monitor.Monitor;

/**
 * Data for a single exceuted opcode for execution tracing.
 * Simple data bean, which can be easily reused
 */
public class Trace
{
  public int address;
  public Opcode opcode;
  public int argument;

  public String toString()
  {
    int[] bytes = {opcode.getOpcode(), argument & 0xFF, argument >> 8};
    return Monitor.disassemble(address, bytes, opcode, argument);
  }
}
