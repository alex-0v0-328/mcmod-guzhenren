package net.alex.guzhenren.item.gu.tending;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.item.gu.stack.GuCooldowns;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.UnitAsserts.assertFalse;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 真实 {@link ItemCooldowns} 的炼化后冷却：挡满结算窗口，不缩短更长冷却；需绑定注册表；窗口常量、时间戳算术见
 * {@code TendedGuItemTest}。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TendedGuItemGameTests {

    private TendedGuItemGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void freshlyRefinedGuIsBlockedForTheWholeSettleWindow(GameTestHelper helper) {
        ItemCooldowns cooldowns = new ItemCooldowns();

        GuCooldowns.applyPostRefineCooldown(cooldowns, Items.AIR, TendedGuItem.POST_REFINE_COOLDOWN_TICKS);

        assertTrue(cooldowns.isOnCooldown(Items.AIR));
        for (int tick = 1; tick < Ticks.SECOND; tick++) {
            cooldowns.tick();
            assertTrue(cooldowns.isOnCooldown(Items.AIR), "usable again at tick " + tick);
        }
        cooldowns.tick();
        assertFalse(cooldowns.isOnCooldown(Items.AIR));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void settlingNeverShortensAnExistingLongerCooldown(GameTestHelper helper) {
        ItemCooldowns cooldowns = new ItemCooldowns();
        cooldowns.addCooldown(Items.AIR, 120 * Ticks.SECOND);

        GuCooldowns.applyPostRefineCooldown(cooldowns, Items.AIR, TendedGuItem.POST_REFINE_COOLDOWN_TICKS);

        for (int tick = 0; tick < Ticks.SECOND; tick++) {
            cooldowns.tick();
            assertTrue(cooldowns.isOnCooldown(Items.AIR), "long cooldown was shortened at tick " + tick);
        }
        helper.succeed();
    }
}
