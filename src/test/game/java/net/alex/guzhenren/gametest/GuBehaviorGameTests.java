package net.alex.guzhenren.gametest;

import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.mortal.behavior.RecastBehavior;
import net.alex.guzhenren.item.gu.mortal.behavior.TimedBuffBehavior;
import net.alex.guzhenren.item.gu.mortal.behavior.TimedEffects;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.DeferredHolder;
import static net.alex.guzhenren.gametest.GameTestGu.awakened;
import static net.alex.guzhenren.gametest.GameTestGu.click;
import static net.alex.guzhenren.gametest.GameTestGu.hold;
import static net.alex.guzhenren.gametest.GameTestGu.hunger;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertFalse;
import static net.alex.guzhenren.gametest.UnitAsserts.assertNotNull;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 靠注册而非类接线的蛊行为：增益蛊都带再催，效果冷却内再催照样生效、饱食[Hunger]翻倍。漏一行注册时其余测试仍全绿，故在
 * 此钉住；自动蛊归 {@link SelfRelianceGameTests}、{@link HardshipStrengthGameTests}。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GuBehaviorGameTests {
    private GuBehaviorGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void everyBuffGuCarriesARecast(GameTestHelper helper) {
        List<MortalGuItem> buffs = ModItems.ITEMS.getEntries().stream()
                .map(DeferredHolder::get)
                .filter(item -> item instanceof MortalGuItem gu && gu.getBehavior(TimedBuffBehavior.class) != null)
                .map(MortalGuItem.class::cast)
                .toList();

        assertFalse(buffs.isEmpty(), "no buff Gu is registered");
        for (MortalGuItem gu : buffs) {
            assertNotNull(gu.getBehavior(RecastBehavior.class), () -> gu + " cannot be recast");
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aRecastInsideTheEffectCooldownPaysDoubleHunger(GameTestHelper helper) {
        ServerPlayer player = awakened(GameTestPlayers.survival(helper, "gzr-gu-behavior-gametest").onGround(), Rank.ONE);
        TendedGuItem gu = (TendedGuItem) ModItems.FLOWER_BOAR_GU_1.get();
        ItemStack stack = hold(player, gu, 14);
        long essence = ApertureEssenceService.getSpendable(player);

        click(helper, player, stack);
        assertEquals(14 - 3, hunger(stack));
        assertTrue(gu.getEffectCooldownLeft(player, stack) > 0, "the use left no effect cooldown");
        player.getCooldowns().removeCooldown(gu);

        click(helper, player, stack);
        assertEquals(essence - 80L, ApertureEssenceService.getSpendable(player), "the recast was refused");
        assertEquals(14 - 3 - 2 * 3, hunger(stack), "the recast did not pay double hunger");
        assertTrue(TimedEffects.hasBuff(player, ModEffects.FLOWER_BOAR_GU), "the recast lost the buff");
        helper.succeed();
    }
}
