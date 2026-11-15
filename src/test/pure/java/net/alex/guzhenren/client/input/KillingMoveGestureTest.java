package net.alex.guzhenren.client.input;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.item.gu.killingmove.KillingMoveService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class KillingMoveGestureTest {

    private static final String MOVE = "km_mortal_little_light_amplify";

    @Test
    @DisplayName("a tap sends the plain blade")
    void tapSendsPlain() {
        List<String> sent = new ArrayList<>();
        new KillingMoveGesture(sent::add).releaseUnopened();
        assertEquals(List.of(KillingMoveService.PLAIN), sent);
    }

    @Test
    @DisplayName("a hold with nothing to list sends nothing on release")
    void emptyHoldSendsNothing() {
        List<String> sent = new ArrayList<>();
        KillingMoveGesture gesture = new KillingMoveGesture(sent::add);
        gesture.list(List.of());
        gesture.empty();
        gesture.releaseUnopened();
        assertEquals(List.of(), sent);
    }

    @Test
    @DisplayName("a pick sends the move listed at that sector")
    void pickSendsListedMove() {
        List<String> sent = new ArrayList<>();
        KillingMoveGesture gesture = new KillingMoveGesture(sent::add);
        gesture.list(List.of("other", MOVE));
        gesture.pick(1);
        assertEquals(List.of(MOVE), sent);
    }
}
