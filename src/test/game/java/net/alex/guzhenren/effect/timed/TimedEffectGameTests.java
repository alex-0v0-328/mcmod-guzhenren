package net.alex.guzhenren.effect.timed;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;

/**
 * 需构造 {@code MobEffect}（要求效果注册表已绑定）的限时蛊效果：恶念蛊[Malicious Thought Gu]脉冲次数；常量见
 * {@code MaliciousThoughtGuEffectTest}。
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TimedEffectGameTests {

    private static final long[] EVIL_PER_SECOND = { 2L, 20L, 200L, 2_000L };

    private TimedEffectGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void maliciousThoughtPulsesExactlyTwelveTimesInTwelveSeconds(GameTestHelper helper) {
        MaliciousThoughtGuEffect effect = new MaliciousThoughtGuEffect(
                MobEffectCategory.BENEFICIAL, 0, EVIL_PER_SECOND);
        int count = 0;
        for (int duration = MaliciousThoughtGuEffect.DURATION_TICKS; duration > 0; duration--) {
            if (effect.isPulseTick(duration)) count++;
        }
        assertEquals(12, count);
        helper.succeed();
    }
}
