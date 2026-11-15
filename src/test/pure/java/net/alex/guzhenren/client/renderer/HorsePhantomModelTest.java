package net.alex.guzhenren.client.renderer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HorsePhantomModelTest {

    private static final float DELTA = 0.001F;

    @Test
    @DisplayName("the horse phantom rears up over 4 ticks, holds to 12, comes down by 18 and then stands")
    void theRearRisesHoldsAndComesDown() {
        assertEquals(0.0F, HorsePhantomModel.getRear(0.0F), DELTA);
        assertEquals(0.5F, HorsePhantomModel.getRear(2.0F), DELTA);
        assertEquals(1.0F, HorsePhantomModel.getRear(HorsePhantomModel.REAR_RISE_TICKS), DELTA);
        assertEquals(1.0F, HorsePhantomModel.getRear(HorsePhantomModel.REAR_HOLD_TICKS), DELTA);
        assertEquals(0.5F, HorsePhantomModel.getRear(15.0F), DELTA);
        assertEquals(0.0F, HorsePhantomModel.getRear(HorsePhantomModel.REAR_END_TICKS), DELTA);
        assertEquals(0.0F, HorsePhantomModel.getRear(40.0F), DELTA, "the horse reared again");
    }
}
