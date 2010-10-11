package de.heiden.jem.models.c64.components.keyboard;

/**
 * C64 key.
 */
public enum Key
{
  RESTORE(),

  DEL(0, 0),
  ENTER(0, 1),
  RIGHT(0, 2),
  F7(0, 3),
  F1(0, 4),
  F3(0, 5),
  F5(0, 6),
  DOWN(0, 7),

  NUMBER_3(1, 0),
  W(1, 1),
  A(1, 2),
  NUMBER_4(1, 3),
  Z(1, 4),
  S(1, 5),
  E(1, 6),
  LEFT_SHIFT(1, 7),

  NUMBER_5(2, 0),
  R(2, 1),
  D(2, 2),
  NUMBER_6(2, 3),
  C(2, 4),
  F(2, 5),
  T(2, 6),
  X(2, 7),

  NUMBER_7(3, 0),
  Y(3, 1),
  G(3, 2),
  NUMBER_8(3, 3),
  B(3, 4),
  H(3, 5),
  U(3, 6),
  V(3, 7),

  NUMBER_9(4, 0),
  I(4, 1),
  J(4, 2),
  NUMBER_0(4, 3),
  M(4, 4),
  K(4, 5),
  O(4, 6),
  N(4, 7),

  PLUS(5, 0),
  P(5, 1),
  L(5, 2),
  MINUS(5, 3),
  PERIOD(5, 4),
  COLON(5, 5),
  AT(5, 6),
  COMMA(5, 7),

  POUND(6, 0),
  ASTERISK(6, 1),
  SEMICOLON(6, 2),
  HOME(6, 3),
  RIGHT_SHIFT(6, 4),
  EQUALS(6, 5),
  UP_ARROW(6, 6),
  SLASH(6, 7),

  NUMBER_1(7, 0),
  LET_ARROW(7, 1),
  CTRL(7, 2),
  NUMBER_2(7, 3),
  SPACE(7, 4),
  COMMODORE(7, 5),
  Q(7, 6),
  RUN_STOP(7, 7);

  private final int _row;

  private final int _column;

  /**
   * Constructor for special key not mapped on the keyboard matrix.
   */
  private Key()
  {
    _row = -1;
    _column = -1;
  }

  /**
   * Constructor.
   *
   * @param row row of key in matrix
   * @param column column of key in matrix
   * @require row >= 0 && row < 8
   * @require column >= 0 && column < 8
   */
  private Key(int row, int column)
  {
    assert row >= 0 && row < 8 : "Precondition: row >= 0 && row < 8";
    assert column >= 0 && column < 8 : "Precondition: column >= 0 && column < 8";

    _row = row;
    _column = column;
  }

  /**
   * Row of key in matrix.
   *
   * @ensure result >= 0 && result < 8
   */
  public int getRow()
  {
    assert _row >= 0 && _row < 8 : "result >= 0 && result < 8";
    return _row;
  }

  /**
   * Column of key in matrix.
   */
  public int getColumn()
  {
    assert _column >= 0 && _column < 8 : "result >= 0 && result < 8";
    return _column;
  }
}

