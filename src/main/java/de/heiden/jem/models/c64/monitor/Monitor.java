package de.heiden.jem.models.c64.monitor;

import de.heiden.c64dt.assembler.Opcode;
import de.heiden.c64dt.util.HexUtil;
import de.heiden.jem.components.bus.BusDevice;
import de.heiden.jem.models.c64.components.cpu.CPU6510State;

/**
 * Monitor.
 */
public class Monitor {
  /**
   * CPU state as string.
   *
   * @param state cpu state
   */
  public static String state(CPU6510State state) {
    StringBuilder result = new StringBuilder(64);
    result.append(".  PC=");
    result.append(HexUtil.hexWord(state.PC));
    result.append(" S=");
    result.append(HexUtil.hexWord(state.getS()));
    result.append(" A=");
    result.append(HexUtil.hexByte(state.A));
    result.append(" X=");
    result.append(HexUtil.hexByte(state.X));
    result.append(" Y=");
    result.append(HexUtil.hexByte(state.Y));
    result.append(" P=");
    result.append(state.N ? "N" : "n");
    result.append(state.V ? "V" : "v");
    result.append("1");
    result.append(state.B ? "B" : "b");
    result.append(state.D ? "D" : "d");
    result.append(state.I ? "I" : "i");
    result.append(state.Z ? "Z" : "z");
    result.append(state.C ? "C" : "c");
    if (state.NMI) {
      result.append(" NMI");
    }
    if (state.IRQ) {
      result.append(" IRQ");
    }

    return result.toString();
  }

  /**
   * Disassemble one line of code.
   *
   * @param addr address to disassemble
   * @param bus bus to read opcode and arg from
   */
  public static String disassemble(int addr, BusDevice bus) {
    // read opcode with arg
    Opcode opcode = Opcode.values()[bus.read(addr)];
    int[] bytes = new int[]{opcode.getOpcode(), 0, 0};
    for (int i = 1; i <= opcode.getMode().getSize(); i++) {
      bytes[i] = bus.read((addr + i) & 0xFFFF);
    }
    int arg = bytes[1] + (bytes[2] << 8);

    return disassemble(addr, bytes, opcode, arg);
  }

  /**
   * Disassemble one line of code.
   */
  public static String disassemble(int addr, int[] bytes, Opcode opcode, int arg) {
    StringBuilder result = new StringBuilder(32);
    result.append(".> ");

    // mem dump
    result.append(HexUtil.hexWordPlain(addr));
    result.append(' ');
    for (int i = 0; i < opcode.getSize(); i++) {
      result.append(HexUtil.hexBytePlain(bytes[i]));
      result.append(' ');
    }
    for (int i = opcode.getSize(); i < 3; i++) {
      result.append("   ");
    }

    // disassemble
    result.append(opcode.toString(addr, arg));

    return result.toString();
  }

  /**
   * Mem dump.
   *
   * @param from start address
   * @param to end address
   * @param bus bus
   */
  public static String dump(int from, int to, BusDevice bus) {
    StringBuilder result = new StringBuilder();

    for (int i = from; i < to; ) {
      result.append("M ");
      result.append(HexUtil.hexWordPlain(i));
      for (int j = 0; j < 16 && i < to; i++, j++) {
        result.append(" ");
        result.append(HexUtil.hexBytePlain(bus.read(i)));
      }
      result.append("\n");
    }

    return result.toString();
  }
}
