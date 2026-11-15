package net.alex.guzhenren.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorageService;
import net.alex.guzhenren.gameplay.path.PathEffect;
import net.alex.guzhenren.gameplay.path.PathEffectService;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.gameplay.path.strength.SelfRelianceHeal;
import net.alex.guzhenren.item.GuItem;
import net.alex.guzhenren.item.gu.mortal.path.strength.AutoUse;
import net.alex.guzhenren.item.gu.mortal.path.strength.SelfRelianceBehavior;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.alex.guzhenren.registry.effect.ModPathEffectTypes;
import net.alex.guzhenren.registry.item.ModDataComponents;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.GameTestGu.hold;
import static net.alex.guzhenren.gametest.GameTestGu.hunger;
import static net.alex.guzhenren.gametest.GameTestGu.inSlot;
import static net.alex.guzhenren.gametest.GameTestGu.refined;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertFalse;
import static net.alex.guzhenren.gametest.UnitAsserts.assertNotNull;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 在线玩家自力更生蛊[Self Reliance Gu]自动回复测试。
 * <ul>
 *   <li>生命低于上限 75% 自行启动，只付一次催动代价；背包与空窍[Aperture]各一只算一份，存储的那只照样扣费并写回；
 *       两种转数只高转计入并付费。</li>
 *   <li>回复脉冲向 75% 递减，末跳停在 75%，窗口继续（按转 15/20/25 秒，青牛兽力再加 10 秒）。</li>
 *   <li>失去最后一只或死亡即停，放回后重启再付费；效果冷却隔开两次启动；回复被取消时暂停；手动催动被拒且不扣费。</li>
 * </ul>
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SelfRelianceGameTests {
    private SelfRelianceGameTests() {}

    private static final int BAG_SLOT = 9;
    private static final double HEALTH_DELTA = 1.0E-3D;
    private static final int USE_HUNGER = 3;
    private static final long RANK_TWO_ESSENCE = 320L;
    private static final long RANK_FOUR_ESSENCE = 20_480L;

    @GameTest(template = "empty9x9x9")
    public static void itStartsOnlyUnderThreeQuartersAndPaysOnce(GameTestHelper helper) {
        ServerPlayer player = awakened(helper);
        ItemStack gu = inSlot(player, BAG_SLOT, ModItems.SELF_RELIANCE_GU_2.get(), 26);
        long essence = ApertureEssenceService.getSpendable(player);

        player.setHealth(player.getMaxHealth() * 0.8F);
        AutoUse.tryStart(player);
        assertTrue(heal(player) == null, "it started above 75%");

        player.setHealth(player.getMaxHealth() * 0.7F);
        AutoUse.tryStart(player);
        AutoUse.tryStart(player);
        assertTrue(heal(player) != null, "it did not start under 75%");
        assertTrue(player.hasEffect(ModEffects.SELF_RELIANCE_GU), "the heal shows no icon");
        assertEquals(essence - RANK_TWO_ESSENCE, ApertureEssenceService.getSpendable(player), "not billed once");
        assertEquals(26 - USE_HUNGER, hunger(gu), "not one use of hunger");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aCopyInTheBagAndOneInAnApertureAreOneShare(GameTestHelper helper) {
        ServerPlayer player = awakened(helper);
        ItemStack carried = inSlot(player, BAG_SLOT, ModItems.SELF_RELIANCE_GU_2.get(), 26);
        store(player, refined(ModItems.SELF_RELIANCE_GU_2.get(), 26));
        long essence = ApertureEssenceService.getSpendable(player);
        player.setHealth(player.getMaxHealth() * 0.5F);

        AutoUse.tryStart(player);

        assertTrue(heal(player) != null, "nothing started");
        assertEquals(essence - RANK_TWO_ESSENCE, ApertureEssenceService.getSpendable(player), "billed twice");
        assertEquals(26 - USE_HUNGER, hunger(carried), "the bag copy, first in scan order, did not pay");
        assertEquals(26, hunger(stored(player)), "the stored copy paid as well");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aStoredCopyStartsAndIsWrittenBack(GameTestHelper helper) {
        ServerPlayer player = awakened(helper);
        store(player, refined(ModItems.SELF_RELIANCE_GU_2.get(), 26));
        long essence = ApertureEssenceService.getSpendable(player);
        player.setHealth(player.getMaxHealth() * 0.5F);

        AutoUse.tryStart(player);

        assertTrue(heal(player) != null, "a stored copy did not start");
        assertEquals(essence - RANK_TWO_ESSENCE, ApertureEssenceService.getSpendable(player));
        assertEquals(26 - USE_HUNGER, hunger(stored(player)), "the stored copy's bill was not written back");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void ofTwoRanksOnlyTheHigherStartsAndPays(GameTestHelper helper) {
        ServerPlayer player = awakened(helper);
        ItemStack second = inSlot(player, BAG_SLOT, ModItems.SELF_RELIANCE_GU_2.get(), 26);
        ItemStack fourth = inSlot(player, BAG_SLOT + 1, ModItems.SELF_RELIANCE_GU_4.get(), 98);
        long essence = ApertureEssenceService.getSpendable(player);
        player.setHealth(player.getMaxHealth() * 0.5F);

        AutoUse.tryStart(player);

        PathEffect<SelfRelianceHeal> heal = assertNotNull(heal(player), () -> "nothing started");
        assertEquals(source(ModItems.SELF_RELIANCE_GU_4.get()), heal.source(), "the lower rank started");
        assertEquals(3, heal.payload().grade());
        assertEquals(essence - RANK_FOUR_ESSENCE, ApertureEssenceService.getSpendable(player), "not billed at rank IV");
        assertEquals(26, hunger(second), "the lower rank paid");
        assertEquals(98 - USE_HUNGER, hunger(fourth));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void thePulseTapersAndTheLastOneStopsAtThreeQuarters(GameTestHelper helper) {
        ServerPlayer player = awakened(helper);
        inSlot(player, BAG_SLOT, ModItems.SELF_RELIANCE_GU_2.get(), 26);
        float max = player.getMaxHealth();
        player.setHealth(max * 0.25F);
        AutoUse.tryStart(player);

        pulse(player, 1);
        assertEquals(max * 0.28F, player.getHealth(), HEALTH_DELTA);
        player.setHealth(max * 0.575F);
        pulse(player, 2);
        assertEquals(max * 0.59375F, player.getHealth(), HEALTH_DELTA);

        pulseUntilHealed(player);
        assertEquals(max * 0.75F, player.getHealth(), HEALTH_DELTA);
        pulse(player, 401);
        assertEquals(max * 0.75F, player.getHealth(), HEALTH_DELTA);
        assertTrue(heal(player) != null, "the window ended with the heal");
        assertTrue(player.hasEffect(ModEffects.SELF_RELIANCE_GU), "the icon left before the window ran out");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theWindowIsFifteenSecondsByRankAndTheGreenBullAddsTen(GameTestHelper helper) {
        ServerPlayer player = awakened(helper);
        inSlot(player, BAG_SLOT, ModItems.SELF_RELIANCE_GU_2.get(), 26);
        float max = player.getMaxHealth();
        player.setHealth(max * 0.5F);
        long now = player.level().getGameTime();
        AutoUse.tryStart(player);
        PathEffect<SelfRelianceHeal> heal = assertNotNull(heal(player), () -> "nothing started");
        assertEquals(now + 15L * Ticks.SECOND, heal.endTick(), "the rank-two window is not fifteen seconds");

        player.setHealth(max * 0.9F);
        AutoUse.tick(player);
        assertTrue(heal(player) != null, "healing past 75% ended the window");

        PathEffectService.removeType(player, ModPathEffectTypes.SELF_RELIANCE);
        player.getInventory().setItem(BAG_SLOT, ItemStack.EMPTY);
        ItemStack fourth = inSlot(player, BAG_SLOT, ModItems.SELF_RELIANCE_GU_4.get(), 98);
        clearCooldown(player, fourth);
        assertTrue(PathStrengthService.grant(player, BeastStrength.GREEN_BULL), "the bull was refused");
        player.setHealth(max * 0.5F);
        now = player.level().getGameTime();
        AutoUse.tryStart(player);
        heal = assertNotNull(heal(player), () -> "the rank-four copy did not start");
        assertEquals(now + 25L * Ticks.SECOND + 10L * Ticks.SECOND, heal.endTick(),
                "the rank-four window with the bull is not thirty-five seconds");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void losingTheLastCopyStopsItAndPuttingItBackPaysAgain(GameTestHelper helper) {
        ServerPlayer player = awakened(helper);
        ItemStack gu = inSlot(player, BAG_SLOT, ModItems.SELF_RELIANCE_GU_2.get(), 26);
        long essence = ApertureEssenceService.getSpendable(player);
        float max = player.getMaxHealth();
        player.setHealth(max * 0.5F);
        AutoUse.tryStart(player);

        player.getInventory().setItem(BAG_SLOT, ItemStack.EMPTY);
        AutoUse.tick(player);
        assertTrue(heal(player) == null, "the heal outlived its last copy");
        assertEquals(max * 0.5F, player.getHealth(), HEALTH_DELTA);

        player.getInventory().setItem(BAG_SLOT, gu);
        clearCooldown(player, gu);
        AutoUse.tryStart(player);
        assertTrue(heal(player) != null, "putting it back did not start it");
        assertEquals(essence - 2 * RANK_TWO_ESSENCE, ApertureEssenceService.getSpendable(player), "the restart was free");
        assertEquals(26 - 2 * USE_HUNGER, hunger(gu));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theEffectCooldownSpacesTwoStarts(GameTestHelper helper) {
        ServerPlayer player = awakened(helper);
        ItemStack gu = inSlot(player, BAG_SLOT, ModItems.SELF_RELIANCE_GU_2.get(), 26);
        long essence = ApertureEssenceService.getSpendable(player);
        float max = player.getMaxHealth();
        player.setHealth(max * 0.7F);
        AutoUse.tryStart(player);

        PathEffectService.removeType(player, ModPathEffectTypes.SELF_RELIANCE);
        assertTrue(heal(player) == null, "the window did not end");

        player.setHealth(max * 0.5F);
        player.getCooldowns().removeCooldown(gu.getItem());
        AutoUse.tryStart(player);
        assertTrue(heal(player) == null, "it started again inside its effect cooldown");
        assertEquals(essence - RANK_TWO_ESSENCE, ApertureEssenceService.getSpendable(player));
        assertEquals(26 - USE_HUNGER, hunger(gu));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aCancelledHealPausesTheHealWithoutEndingIt(GameTestHelper helper) {
        ServerPlayer player = awakened(helper);
        inSlot(player, BAG_SLOT, ModItems.SELF_RELIANCE_GU_2.get(), 26);
        float max = player.getMaxHealth();
        player.setHealth(max * 0.25F);
        AutoUse.tryStart(player);

        Consumer<LivingHealEvent> block = event -> {
            if (event.getEntity() == player) event.setCanceled(true);
        };
        NeoForge.EVENT_BUS.addListener(block);
        try {
            pulse(player, 1);
            pulse(player, 2);
        } finally {
            NeoForge.EVENT_BUS.unregister(block);
        }
        assertEquals(max * 0.25F, player.getHealth(), HEALTH_DELTA);
        assertTrue(heal(player) != null, "a cancelled heal ended the round");

        pulse(player, 3);
        assertEquals(max * 0.28F, player.getHealth(), HEALTH_DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void dyingStopsItAtOnce(GameTestHelper helper) {
        ServerPlayer player = awakened(helper);
        inSlot(player, BAG_SLOT, ModItems.SELF_RELIANCE_GU_2.get(), 26);
        player.setHealth(player.getMaxHealth() * 0.5F);
        AutoUse.tryStart(player);

        player.setHealth(0.0F);
        pulse(player, 1);

        boolean ended = heal(player) == null;
        float health = player.getHealth();
        player.setHealth(player.getMaxHealth());
        assertTrue(ended, "the heal outlived its holder");
        assertEquals(0.0F, health, HEALTH_DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aHandUseIsRefusedAndPaysNothing(GameTestHelper helper) {
        ServerPlayer player = awakened(helper);
        ItemStack gu = hold(player, ModItems.SELF_RELIANCE_GU_2.get(), 26);
        long essence = ApertureEssenceService.getSpendable(player);
        player.setHealth(player.getMaxHealth() * 0.5F);

        GameTestGu.click(helper, player, gu);

        assertTrue(heal(player) == null, "a hand use started the heal");
        assertEquals(essence, ApertureEssenceService.getSpendable(player), "a refused hand use paid essence");
        assertEquals(26, hunger(gu), "a refused hand use paid hunger");
        TendedGuItem item = (TendedGuItem) gu.getItem();
        GuItem.Refusal refusal = assertNotNull(
                item.getBehavior(SelfRelianceBehavior.class).checkEffect(item, player, gu), () -> "no refusal");
        assertEquals("guzhenren.item.failed.auto_effect", refusal.key());
        for (ItemStack rank : List.of(new ItemStack(ModItems.SELF_RELIANCE_GU_2.get()),
                new ItemStack(ModItems.SELF_RELIANCE_GU_3.get()), new ItemStack(ModItems.SELF_RELIANCE_GU_4.get()))) {
            assertFalse(((TendedGuItem) rank.getItem()).canBeAmplified(), rank + " can still be amplified");
        }
        helper.succeed();
    }

    private static void pulseUntilHealed(ServerPlayer player) {
        for (int pulse = 10; pulse <= 400; pulse++) {
            pulse(player, pulse);
            if (!AutoUse.isHurt(player)) return;
        }
        throw new AssertionError("the heal never reached 75%");
    }

    private static void pulse(ServerPlayer player, int pulse) {
        player.tickCount = pulse * SelfRelianceBehavior.PULSE_TICKS;
        AutoUse.tick(player);
    }

    private static PathEffect<SelfRelianceHeal> heal(ServerPlayer player) {
        return PathEffectService.find(player, ModPathEffectTypes.SELF_RELIANCE);
    }

    private static void store(ServerPlayer player, ItemStack stack) {
        ApertureStorageService.set(player, 0, new ArrayList<>(List.of(stack)));
        assertEquals(1, ApertureStorageService.count(player, 0), "the aperture refused the copy");
    }

    private static ItemStack stored(ServerPlayer player) {
        return ApertureStorageService.getView(player, 0).getFirst();
    }

    private static ResourceLocation source(Item item) {
        return ((TendedGuItem) item).getEffectSource();
    }

    private static void clearCooldown(ServerPlayer player, ItemStack gu) {
        gu.remove(ModDataComponents.USED_AT);
        player.getCooldowns().removeCooldown(gu.getItem());
    }

    private static ServerPlayer awakened(GameTestHelper helper) {
        return GameTestGu.awakened(
                GameTestPlayers.survival(helper, "gzr-self-reliance-gametest").onGround(), Rank.FIVE);
    }
}
