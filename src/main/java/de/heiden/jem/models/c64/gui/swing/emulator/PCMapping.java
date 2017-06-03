package de.heiden.jem.models.c64.gui.swing.emulator;

import java.awt.event.KeyEvent;

import static de.heiden.jem.models.c64.components.keyboard.Key.*;

/**
 * Key mapping using the default pc keys.
 */
public class PCMapping extends KeyMapping {
  @Override
  protected final void generateKeyMapping() {
    addChar(' ', SPACE);

    addChar('1', NUMBER_1);
    addChar('2', NUMBER_2);
    addChar('3', NUMBER_3);
    addChar('4', NUMBER_4);
    addChar('5', NUMBER_5);
    addChar('6', NUMBER_6);
    addChar('7', NUMBER_7);
    addChar('8', NUMBER_8);
    addChar('9', NUMBER_9);
    addChar('0', NUMBER_0);

    addChar('!', LEFT_SHIFT, NUMBER_1);
    addChar('"', LEFT_SHIFT, NUMBER_2);
    addChar('#', LEFT_SHIFT, NUMBER_3);
    addChar('$', LEFT_SHIFT, NUMBER_4);
    addChar('%', LEFT_SHIFT, NUMBER_5);
    addChar('&', LEFT_SHIFT, NUMBER_6);
    addChar('\'', LEFT_SHIFT, NUMBER_7);
    addChar('(', LEFT_SHIFT, NUMBER_8);
    addChar(')', LEFT_SHIFT, NUMBER_9);
    addChar('+', PLUS);
    addChar('-', MINUS);
    // TODO how to map POUND?

    addChar('a', A);
    addChar('A', LEFT_SHIFT, A);
    addChar('b', B);
    addChar('B', LEFT_SHIFT, B);
    addChar('c', C);
    addChar('C', LEFT_SHIFT, C);
    addChar('d', D);
    addChar('D', LEFT_SHIFT, D);
    addChar('e', E);
    addChar('E', LEFT_SHIFT, E);
    addChar('f', F);
    addChar('F', LEFT_SHIFT, F);
    addChar('g', G);
    addChar('G', LEFT_SHIFT, G);
    addChar('h', H);
    addChar('H', LEFT_SHIFT, H);
    addChar('i', I);
    addChar('I', LEFT_SHIFT, I);
    addChar('j', J);
    addChar('J', LEFT_SHIFT, J);
    addChar('k', K);
    addChar('K', LEFT_SHIFT, K);
    addChar('l', L);
    addChar('L', LEFT_SHIFT, L);
    addChar('m', M);
    addChar('M', LEFT_SHIFT, M);
    addChar('n', N);
    addChar('N', LEFT_SHIFT, N);
    addChar('o', O);
    addChar('O', LEFT_SHIFT, O);
    addChar('p', P);
    addChar('P', LEFT_SHIFT, P);
    addChar('q', Q);
    addChar('Q', LEFT_SHIFT, Q);
    addChar('r', R);
    addChar('R', LEFT_SHIFT, R);
    addChar('s', S);
    addChar('S', LEFT_SHIFT, S);
    addChar('t', T);
    addChar('T', LEFT_SHIFT, T);
    addChar('u', U);
    addChar('U', LEFT_SHIFT, U);
    addChar('v', V);
    addChar('V', LEFT_SHIFT, V);
    addChar('w', W);
    addChar('W', LEFT_SHIFT, W);
    addChar('x', X);
    addChar('X', LEFT_SHIFT, X);
    addChar('y', Y);
    addChar('Y', LEFT_SHIFT, Y);
    addChar('z', Z);
    addChar('Z', LEFT_SHIFT, Z);

    addChar('.', PERIOD);
    addChar(':', COLON);
    addChar('<', LEFT_SHIFT, COMMA);
    addChar('>', LEFT_SHIFT, PERIOD);
    addChar(',', COMMA);
    addChar(';', SEMICOLON);
    addChar('[', LEFT_SHIFT, COLON);
    addChar(']', LEFT_SHIFT, SEMICOLON);
    addChar('=', EQUALS);
    addChar('/', SLASH);
    addChar('?', LEFT_SHIFT, SLASH);
    addChar('_', COMMODORE, P);

    addChar('@', AT);
    addChar('*', ASTERISK);
    // TODO how to map UP_ARROW?
    addChar('^', UP_ARROW);
    addChar('\n', ENTER);

    addCode(KeyEvent.KEY_LOCATION_STANDARD, KeyEvent.VK_ENTER, ENTER);
    addCode(KeyEvent.KEY_LOCATION_STANDARD, KeyEvent.VK_BACK_SPACE, DEL);
    addCode(KeyEvent.KEY_LOCATION_STANDARD, KeyEvent.VK_INSERT, LEFT_SHIFT, DEL);
    addCode(KeyEvent.KEY_LOCATION_STANDARD, KeyEvent.VK_HOME, HOME);

    addCode(KeyEvent.KEY_LOCATION_STANDARD, KeyEvent.VK_UP, RIGHT_SHIFT, DOWN);
    addCode(KeyEvent.KEY_LOCATION_STANDARD, KeyEvent.VK_DOWN, DOWN);
    addCode(KeyEvent.KEY_LOCATION_STANDARD, KeyEvent.VK_LEFT, RIGHT_SHIFT, RIGHT);
    addCode(KeyEvent.KEY_LOCATION_STANDARD, KeyEvent.VK_RIGHT, RIGHT);

    addCode(KeyEvent.KEY_LOCATION_STANDARD, KeyEvent.VK_F1, F1);
    addCode(KeyEvent.KEY_LOCATION_STANDARD, KeyEvent.VK_F2, LEFT_SHIFT, F1);
    addCode(KeyEvent.KEY_LOCATION_STANDARD, KeyEvent.VK_F3, F3);
    addCode(KeyEvent.KEY_LOCATION_STANDARD, KeyEvent.VK_F4, LEFT_SHIFT, F3);
    addCode(KeyEvent.KEY_LOCATION_STANDARD, KeyEvent.VK_F5, F5);
    addCode(KeyEvent.KEY_LOCATION_STANDARD, KeyEvent.VK_F6, LEFT_SHIFT, F5);
    addCode(KeyEvent.KEY_LOCATION_STANDARD, KeyEvent.VK_F7, F7);
    addCode(KeyEvent.KEY_LOCATION_STANDARD, KeyEvent.VK_F8, LEFT_SHIFT, F7);

    // Shift is disabled, because the above chars/keys already contain shift
    // addCode(KeyEvent.KEY_LOCATION_LEFT, KeyEvent.VK_SHIFT, LEFT_SHIFT);
    // addCode(KeyEvent.KEY_LOCATION_RIGHT, KeyEvent.VK_SHIFT, RIGHT_SHIFT);
    addCode(KeyEvent.KEY_LOCATION_STANDARD, KeyEvent.VK_CONTROL, CTRL);
    addCode(KeyEvent.KEY_LOCATION_STANDARD, KeyEvent.VK_ALT, COMMODORE);

    // run stop -> esc
    addCode(KeyEvent.KEY_LOCATION_STANDARD, KeyEvent.VK_ESCAPE, RUN_STOP);
    // restore -> delete
    addCode(KeyEvent.KEY_LOCATION_STANDARD, KeyEvent.VK_DELETE, RESTORE);

    // TODO how to map LEFT_ARROW
  }
}
