package net.alex.guzhenren.client.input;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DoubleTapTest {

    private static final int WINDOW = 7;

    @Test
    @DisplayName("a single press is never a double tap")
    void singlePressDoesNothing() {
        assertFalse(new DoubleTap(WINDOW).press(100));
    }

    @Test
    @DisplayName("the window is inclusive: the last tick inside counts, the next one starts over")
    void windowIsInclusive() {
        DoubleTap inside = new DoubleTap(WINDOW);
        inside.press(100);
        assertTrue(inside.press(100 + WINDOW));

        DoubleTap outside = new DoubleTap(WINDOW);
        outside.press(100);
        assertFalse(outside.press(100 + WINDOW + 1));
    }

    @Test
    @DisplayName("a late press becomes the first of a new pair")
    void latePressStartsNewPair() {
        DoubleTap tap = new DoubleTap(WINDOW);
        tap.press(100);
        assertFalse(tap.press(200));
        assertTrue(tap.press(203));
    }

    @Test
    @DisplayName("a third quick press starts a new pair instead of toggling again")
    void thirdPressStartsNewPair() {
        DoubleTap tap = new DoubleTap(WINDOW);
        tap.press(100);
        assertTrue(tap.press(102));
        assertFalse(tap.press(104));
        assertTrue(tap.press(105));
    }

    @Test
    @DisplayName("the very first press counts even at tick zero or below")
    void firstPressAtAnyTick() {
        DoubleTap tap = new DoubleTap(WINDOW);
        assertFalse(tap.press(0));
        assertTrue(tap.press(1));
        DoubleTap negative = new DoubleTap(WINDOW);
        assertFalse(negative.press(-5));
        assertTrue(negative.press(-1));
    }
}
