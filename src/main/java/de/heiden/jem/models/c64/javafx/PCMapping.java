package de.heiden.jem.models.c64.javafx;

import javafx.scene.input.KeyCode;

import static de.heiden.jem.models.c64.components.keyboard.Key.*;

/**
 * Key mapping using the default pc keys.
 */
public class PCMapping extends KeyMapping {
  @Override
  protected final void generateKeyMapping() {
    add(' ', SPACE);

    add('1', NUMBER_1);
    add('2', NUMBER_2);
    add('3', NUMBER_3);
    add('4', NUMBER_4);
    add('5', NUMBER_5);
    add('6', NUMBER_6);
    add('7', NUMBER_7);
    add('8', NUMBER_8);
    add('9', NUMBER_9);
    add('0', NUMBER_0);

    add('!', LEFT_SHIFT, NUMBER_1);
    add('"', LEFT_SHIFT, NUMBER_2);
    add('#', LEFT_SHIFT, NUMBER_3);
    add('$', LEFT_SHIFT, NUMBER_4);
    add('%', LEFT_SHIFT, NUMBER_5);
    add('&', LEFT_SHIFT, NUMBER_6);
    add('\'', LEFT_SHIFT, NUMBER_7);
    add('(', LEFT_SHIFT, NUMBER_8);
    add(')', LEFT_SHIFT, NUMBER_9);
    add('+', PLUS);
    add('-', MINUS);
    // TODO how to map POUND?

    add('a', A);
    add('A', LEFT_SHIFT, A);
    add('b', B);
    add('B', LEFT_SHIFT, B);
    add('c', C);
    add('C', LEFT_SHIFT, C);
    add('d', D);
    add('D', LEFT_SHIFT, D);
    add('e', E);
    add('E', LEFT_SHIFT, E);
    add('f', F);
    add('F', LEFT_SHIFT, F);
    add('g', G);
    add('G', LEFT_SHIFT, G);
    add('h', H);
    add('H', LEFT_SHIFT, H);
    add('i', I);
    add('I', LEFT_SHIFT, I);
    add('j', J);
    add('J', LEFT_SHIFT, J);
    add('k', K);
    add('K', LEFT_SHIFT, K);
    add('l', L);
    add('L', LEFT_SHIFT, L);
    add('m', M);
    add('M', LEFT_SHIFT, M);
    add('n', N);
    add('N', LEFT_SHIFT, N);
    add('o', O);
    add('O', LEFT_SHIFT, O);
    add('p', P);
    add('P', LEFT_SHIFT, P);
    add('q', Q);
    add('Q', LEFT_SHIFT, Q);
    add('r', R);
    add('R', LEFT_SHIFT, R);
    add('s', S);
    add('S', LEFT_SHIFT, S);
    add('t', T);
    add('T', LEFT_SHIFT, T);
    add('u', U);
    add('U', LEFT_SHIFT, U);
    add('v', V);
    add('V', LEFT_SHIFT, V);
    add('w', W);
    add('W', LEFT_SHIFT, W);
    add('x', X);
    add('X', LEFT_SHIFT, X);
    add('y', Y);
    add('Y', LEFT_SHIFT, Y);
    add('z', Z);
    add('Z', LEFT_SHIFT, Z);

    add('.', PERIOD);
    add(':', COLON);
    add('<', LEFT_SHIFT, COMMA);
    add('>', LEFT_SHIFT, PERIOD);
    add(',', COMMA);
    add(';', SEMICOLON);
    add('[', LEFT_SHIFT, COLON);
    add(']', LEFT_SHIFT, SEMICOLON);
    add('=', EQUALS);
    add('/', SLASH);
    add('?', LEFT_SHIFT, SLASH);

    add('@', AT);
    add('*', ASTERISK);
    // TODO how to map UP_ARROW?
    add('^', UP_ARROW);
    add('\n', ENTER);

    add(KeyCode.ENTER, ENTER);
    add(KeyCode.BACK_SPACE, DEL);
    add(KeyCode.INSERT, LEFT_SHIFT, DEL);
    add(KeyCode.HOME, HOME);

    add(KeyCode.UP, RIGHT_SHIFT, DOWN);
    add(KeyCode.DOWN, DOWN);
    add(KeyCode.LEFT, RIGHT_SHIFT, RIGHT);
    add(KeyCode.RIGHT, RIGHT);

    add(KeyCode.F1, F1);
    add(KeyCode.F2, LEFT_SHIFT, F1);
    add(KeyCode.F3, F3);
    add(KeyCode.F4, LEFT_SHIFT, F3);
    add(KeyCode.F5, F5);
    add(KeyCode.F6, LEFT_SHIFT, F5);
    add(KeyCode.F7, F7);
    add(KeyCode.F8, LEFT_SHIFT, F7);

    // Shift is disabled, because the above chars/keys already contain shift
    // add(KeyCode.SHIFT, LEFT_SHIFT);
    // add(KeyCode.SHIFT, RIGHT_SHIFT);
    add(KeyCode.CONTROL, CTRL);
    add(KeyCode.ALT, COMMODORE);

    // run stop -> esc
    add(KeyCode.ESCAPE, RUN_STOP);
    // restore -> delete
    add(KeyCode.DELETE, RESTORE);

    // TODO how to map LEFT_ARROW
  }
}
