package net.alex.guzhenren.client.input;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlungeInputTest {

    @Test
    @DisplayName("an Alt press while sneak is held is the chord")
    void altPressWhileSneaking() {
        assertTrue(PlungeInput.isChord(true, false, true, true));
    }

    @Test
    @DisplayName("a sneak press while Alt is held is the chord")
    void sneakPressWhileAltIsHeld() {
        assertTrue(PlungeInput.isChord(false, true, true, true));
    }

    @Test
    @DisplayName("either key alone, or both only held, is no chord")
    void aLoneKeyOrHeldKeysDoNothing() {
        assertFalse(PlungeInput.isChord(true, false, true, false));
        assertFalse(PlungeInput.isChord(false, true, false, true));
        assertFalse(PlungeInput.isChord(false, false, true, true));
    }
}
