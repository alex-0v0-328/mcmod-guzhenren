package net.alex.guzhenren;

import net.alex.guzhenren.effect.timed.WatchGuEffect;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WatchGuEffectGameTests {

    private WatchGuEffectGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void secondWatchCapsAtFourfoldTime(GameTestHelper helper) {
        WatchGuEffect effect = effect(2, 2);

        assertEquals(0, effect.getNextAmplifier(-1));
        assertEquals(1, effect.getNextAmplifier(0));
        assertEquals(1, effect.getNextAmplifier(1));
        assertEquals(2, effect.timeRate(0));
        assertEquals(4, effect.timeRate(1));
        assertEquals(4, effect.timeRate(99));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void thirdWatchCapsAtNinefoldTime(GameTestHelper helper) {
        WatchGuEffect effect = effect(3, 3);

        assertEquals(0, effect.getNextAmplifier(-1));
        assertEquals(1, effect.getNextAmplifier(0));
        assertEquals(2, effect.getNextAmplifier(1));
        assertEquals(2, effect.getNextAmplifier(2));
        assertEquals(3, effect.timeRate(0));
        assertEquals(6, effect.timeRate(1));
        assertEquals(9, effect.timeRate(2));
        assertEquals(9, effect.timeRate(99));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void cappedWatchGuEffectsAddToThirteenfoldTime(GameTestHelper helper) {
        assertEquals(13, effect(2, 2).timeRate(1) + effect(3, 3).timeRate(2));
        helper.succeed();
    }

    private static WatchGuEffect effect(int ratePerLayer, int maxLayers) {
        return new WatchGuEffect(MobEffectCategory.BENEFICIAL, 0xFFFFFF, ratePerLayer, maxLayers);
    }
}
