package net.alex.guzhenren.item.gu;

import net.alex.guzhenren.client.input.DashInput;
import net.alex.guzhenren.network.ClientIntents;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashInputTest {

    @Test
    void altPressStartsDashWhileDirectionIsHeld() {
        assertTrue(DashInput.shouldStartDash(true, false, false));
    }

    @Test
    void directionPressStartsDashWhileAltIsHeld() {
        assertTrue(DashInput.shouldStartDash(true, true, true));
    }

    @Test
    void heldKeysDoNotRepeatDash() {
        assertFalse(DashInput.shouldStartDash(true, true, false));
        assertFalse(DashInput.shouldStartDash(false, false, true));
    }

    @Test
    void dashDirectionNeedsMatchingCrashEffect() {
        assertTrue(ClientIntents.canDash(0, 1, true, false, false));
        assertTrue(ClientIntents.canDash(1, 0, false, true, false));
        assertFalse(ClientIntents.canDash(1, 0, true, false, false));
        assertFalse(ClientIntents.canDash(1, 1, true, false, false));
        assertTrue(ClientIntents.canDash(1, 1, false, false, true));
        assertFalse(ClientIntents.canDash(0, 0, true, true, true));
    }
}
