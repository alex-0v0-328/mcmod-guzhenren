package net.alex.guzhenren.gametest;

import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.path.PathEffect;
import net.alex.guzhenren.gameplay.path.PathEffectService;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.BodyOverload;
import net.alex.guzhenren.gameplay.path.strength.ComboRound;
import net.alex.guzhenren.gameplay.path.strength.HardshipStrength;
import net.alex.guzhenren.gameplay.path.strength.HumanStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.gameplay.path.strength.StrengthAmplifier;
import net.alex.guzhenren.gameplay.path.strength.StrengthPathBranch;
import net.alex.guzhenren.gameplay.path.strength.StrengthService;
import net.alex.guzhenren.gameplay.path.strength.StrengthSnapshot;
import net.alex.guzhenren.item.GuItem;
import net.alex.guzhenren.item.gu.mortal.path.strength.AutoUse;
import net.alex.guzhenren.item.gu.mortal.behavior.TimedEffects;
import net.alex.guzhenren.item.gu.mortal.path.rule.AmplifyEffectGuItem.Amplification;
import net.alex.guzhenren.item.gu.mortal.path.strength.HardshipBehavior;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.alex.guzhenren.registry.effect.ModPathEffectTypes;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.GameTestGu.hold;
import static net.alex.guzhenren.gametest.GameTestGu.hunger;
import static net.alex.guzhenren.gametest.GameTestGu.inSlot;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertFalse;
import static net.alex.guzhenren.gametest.UnitAsserts.assertNotNull;
import static net.alex.guzhenren.gametest.UnitAsserts.assertSame;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 活玩家苦力蛊[Hardship Strength Gu]的自动状态。
 * <ul>
 *   <li>身体已入力道分支（永久人力、永久兽力或生效中的限时兽力）时生命低于 75% 自启并付一次费，无则不启，承受力高低
 *       不拦；窗口 30 秒（青牛劳力加 10 秒），回血不止；{@code isActive} 随之开关；手动催动被拒、不计费。</li>
 *   <li>各档位边界结算读其伤害倍率（+10% 到 +50%），人力与兽力[Beast Strength]虚影身体相同；不给力量点
 *       [Strength Points]、不加承受力，失去分支即无倍率；放大后超出承受力的部分按过载反噬扣血。</li>
 *   <li>{@code getTier(Player)}：状态未开、未入分支或生命 ≥ 75% 为 0，否则为伤势档位 1–5。</li>
 *   <li>运行中受伤或回血跨档，攻击属性不等每秒心跳即按新档位的伤害倍率更新：受伤当即，回血至 tick 末（检查等
 *       2 tick，模拟玩家不 tick、没有心跳）；回到 75% 以上倍率归 1。</li>
 *   <li>人力身体持两层钧（60 点），兽力身体持熊（40 点），均五转（承受力 72）。</li>
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
    private static final int[] DAMAGE_PERCENT = { 10, 20, 30, 40, 50, 50 };
    private static final int[] TIERS = { 1, 2, 3, 4, 5, 5 };
    private static final int EFFECT_TICKS = 600;
    private static final int SETTLE_TICKS = 2;
    private static final double DELTA = 0.001D;

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
        assertTrue(amplifier(player) == null, "a healed body still hit harder");
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
    public static void aTimedBeastIsABranchEnoughToStartAndHitHarder(GameTestHelper helper) {
        ServerPlayer player = awakened(helper);
        inSlot(player, BAG_SLOT, ModItems.HARDSHIP_STRENGTH_GU_4.get(), HUNGER);
        TimedEffects.grantBuff(player, (TendedGuItem) ModItems.FLOWER_BOAR_GU_1.get(), ModEffects.FLOWER_BOAR_GU, 0,
                EFFECT_TICKS, Amplification.NONE);
        assertEquals(0, StrengthService.getPermanentPoints(player), "the timed boar counted as permanent");
        assertSame(StrengthPathBranch.BEAST_STRENGTH_PHANTOM, PathStrengthService.getHeldBranch(player));
        assertTrue(new HardshipBehavior().canStart(player), "a timed boar is no branch to Hardship");
        player.setHealth(player.getMaxHealth() * 0.5F);

        AutoUse.tryStart(player);
        assertTrue(HardshipStrength.isActive(player), "a timed boar holds the beast branch, yet it did not start");
        StrengthAmplifier amplifier = assertNotNull(amplifier(player), () -> "no amplifier on a timed beast body");
        assertEquals(20, amplifier.damagePercent(), "50% health is not tier two");
        assertEquals(24, StrengthService.getStrengthPoints(player));
        assertEquals(28, StrengthService.getSettledPoints(player), "24 x 1.2 = 28.8");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aSmallCapacityDoesNotKeepItFromStarting(GameTestHelper helper) {
        ServerPlayer mortal = GameTestPlayers.survival(helper, "gzr-hardship-mortal-gametest").onGround();
        assertEquals(12, StrengthService.getCapacity(mortal));
        assertFalse(new HardshipBehavior().canStart(mortal), "a body with no branch may start");
        assertTrue(PathStrengthService.setHumanStrength(mortal, HumanStrength.JIN, 1), "the jin was refused");
        assertTrue(new HardshipBehavior().canStart(mortal), "a 12 capacity kept a branch from starting");
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
    public static void withoutABranchItNeverStartsOrPays(GameTestHelper helper) {
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
    public static void aHumanBodyHitsHarderByTierAndGainsNoCapacity(GameTestHelper helper) {
        ServerPlayer player = human(helper);
        start(player);
        int capacity = StrengthService.getCapacity(player);
        int permanent = StrengthService.getPermanentPoints(player);
        assertEquals(72, capacity, "a bare rank five body does not carry 72");
        assertEquals(60, permanent, "the body does not hold 60 points of jun");

        for (int bound = 0; bound < BOUNDS.length; bound++) {
            player.setHealth(player.getMaxHealth() * BOUNDS[bound]);
            String at = "at " + BOUNDS[bound];
            StrengthAmplifier amplifier = assertNotNull(amplifier(player), () -> "no amplifier " + at);
            int settled = permanent * (100 + DAMAGE_PERCENT[bound]) / 100;
            assertTrue(amplifier.family() == null, "the amplifier has a family " + at);
            assertEquals(0, amplifier.points(), "it lent points " + at);
            assertEquals(DAMAGE_PERCENT[bound], amplifier.damagePercent(), at);
            assertEquals(TIERS[bound], HardshipStrength.getTier(player), "the tier " + at);
            assertEquals(permanent, StrengthService.getStrengthPoints(player), at);
            assertEquals(capacity, StrengthService.getCapacity(player), at);
            assertEquals(settled, StrengthService.getSettledPoints(player), at);
            StrengthSnapshot snapshot = StrengthService.snapshot(player);
            assertEquals(Math.max(0, settled - capacity), snapshot.overloadPoints(snapshot.settledPoints()), at);
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aBeastPhantomBodyHitsHarderByTheSameTiers(GameTestHelper helper) {
        ServerPlayer player = awakened(helper);
        assertTrue(PathStrengthService.grant(player, BeastStrength.BEAR), "the bear was refused");
        start(player);
        int capacity = StrengthService.getCapacity(player);
        int[] settled = { 44, 48, 52, 56, 60, 60 };

        for (int bound = 0; bound < BOUNDS.length; bound++) {
            player.setHealth(player.getMaxHealth() * BOUNDS[bound]);
            String at = "at " + BOUNDS[bound];
            StrengthAmplifier amplifier = assertNotNull(amplifier(player), () -> "no amplifier " + at);
            assertEquals(0, amplifier.points(), "it lent points " + at);
            assertEquals(DAMAGE_PERCENT[bound], amplifier.damagePercent(), at);
            assertEquals(TIERS[bound], HardshipStrength.getTier(player), "the tier " + at);
            assertEquals(40, StrengthService.getStrengthPoints(player), at);
            assertEquals(capacity, StrengthService.getCapacity(player), at);
            assertEquals(settled[bound], StrengthService.getSettledPoints(player), at);
            StrengthSnapshot snapshot = StrengthService.snapshot(player);
            assertEquals(Math.max(0, settled[bound] - capacity), snapshot.overloadPoints(snapshot.settledPoints()), at);
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theTierJoinsTheCritAndTheChargeInOneSettlement(GameTestHelper helper) {
        ServerPlayer player = awakened(helper);
        for (BeastStrength beast : List.of(BeastStrength.BEAR, BeastStrength.TIGER, BeastStrength.WHITE_BOAR,
                BeastStrength.BLACK_BOAR)) {
            assertTrue(PathStrengthService.grant(player, beast), beast + " was refused");
        }
        start(player);
        player.setHealth(player.getMaxHealth() * 0.45F);

        StrengthSnapshot snapshot = StrengthService.snapshot(player);
        assertEquals(152, snapshot.totalPoints());
        assertEquals(1.3D, snapshot.damageFactor(), DELTA);
        assertEquals(197, StrengthService.getSettledPoints(player), "152 x 1.3 = 197.6");
        assertEquals(842, snapshot.settle(BeastStrength.TIGER.getPoints(), 3.0D), "(40 + 64 x 2 + 48) x 1.3 x 3");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aBodyThatLostItsBranchHitsNoHarder(GameTestHelper helper) {
        ServerPlayer player = human(helper);
        start(player);
        player.setHealth(player.getMaxHealth() * 0.2F);
        assertEquals(40, assertNotNull(amplifier(player), () -> "no amplifier at tier four").damagePercent());

        assertTrue(PathStrengthService.setHumanStrength(player, HumanStrength.JUN, 0), "the jun stayed");
        assertSame(StrengthPathBranch.NORMAL, PathStrengthService.getHeldBranch(player));
        assertTrue(HardshipStrength.isActive(player), "the window closed before its recheck");
        assertTrue(amplifier(player) == null, "a body with no branch still hit harder");
        assertEquals(0, HardshipStrength.getTier(player), "a body with no branch kept its tier");
        assertEquals(1.0D, StrengthService.snapshot(player).damageFactor(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theTierIsZeroOffTheStateOrAtThreeQuarters(GameTestHelper helper) {
        ServerPlayer player = human(helper);
        float max = player.getMaxHealth();
        player.setHealth(max * 0.2F);
        assertEquals(0, HardshipStrength.getTier(player), "a body with the state off had a tier");

        start(player);
        player.setHealth(max * 0.2F);
        assertEquals(4, HardshipStrength.getTier(player), "20% health under the state is not tier four");
        player.setHealth(max * 0.75F);
        assertEquals(0, HardshipStrength.getTier(player), "75% health had a tier");
        player.setHealth(max);
        assertEquals(0, HardshipStrength.getTier(player), "full health had a tier");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theHarderPunchPastTheCapacityRecoils(GameTestHelper helper) {
        ServerPlayer player = fighter(helper, "gzr-hardship-recoil-gametest");
        assertTrue(PathStrengthService.setHumanStrength(player, HumanStrength.TEN_JIN, 6), "the ten jin was refused");
        assertTrue(PathStrengthService.setHumanStrength(player, HumanStrength.JIN, 3), "the jin were refused");
        float max = player.getMaxHealth();
        player.setHealth(max);
        strike(helper, player, new BlockPos(2, 1, 2));
        assertEquals(max, player.getHealth(), DELTA);

        start(player);
        assertEquals(69, StrengthService.getSettledPoints(player), "63 x 1.1");
        strike(helper, player, new BlockPos(6, 1, 2));
        assertEquals(max * 0.7F, player.getHealth(), DELTA);

        player.setHealth(max * 0.5F);
        assertEquals(75, StrengthService.getSettledPoints(player), "63 x 1.2");
        strike(helper, player, new BlockPos(2, 1, 6));
        assertEquals(max * 0.5F - 3.0F / 72.0F * max * BodyOverload.RECOIL_RATIO, player.getHealth(), DELTA);
        ComboRound.clear(player);
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

    @GameTest(template = "empty9x9x9")
    public static void aWoundAcrossTiersMovesTheAttackAtOnce(GameTestHelper helper) {
        ServerPlayer player = fighter(helper, "gzr-hardship-wound-gametest");
        double base = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        assertTrue(PathStrengthService.setHumanStrength(player, HumanStrength.JUN, 2), "the jun was refused");
        assertEquals(base + 7.5D, player.getAttributeValue(Attributes.ATTACK_DAMAGE), DELTA);
        start(player);
        float max = player.getMaxHealth();

        assertTrue(player.hurt(player.damageSources().magic(), max * 0.15F), "the wound did not land");
        assertEquals(max * 0.55F, player.getHealth(), DELTA);
        assertEquals(72, StrengthService.getSettledPoints(player), "60 x 1.2");
        assertEquals(base + 9.0D, player.getAttributeValue(Attributes.ATTACK_DAMAGE), DELTA);

        player.invulnerableTime = 0;
        assertTrue(player.hurt(player.damageSources().magic(), max * 0.3F), "the second wound did not land");
        assertEquals(max * 0.25F, player.getHealth(), DELTA);
        assertEquals(84, StrengthService.getSettledPoints(player), "60 x 1.4");
        assertEquals(base + 10.5D, player.getAttributeValue(Attributes.ATTACK_DAMAGE), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aHealAcrossTiersMovesTheAttackByTheEndOfTheTick(GameTestHelper helper) {
        ServerPlayer player = fighter(helper, "gzr-hardship-heal-gametest");
        double base = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        assertTrue(PathStrengthService.setHumanStrength(player, HumanStrength.JUN, 2), "the jun was refused");
        start(player);
        float max = player.getMaxHealth();
        assertTrue(player.hurt(player.damageSources().magic(), max * 0.45F), "the wound did not land");
        assertEquals(max * 0.25F, player.getHealth(), DELTA);
        assertEquals(base + 10.5D, player.getAttributeValue(Attributes.ATTACK_DAMAGE), DELTA);

        player.heal(max * 0.25F);
        assertEquals(max * 0.5F, player.getHealth(), DELTA);
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            assertEquals(base + 9.0D, player.getAttributeValue(Attributes.ATTACK_DAMAGE), DELTA);

            player.heal(max * 0.3F);
            assertEquals(max * 0.8F, player.getHealth(), DELTA);
            helper.runAfterDelay(SETTLE_TICKS, () -> {
                assertTrue(HardshipStrength.isActive(player), "the heal closed the window");
                assertTrue(amplifier(player) == null, "a healed body still hit harder");
                assertEquals(base + 7.5D, player.getAttributeValue(Attributes.ATTACK_DAMAGE), DELTA);
                helper.succeed();
            });
        });
    }

    private static ServerPlayer fighter(GameTestHelper helper, String name) {
        return GameTestGu.awakened(GameTestPlayers.survival(helper, name).pastSpawnInvulnerability().onGround(),
                Rank.FIVE);
    }

    private static void strike(GameTestHelper helper, ServerPlayer player, BlockPos at) {
        ComboRound.beginStrike(player, false);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, at);
        assertTrue(Punches.land(player, pig, 1.0F), "the punch did no damage");
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
