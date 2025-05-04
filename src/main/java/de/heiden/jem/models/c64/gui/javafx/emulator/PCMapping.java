package de.heiden.jem.models.c64.gui.javafx.emulator;

import javafx.scene.input.KeyCode;

import static de.heiden.jem.models.c64.components.keyboard.Key.A;
import static de.heiden.jem.models.c64.components.keyboard.Key.ASTERISK;
import static de.heiden.jem.models.c64.components.keyboard.Key.AT;
import static de.heiden.jem.models.c64.components.keyboard.Key.B;
import static de.heiden.jem.models.c64.components.keyboard.Key.C;
import static de.heiden.jem.models.c64.components.keyboard.Key.COLON;
import static de.heiden.jem.models.c64.components.keyboard.Key.COMMA;
import static de.heiden.jem.models.c64.components.keyboard.Key.COMMODORE;
import static de.heiden.jem.models.c64.components.keyboard.Key.CTRL;
import static de.heiden.jem.models.c64.components.keyboard.Key.D;
import static de.heiden.jem.models.c64.components.keyboard.Key.DEL;
import static de.heiden.jem.models.c64.components.keyboard.Key.DOWN;
import static de.heiden.jem.models.c64.components.keyboard.Key.E;
import static de.heiden.jem.models.c64.components.keyboard.Key.ENTER;
import static de.heiden.jem.models.c64.components.keyboard.Key.EQUALS;
import static de.heiden.jem.models.c64.components.keyboard.Key.F;
import static de.heiden.jem.models.c64.components.keyboard.Key.F1;
import static de.heiden.jem.models.c64.components.keyboard.Key.F3;
import static de.heiden.jem.models.c64.components.keyboard.Key.F5;
import static de.heiden.jem.models.c64.components.keyboard.Key.F7;
import static de.heiden.jem.models.c64.components.keyboard.Key.G;
import static de.heiden.jem.models.c64.components.keyboard.Key.H;
import static de.heiden.jem.models.c64.components.keyboard.Key.HOME;
import static de.heiden.jem.models.c64.components.keyboard.Key.I;
import static de.heiden.jem.models.c64.components.keyboard.Key.J;
import static de.heiden.jem.models.c64.components.keyboard.Key.K;
import static de.heiden.jem.models.c64.components.keyboard.Key.L;
import static de.heiden.jem.models.c64.components.keyboard.Key.LEFT_SHIFT;
import static de.heiden.jem.models.c64.components.keyboard.Key.M;
import static de.heiden.jem.models.c64.components.keyboard.Key.MINUS;
import static de.heiden.jem.models.c64.components.keyboard.Key.N;
import static de.heiden.jem.models.c64.components.keyboard.Key.NUMBER_0;
import static de.heiden.jem.models.c64.components.keyboard.Key.NUMBER_1;
import static de.heiden.jem.models.c64.components.keyboard.Key.NUMBER_2;
import static de.heiden.jem.models.c64.components.keyboard.Key.NUMBER_3;
import static de.heiden.jem.models.c64.components.keyboard.Key.NUMBER_4;
import static de.heiden.jem.models.c64.components.keyboard.Key.NUMBER_5;
import static de.heiden.jem.models.c64.components.keyboard.Key.NUMBER_6;
import static de.heiden.jem.models.c64.components.keyboard.Key.NUMBER_7;
import static de.heiden.jem.models.c64.components.keyboard.Key.NUMBER_8;
import static de.heiden.jem.models.c64.components.keyboard.Key.NUMBER_9;
import static de.heiden.jem.models.c64.components.keyboard.Key.O;
import static de.heiden.jem.models.c64.components.keyboard.Key.P;
import static de.heiden.jem.models.c64.components.keyboard.Key.PERIOD;
import static de.heiden.jem.models.c64.components.keyboard.Key.PLUS;
import static de.heiden.jem.models.c64.components.keyboard.Key.Q;
import static de.heiden.jem.models.c64.components.keyboard.Key.R;
import static de.heiden.jem.models.c64.components.keyboard.Key.RESTORE;
import static de.heiden.jem.models.c64.components.keyboard.Key.RIGHT;
import static de.heiden.jem.models.c64.components.keyboard.Key.RIGHT_SHIFT;
import static de.heiden.jem.models.c64.components.keyboard.Key.RUN_STOP;
import static de.heiden.jem.models.c64.components.keyboard.Key.S;
import static de.heiden.jem.models.c64.components.keyboard.Key.SEMICOLON;
import static de.heiden.jem.models.c64.components.keyboard.Key.SLASH;
import static de.heiden.jem.models.c64.components.keyboard.Key.SPACE;
import static de.heiden.jem.models.c64.components.keyboard.Key.T;
import static de.heiden.jem.models.c64.components.keyboard.Key.U;
import static de.heiden.jem.models.c64.components.keyboard.Key.UP_ARROW;
import static de.heiden.jem.models.c64.components.keyboard.Key.V;
import static de.heiden.jem.models.c64.components.keyboard.Key.W;
import static de.heiden.jem.models.c64.components.keyboard.Key.X;
import static de.heiden.jem.models.c64.components.keyboard.Key.Y;
import static de.heiden.jem.models.c64.components.keyboard.Key.Z;

/**
 * Key mapping using the default PC keys.
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

        addCode(KeyCode.ENTER, ENTER);
        addCode(KeyCode.BACK_SPACE, DEL);
        addCode(KeyCode.INSERT, LEFT_SHIFT, DEL);
        addCode(KeyCode.HOME, HOME);

        addCode(KeyCode.UP, RIGHT_SHIFT, DOWN);
        addCode(KeyCode.DOWN, DOWN);
        addCode(KeyCode.LEFT, RIGHT_SHIFT, RIGHT);
        addCode(KeyCode.RIGHT, RIGHT);

        addCode(KeyCode.F1, F1);
        addCode(KeyCode.F2, LEFT_SHIFT, F1);
        addCode(KeyCode.F3, F3);
        addCode(KeyCode.F4, LEFT_SHIFT, F3);
        addCode(KeyCode.F5, F5);
        addCode(KeyCode.F6, LEFT_SHIFT, F5);
        addCode(KeyCode.F7, F7);
        addCode(KeyCode.F8, LEFT_SHIFT, F7);

        // Shift is disabled, because the above chars/keys already contain shift
        // addCode(KeyCode.SHIFT, LEFT_SHIFT);
        // addCode(KeyCode.SHIFT, RIGHT_SHIFT);
        addCode(KeyCode.CONTROL, CTRL);
        addCode(KeyCode.ALT, COMMODORE);

        // run stop -> esc
        addCode(KeyCode.ESCAPE, RUN_STOP);
        // restore -> delete
        addCode(KeyCode.DELETE, RESTORE);

        // TODO how to map LEFT_ARROW
    }
}
