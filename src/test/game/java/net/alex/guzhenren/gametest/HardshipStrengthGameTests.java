package net.alex.guzhenren.gametest;

import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.path.PathEffect;
import net.alex.guzhenren.gameplay.path.PathEffectService;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.HardshipStrength;
import net.alex.guzhenren.gameplay.path.strength.HumanStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.gameplay.path.strength.StrengthAmplifier;
import net.alex.guzhenren.gameplay.path.strength.StrengthService;
import net.alex.guzhenren.item.GuItem;
import net.alex.guzhenren.item.gu.mortal.path.strength.AutoUse;
import net.alex.guzhenren.item.gu.mortal.path.strength.HardshipBehavior;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.alex.guzhenren.registry.effect.ModPathEffectTypes;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.GameTestGu.hold;
import static net.alex.guzhenren.gametest.GameTestGu.hunger;
import static net.alex.guzhenren.gametest.GameTestGu.inSlot;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertFalse;
import static net.alex.guzhenren.gametest.UnitAsserts.assertNotNull;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 活玩家苦力蛊[Hardship Strength Gu]的自动状态。
 * <ul>
 *   <li>身体持永久力道分支时生命低于 75% 自启并付一次费，无则不启；窗口 30 秒（青牛劳力加 10 秒），回血不止；
 *       {@code isActive} 随之开关；手动催动被拒、不计费。</li>
 *   <li>各档位边界结算读其承受加成，人力与兽力[Beast Strength]虚影身体占正常承受份额相同，不给力量点
 *       [Strength Points]。</li>
 *   <li>人力身体持两层钧（60 点），兽力身体持熊（40 点），均五转。</li>
 * </ul>
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HardshipStrengthGameTests {
    private HardshipStrengthGameTests() {}

    private static final int BAG_SLOT = 9;
    private static final int HUNGER = 98;
    private static final int USE_HUNGER = 3;
    private static final long ESSENCE = 20_480L;
    private static final float[] BOUNDS = { 0.749F, 0.60F, 0.45F, 0.30F, 0.15F, 0.149F };
    private static final int[] CAPACITY_PERCENT = { 5, 10, 25, 50, 100, 100 };

    @GameTest(template = "empty9x9x9")
    public static void itStartsUnderThreeQuartersPaysOnceAndOutlivesTheHeal(GameTestHelper helper) {
        ServerPlayer player = human(helper);
        ItemStack gu = inSlot(player, BAG_SLOT, ModItems.HARDSHIP_STRENGTH_GU_4.get(), HUNGER);
        long essence = ApertureEssenceService.getSpendable(player);
        float max = player.getMaxHealth();

        player.setHealth(max * 0.8F);
        AutoUse.tryStart(player);
        assertFalse(HardshipStrength.isActive(player), "it started above 75%");

        player.setHealth(max * 0.7F);
        long now = player.level().getGameTime();
        AutoUse.tryStart(player);
        AutoUse.tryStart(player);
        assertTrue(HardshipStrength.isActive(player), "it did not start under 75%");
        assertTrue(player.hasEffect(ModEffects.HARDSHIP_STRENGTH_GU), "the state shows no icon");
        assertEquals(essence - ESSENCE, ApertureEssenceService.getSpendable(player), "not billed once");
        assertEquals(HUNGER - USE_HUNGER, hunger(gu), "not one use of hunger");
        PathEffect<?> state = assertNotNull(PathEffectService.find(player, ModPathEffectTypes.HARDSHIP_STRENGTH),
                () -> "no state");
        assertEquals(now + 30L * Ticks.SECOND, state.endTick(), "the window is not thirty seconds");

        player.setHealth(max * 0.75F);
        AutoUse.tick(player);
        assertTrue(HardshipStrength.isActive(player), "healing ended the window");
        assertTrue(player.hasEffect(ModEffects.HARDSHIP_STRENGTH_GU), "the icon left before the window ran out");
        assertTrue(amplifier(player) == null, "a healed body still gained capacity");
        assertEquals(essence - ESSENCE, ApertureEssenceService.getSpendable(player), "the healed window billed again");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theGreenBullAddsTenSecondsToTheWindow(GameTestHelper helper) {
        ServerPlayer player = awakened(helper);
        assertTrue(PathStrengthService.grant(player, BeastStrength.GREEN_BULL), "the bull was refused");
        inSlot(player, BAG_SLOT, ModItems.HARDSHIP_STRENGTH_GU_4.get(), HUNGER);
        player.setHealth(player.getMaxHealth() * 0.5F);
        long now = player.level().getGameTime();
        AutoUse.tryStart(player);
        PathEffect<?> state = assertNotNull(PathEffectService.find(player, ModPathEffectTypes.HARDSHIP_STRENGTH),
                () -> "the bull's body did not start");
        assertEquals(now + 40L * Ticks.SECOND, state.endTick(), "the window with the bull is not forty seconds");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void losingTheLastCopyStopsIt(GameTestHelper helper) {
        ServerPlayer player = human(helper);
        inSlot(player, BAG_SLOT, ModItems.HARDSHIP_STRENGTH_GU_4.get(), HUNGER);
        player.setHealth(player.getMaxHealth() * 0.5F);
        AutoUse.tryStart(player);
        assertTrue(HardshipStrength.isActive(player), "it did not start");

        player.getInventory().setItem(BAG_SLOT, ItemStack.EMPTY);
        AutoUse.tick(player);

        assertFalse(HardshipStrength.isActive(player), "the state outlived its last copy");
        assertTrue(amplifier(player) == null, "the amplifier outlived the state");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void oneJinIsABranchEnoughToStart(GameTestHelper helper) {
        ServerPlayer player = awakened(helper);
        inSlot(player, BAG_SLOT, ModItems.HARDSHIP_STRENGTH_GU_4.get(), HUNGER);
        PathStrengthService.setHumanStrength(player, HumanStrength.JIN, 1);
        player.setHealth(player.getMaxHealth() * 0.2F);

        AutoUse.tryStart(player);
        assertTrue(HardshipStrength.isActive(player), "one jin holds the human branch, yet it did not start");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aRunningStateIsRecheckedWithoutBillingAgain(GameTestHelper helper) {
        ServerPlayer player = human(helper);
        ItemStack gu = inSlot(player, BAG_SLOT, ModItems.HARDSHIP_STRENGTH_GU_4.get(), HUNGER);
        player.setHealth(player.getMaxHealth() * 0.5F);
        AutoUse.tryStart(player);
        assertTrue(HardshipStrength.isActive(player), "it did not start");
        long essence = ApertureEssenceService.getSpendable(player);
        int hunger = hunger(gu);

        player.removeEffect(ModEffects.HARDSHIP_STRENGTH_GU);
        AutoUse.tryStart(player);
        AutoUse.tick(player);

        assertTrue(HardshipStrength.isActive(player), "the recheck stopped a state whose conditions still hold");
        assertEquals(essence, ApertureEssenceService.getSpendable(player), "the recheck billed essence again");
        assertEquals(hunger, hunger(gu), "the recheck billed hunger again");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void withoutPermanentPointsItNeverStartsOrPays(GameTestHelper helper) {
        ServerPlayer player = awakened(helper);
        ItemStack gu = inSlot(player, BAG_SLOT, ModItems.HARDSHIP_STRENGTH_GU_4.get(), HUNGER);
        long essence = ApertureEssenceService.getSpendable(player);
        player.setHealth(player.getMaxHealth() * 0.2F);

        AutoUse.tryStart(player);

        assertFalse(HardshipStrength.isActive(player), "it started with no branch");
        assertEquals(essence, ApertureEssenceService.getSpendable(player), "it paid essence");
        assertEquals(HUNGER, hunger(gu), "it paid hunger");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aHumanBodyGainsAShareOfItsNormalCapacityByTier(GameTestHelper helper) {
        ServerPlayer player = human(helper);
        start(player);
        int normal = StrengthService.getNormalCapacity(player);
        int permanent = StrengthService.getPermanentPoints(player);
        assertEquals(60, permanent, "the body does not hold 60 points of jun");

        for (int bound = 0; bound < BOUNDS.length; bound++) {
            player.setHealth(player.getMaxHealth() * BOUNDS[bound]);
            String at = "at " + BOUNDS[bound];
            StrengthAmplifier amplifier = assertNotNull(amplifier(player), () -> "no amplifier " + at);
            int bonus = normal * CAPACITY_PERCENT[bound] / 100;
            assertTrue(amplifier.family() == null, "the amplifier has a family " + at);
            assertEquals(0, amplifier.points(), "it lent points " + at);
            assertEquals(bonus, amplifier.capacityBonus(), at);
            assertEquals(permanent, StrengthService.getStrengthPoints(player), at);
            assertEquals(normal + bonus, StrengthService.getCapacity(player), at);
            assertEquals(Math.min(normal + bonus, permanent), StrengthService.getUsablePoints(player), at);
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aBeastPhantomBodyGainsTheSameShares(GameTestHelper helper) {
        ServerPlayer player = awakened(helper);
        assertTrue(PathStrengthService.grant(player, BeastStrength.BEAR), "the bear was refused");
        start(player);
        int normal = StrengthService.getNormalCapacity(player);

        for (int bound = 0; bound < BOUNDS.length; bound++) {
            player.setHealth(player.getMaxHealth() * BOUNDS[bound]);
            String at = "at " + BOUNDS[bound];
            StrengthAmplifier amplifier = assertNotNull(amplifier(player), () -> "no amplifier " + at);
            int bonus = normal * CAPACITY_PERCENT[bound] / 100;
            assertEquals(0, amplifier.points(), "it lent points " + at);
            assertEquals(bonus, amplifier.capacityBonus(), at);
            assertEquals(40, StrengthService.getStrengthPoints(player), at);
            assertEquals(normal + bonus, StrengthService.getCapacity(player), at);
            assertEquals(40, StrengthService.getUsablePoints(player), "an admitted bear is already carried " + at);
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aHandUseIsRefusedAndPaysNothing(GameTestHelper helper) {
        ServerPlayer player = human(helper);
        ItemStack gu = hold(player, ModItems.HARDSHIP_STRENGTH_GU_4.get(), HUNGER);
        long essence = ApertureEssenceService.getSpendable(player);
        player.setHealth(player.getMaxHealth() * 0.5F);

        GameTestGu.click(helper, player, gu);

        assertFalse(HardshipStrength.isActive(player), "a hand use started the state");
        assertEquals(essence, ApertureEssenceService.getSpendable(player), "a refused hand use paid essence");
        assertEquals(HUNGER, hunger(gu), "a refused hand use paid hunger");
        TendedGuItem item = (TendedGuItem) gu.getItem();
        GuItem.Refusal refusal = assertNotNull(
                item.getBehavior(HardshipBehavior.class).checkEffect(item, player, gu), () -> "no refusal");
        assertEquals("guzhenren.item.failed.auto_effect", refusal.key());
        assertFalse(item.canBeAmplified(), "it can still be amplified");
        helper.succeed();
    }

    private static void start(ServerPlayer player) {
        inSlot(player, BAG_SLOT, ModItems.HARDSHIP_STRENGTH_GU_4.get(), HUNGER);
        player.setHealth(player.getMaxHealth() * 0.7F);
        AutoUse.tryStart(player);
        assertTrue(HardshipStrength.isActive(player), "it did not start");
    }

    private static StrengthAmplifier amplifier(ServerPlayer player) {
        List<StrengthAmplifier> amplifiers = StrengthService.getAmplifiers(player);
        for (StrengthAmplifier amplifier : amplifiers) {
            if (amplifier.source().equals(ModPathEffectTypes.HARDSHIP_STRENGTH.getId())) return amplifier;
        }
        return null;
    }

    private static ServerPlayer human(GameTestHelper helper) {
        ServerPlayer player = awakened(helper);
        assertTrue(PathStrengthService.setHumanStrength(player, HumanStrength.JUN, 2), "the jun was refused");
        return player;
    }

    private static ServerPlayer awakened(GameTestHelper helper) {
        return GameTestGu.awakened(
                GameTestPlayers.survival(helper, "gzr-hardship-strength-gametest").onGround(), Rank.FIVE);
    }
}
