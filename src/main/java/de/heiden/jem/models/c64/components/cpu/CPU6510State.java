package de.heiden.jem.models.c64.components.cpu;

import de.heiden.c64dt.util.HexUtil;

/**
 * State of CPU.
 */
public final class CPU6510State {
  /**
   * Stack base address.
   */
  protected static final int STACK = 0x0100;

  public static final int N_BIT = 1 << 7;
  public static final int V_BIT = 1 << 6;
  public static final int U_BIT = 1 << 5; // bit 5 is unused: always 1
  public static final int B_BIT = 1 << 4;
  public static final int D_BIT = 1 << 3;
  public static final int I_BIT = 1 << 2;
  public static final int Z_BIT = 1 << 1;
  public static final int C_BIT = 1 << 0;

  public int PC; // program counter

  /**
   * Stack pointer.
   * For stack pointer address see {@link #getS()}.
   */
  public int S;

  public boolean C; // status P: carry
  public boolean Z; // status P: zero
  /**
   * Interrupt inhibit flag.
   * Read only access from outside.
   * Use {@link #sei()} or {@link #cli()} to modify.
   */
  public boolean I; // status P: interrupt
  public boolean D; // status P: decimal
  public boolean B; // status P: break
  public boolean V; // status P: overflow
  public boolean N; // status P: negative

  public int A; // accumulator

  public int X; // index x

  public int Y; // index y

  /**
   * Interrupt.
   * Interrupt request with I flag cleared.
   */
  public boolean interrupt;

  /**
   * Interrupt Request.
   * Read only access from outside.
   * Use {@link #triggerIRQ()} or {@link #resetIRQ()} to modify.
   */
  public boolean IRQ;

  /**
   * Non maskable interrupt.
   */
  public boolean NMI;

  /**
   * Constructor.
   */
  public CPU6510State() {
    this(0, 0x00FF, 0, 0, 0, 0, false, false);
  }

  /**
   * Copy constructor.
   */
  CPU6510State(int PC, int S, int P, int A, int X, int Y, boolean IRQ, boolean NMI) {
    assert PC >= 0 && PC < 0x10000 : "Precondition: PC >= 0 && PC < 0x10000";
    assert S >= 0x00 && S <= 0xFF : "Precondition: S >= 0x00 && S <= 0xFF";
    assert P >= 0 && P < 0x100 : "Precondition: P >= 0 && P < 0x100";
    assert A >= 0 && A < 0x100 : "Precondition: A >= 0 && A < 0x100";
    assert X >= 0 && X < 0x100 : "Precondition: X >= 0 && X < 0x100";
    assert Y >= 0 && Y < 0x100 : "Precondition: Y >= 0 && Y < 0x100";

    this.PC = PC;
    this.S = S;
    this.A = A;
    this.X = X;
    this.Y = Y;
    interrupt = false;
    this.IRQ = IRQ;
    this.NMI = NMI;

    setP(P);
  }

  public void sei() {
    I = true;
    interrupt = false;
  }

  public void cli() {
    I = false;
    interrupt = IRQ;
  }

  public void triggerIRQ() {
    IRQ = true;
    interrupt = !I;
  }

  /**
   * Reset interrupt request.
   */
  public final void resetIRQ() {
    IRQ = false;
    interrupt = false;
  }

  /**
   * Get and increment PC.
   */
  public final int incPC() {
    int pc = PC;
    PC = (pc + 1) & 0xFFFF;
    return pc;
  }

  /**
   * Stack pointer address.
   */
  public final int getS() {
    return STACK | S;
  }

  /**
   * Decrement stack pointer.
   *
   * @return Stack pointer address before decrement
   */
  public final int decS() {
    int s = S;
    S = ((s - 1) & 0xFF);
    return STACK | s;
  }

  /**
   * Increment stack pointer.
   *
   * @return Incremented stack pointer address
   */
  public final int incS() {
    int s = ((S + 1) & 0xFF);
    S = s;
    return STACK | s;
  }

  /**
   * Set P.
   *
   * @param p new value for p
   */
  public final void setP(int p) {
    assert p >= 0 && p < 0x100 : "Precondition: p >= 0 && p < 0x100";

    C = (p & C_BIT) != 0;
    Z = (p & Z_BIT) != 0;
    I = (p & I_BIT) != 0;
    D = (p & D_BIT) != 0;
    B = true;
    V = (p & V_BIT) != 0;
    N = (p & N_BIT) != 0;
  }

  /**
   * Calculates status register P from flags.
   */
  public final int getP() {
    int p = U_BIT;
    if (C) {
      p |= C_BIT;
    }
    if (Z) {
      p |= Z_BIT;
    }
    if (I) {
      interrupt = false;
      p |= I_BIT;
    }
    if (D) {
      p |= D_BIT;
    }
    if (B) {
      p |= B_BIT;
    }
    if (V) {
      p |= V_BIT;
    }
    if (N) {
      p |= N_BIT;
    }

    return p;
  }

  //
  // status register related functionality
  //

  /**
   * Set N and Z of P for value.
   * Used for LDA etc.
   */
  public final void setZeroNegativeP(int value) {
    assert value >= 0 && value <= 0x100 : "Precondition: value >= 0 && value <= 0x100";

    Z = value == 0;
    N = (value & 0x80) != 0;
  }

  /**
   * Set Z, V and N of P for value.
   * Used for BIT.
   */
  public final void setZeroOverflowNegativeP(int value, boolean z) {
    assert value >= 0 && value <= 0x100 : "Precondition: value >= 0 && value <= 0x100";

    Z = z;
    V = (value & 0x40) != 0;
    N = (value & 0x80) != 0;
  }

  /**
   * Set N, Z and C of P for value.
   * Used for ROL etc.
   */
  public final void setCarryZeroNegativeP(int value, boolean c) {
    C = c;
    Z = (value & 0xFF) == 0;
    N = (value & 0x80) != 0;
  }

  /**
   * Set N, V, Z and C of P for value.
   * Used for ADC and SBC.
   *
   * @param s1 Summand 1
   * @param s2 Summand 2
   * @param sum Sum
   */
  public final void setCarryZeroOverflowNegativeP(int s1, int s2, int sum) {
    C = (sum & 0x100) != 0;
    Z = (sum & 0xFF) == 0;
    V = ((s1 ^ sum) & (s2 ^ sum) & 0x80) != 0;
    N = (sum & 0x80) != 0;
  }

  //
  //
  //

  /**
   * Copy cpu state.
   */
  public CPU6510State copy() {
    return new CPU6510State(PC, S, getP(), A, X, Y, IRQ, NMI);
  }

  /**
   * toString.
   */
  public String toString() {
    return
      "PC=" + HexUtil.hexWord(PC) +
        ", S=" + HexUtil.hexWord(getS()) +
        ", A=" + HexUtil.hexByte(A) +
        ", X=" + HexUtil.hexByte(X) +
        ", Y=" + HexUtil.hexByte(Y) +
        ", P=" + HexUtil.hexByte(getP()) +
        ", IRQ=" + IRQ +
        ", NMI=" + NMI;
  }

  /**
   * equals.
   */
  public boolean equals(Object o) {
    if (!(o instanceof CPU6510State)) {
      return false;
    }

    CPU6510State state = (CPU6510State) o;
    return PC == state.PC && S == state.S && getP() == state.getP() && A == state.A && X == state.X && Y == state.Y && IRQ == state.IRQ && NMI == state.NMI;
  }

  /**
   * hashCode
   */
  public int hashCode() {
    return PC;
  }
}
