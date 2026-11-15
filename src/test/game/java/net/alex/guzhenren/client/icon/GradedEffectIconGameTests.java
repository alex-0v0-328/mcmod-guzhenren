package net.alex.guzhenren.client.icon;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GradedEffectIconGameTests {

    private GradedEffectIconGameTests() {}

    private static String texture(GradedEffectIcon icon, int amplifier) {
        return icon.getTextureFor(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, amplifier));
    }

    @GameTest(template = "empty9x9x9")
    public static void defaultOffsetMapsAmplifierPlusOne(GameTestHelper helper) {
        GradedEffectIcon icon = GradedEffectIcon.createMobEffect("liquor_worm", 1, 4);
        assertEquals("mob_effect/liquor_worm_1", texture(icon, 0));
        assertEquals("mob_effect/liquor_worm_3", texture(icon, 2));
        assertEquals("mob_effect/liquor_worm_4", texture(icon, 3));
        assertEquals("mob_effect/liquor_worm_4", texture(icon, 9));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void maliciousThoughtCountsAmplifierFromRankTwo(GameTestHelper helper) {
        GradedEffectIcon icon = GradedEffectIcon.createItem("malicious_thought_gu", 2, 5, 2);
        assertEquals("item/malicious_thought_gu_2", texture(icon, 0));
        assertEquals("item/malicious_thought_gu_3", texture(icon, 1));
        assertEquals("item/malicious_thought_gu_4", texture(icon, 2));
        assertEquals("item/malicious_thought_gu_5", texture(icon, 3));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void rangeClampsBothEnds(GameTestHelper helper) {
        GradedEffectIcon icon = GradedEffectIcon.createItem("self_reliance_gu", 2, 4);
        assertEquals("item/self_reliance_gu_2", texture(icon, 0));
        assertEquals("item/self_reliance_gu_2", texture(icon, 1));
        assertEquals("item/self_reliance_gu_3", texture(icon, 2));
        assertEquals("item/self_reliance_gu_4", texture(icon, 3));
        assertEquals("item/self_reliance_gu_4", texture(icon, 10));
        helper.succeed();
    }
}
