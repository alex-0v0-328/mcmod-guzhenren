package net.alex.guzhenren.registry.effect;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.UnitAsserts.assertAll;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertFalse;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ModEffectsGameTests {

    private ModEffectsGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void guEffectsUseWhiteParticles(GameTestHelper helper) {
        assertAll(
                () -> assertEquals(0xFFFFFF, ModEffects.VITALITY_LEAF_GU.get().getColor()),
                () -> assertEquals(0xFFFFFF, ModEffects.LIQUOR_WORM.get().getColor()),
                () -> assertEquals(0xFFFFFF, ModEffects.FLOWER_BOAR_GU.get().getColor()),
                () -> assertEquals(0xFFFFFF, ModEffects.ALL_OUT_EFFORT_GU.get().getColor()),
                () -> assertEquals(0xFFFFFF, ModEffects.BRUTE_FORCE_LONGHORN_BEETLE_GU.get().getColor()),
                () -> assertEquals(0xFFFFFF, ModEffects.HORIZONTAL_CRASH_GU.get().getColor()),
                () -> assertEquals(0xFFFFFF, ModEffects.VERTICAL_CRASH_GU.get().getColor()),
                () -> assertEquals(0xFFFFFF, ModEffects.CHARGING_CRASH_GU.get().getColor()),
                () -> assertEquals(0xFFFFFF, ModEffects.SELF_RELIANCE_GU.get().getColor()),
                () -> assertEquals(0xFFFFFF, ModEffects.HARDSHIP_STRENGTH_GU.get().getColor()),
                () -> assertEquals(0xFFFFFF, ModEffects.JADE_SKIN_GU.get().getColor()),
                () -> assertEquals(0xFFFFFF, ModEffects.WHITE_JADE_GU.get().getColor()),
                () -> assertEquals(0xFFFFFF, ModEffects.SECOND_WATCH_GU.get().getColor()),
                () -> assertEquals(0xFFFFFF, ModEffects.THIRD_WATCH_GU.get().getColor()),
                () -> assertEquals(0xFFFFFF, ModEffects.MALICIOUS_THOUGHT_GU.get().getColor()),
                () -> assertEquals(0xFFFFFF, ModEffects.CASUAL_GU.get().getColor()));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void temporaryZombieUsesWhiteParticles(GameTestHelper helper) {
        assertEquals(0xFFFFFF, ModEffects.TEMPORARY_ZOMBIE.get().getColor());
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void singleArgBuildsParticlesOffIconOn(GameTestHelper helper) {
        MobEffectInstance i = ModEffects.instance(ModEffects.TEMPORARY_ZOMBIE, 100);
        assertFalse(i.isAmbient());
        assertFalse(i.isVisible());
        assertTrue(i.showIcon());
        assertEquals(0, i.getAmplifier());
        assertEquals(100, i.getDuration());
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void amplifiedBuildKeepsParticlesOffIconOn(GameTestHelper helper) {
        MobEffectInstance i = ModEffects.instance(ModEffects.TEMPORARY_ZOMBIE, 40, 2);
        assertFalse(i.isVisible());
        assertTrue(i.showIcon());
        assertEquals(2, i.getAmplifier());
        assertEquals(40, i.getDuration());
        helper.succeed();
    }
}
