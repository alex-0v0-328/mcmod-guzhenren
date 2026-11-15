package net.alex.guzhenren.block;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.UnitAsserts.assertFalse;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 元泉[Spirit Spring]产出判定钉子：128 格玩家门槛、附近元石[Primeval Stone]上限。加载方块类会带出 {@code LiquidBlock}
 * 静态游戏事件注册，需注册表绑定，故用 GameTest。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SpiritSpringBlockGameTests {

    private SpiritSpringBlockGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void noPlayerNoProduction(GameTestHelper helper) {
        assertFalse(SpiritSpringBlock.shouldProduce(false, 0));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void playerNearProduces(GameTestHelper helper) {
        assertTrue(SpiritSpringBlock.shouldProduce(true, 0));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void capPausesProduction(GameTestHelper helper) {
        assertFalse(SpiritSpringBlock.shouldProduce(true, SpiritSpringBlock.NEARBY_STONES_CAP));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void belowCapProduces(GameTestHelper helper) {
        assertTrue(SpiritSpringBlock.shouldProduce(true, SpiritSpringBlock.NEARBY_STONES_CAP - 1));
        helper.succeed();
    }
}
