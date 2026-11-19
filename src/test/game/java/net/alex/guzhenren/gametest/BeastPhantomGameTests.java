package net.alex.guzhenren.gametest;

import java.util.EnumSet;
import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.entity.beast.BeastPhantomEntity;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import net.alex.guzhenren.gameplay.path.strength.BodyOverload;
import net.alex.guzhenren.gameplay.path.strength.ComboRound;
import net.alex.guzhenren.gameplay.path.strength.HumanStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthData;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.gameplay.path.strength.StrengthService;
import net.alex.guzhenren.item.gu.mortal.behavior.TimedEffects;
import net.alex.guzhenren.item.gu.mortal.path.rule.AmplifyEffectGuItem.Amplification;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.alex.guzhenren.registry.damage.ModDamageTypes;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.GameTestApertures.mortal;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertFalse;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 活玩家连招轮[Combo Round]收尾招的兽力[Beast Strength]虚影、全面解放与空手命中的过载反噬（轮的边界归
 * {@code ComboRoundGameTests}）。
 * <ul>
 *   <li>无永久兽力（仅花豕蛊[Flower Boar Gu]亦然）不抽；掷中现一虚影，其兽种永久点数再计一份（×2，不按承受力截断）；没
 *       掷中不抽，同一收尾不补掷。</li>
 *   <li>五转全力以赴每次收尾现一个，中途结束只撤承受力、解放照旧；棕熊本力掷中单熊掷不中的骰（10% 对 5%）。</li>
 *   <li>收尾招命中：解放增量（点数 × 0.125）补进本击护甲前伤害，并记下溢出量；每次出手打中结算一次过载反噬（超出 ÷ 承受
 *       力 × 最大生命 × 50%，停在 2 血），同一招的后续命中、持剑与没打中都不扣，新出招再扣。</li>
 *   <li>虚影占槽：前两个在主人前方右、左，其余在身后扇形自最低排起，与朝向无关；同兽种再现原地续期，异兽种替换，寿命到
 *       或随主人消失。</li>
 *   <li>龟、牛同样成形，六兽种皆有槽；苦力连抽归 {@code ComboRoundGameTests}。</li>
 * </ul>
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BeastPhantomGameTests {

    private BeastPhantomGameTests() {}

    private static final double DELTA = 0.001D;
    private static final float LAND = 0.0F;
    private static final float MISS = 0.99F;
    private static final int EFFECT_TICKS = 600;
    private static final int SETTLE_TICKS = 2;
    private static final int RANK_FIVE = 56;
    private static final int RANK_FIVE_TIER = 4;
    private static final float TURNED_YAW = 90.0F;
    private static final int BEAR_POINTS = 40;
    private static final int TURTLE_POINTS = 48;
    private static final int BROWN_BEAR_POINTS = 96;
    private static final float BLOW = 1.0F;
    private static final float TARGET_HEALTH = 1000.0F;

    @GameTest(template = "empty9x9x9")
    public static void noPermanentBeastShowsNoPhantom(GameTestHelper helper) {
        ServerPlayer plain = mortal(helper);
        ServerPlayer boar = mortal(helper);
        TimedEffects.grantBuff(boar, (TendedGuItem) ModItems.FLOWER_BOAR_GU_1.get(), ModEffects.FLOWER_BOAR_GU, 0,
                EFFECT_TICKS, Amplification.NONE);
        plain.addEffect(ModEffects.instance(ModEffects.ALL_OUT_EFFORT_GU, EFFECT_TICKS, RANK_FIVE_TIER));
        boar.addEffect(ModEffects.instance(ModEffects.ALL_OUT_EFFORT_GU, EFFECT_TICKS, RANK_FIVE_TIER));

        helper.runAfterDelay(SETTLE_TICKS, () -> {
            assertEquals(0.0D, finish(plain, LAND), DELTA);
            assertEquals(0.0D, finish(boar, LAND), DELTA);
            punch(helper, boar, new BlockPos(2, 1, 2));
            for (ServerPlayer player : List.of(plain, boar)) {
                assertTrue(ComboRound.getFamilies(player).isEmpty(), "a body without a permanent beast drew");
                assertTrue(BeastPhantomEntity.findOwned(player).isEmpty(), "a body without a permanent beast "
                        + "showed a phantom");
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void aLandedRollShowsOnePhantomAndAFullRelease(GameTestHelper helper) {
        ServerPlayer player = rankFive(helper);
        PathStrengthService.grant(player, BeastStrength.BEAR);

        assertEquals(BEAR_POINTS * PathStrengthData.ATTACK_PER_POINT, finish(player, LAND), DELTA);
        assertEquals(BEAR_POINTS, ComboRound.getReleasedPoints(player));
        assertEquals(2 * BEAR_POINTS, StrengthService.getSettledPoints(player), "the released bear did not double");
        assertTrue(StrengthService.getSettledPoints(player) > StrengthService.getCapacity(player),
                "the release was cut at the capacity");
        assertEquals(RANK_FIVE, StrengthService.getCapacity(player), "the rank five capacity moved");
        List<BeastPhantomEntity> phantoms = BeastPhantomEntity.findOwned(player);
        assertEquals(1, phantoms.size(), "a landed roll did not show one phantom");
        assertEquals(BeastStrengthFamily.BEAR, phantoms.getFirst().getFamily(), "the phantom is not the bear");
        assertEquals(BeastPhantomEntity.Slot.RIGHT, phantoms.getFirst().getSlot(), "one phantom is not on the right");

        ServerPlayer missed = mortal(helper);
        PathStrengthService.grant(missed, BeastStrength.TIGER);
        assertEquals(0.0D, finish(missed, ComboRound.CHANCE), DELTA);
        assertTrue(ComboRound.isFinishing(missed), "a missed roll ended the finisher");
        assertTrue(ComboRound.getFamilies(missed).isEmpty(), "a missed roll drew a family");
        assertTrue(BeastPhantomEntity.findOwned(missed).isEmpty(), "a missed roll showed a phantom");
        assertEquals(0.0D, ComboRound.strike(missed, LAND), DELTA);
        assertTrue(ComboRound.getFamilies(missed).isEmpty(), "the same finisher rolled again");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theBrownBearLandsARollTheBearAloneMisses(GameTestHelper helper) {
        float roll = 0.07F;
        ServerPlayer bear = mortal(helper);
        PathStrengthService.grant(bear, BeastStrength.BEAR);
        finish(bear, roll);
        assertTrue(ComboRound.getFamilies(bear).isEmpty(), "the bear alone landed a 7% roll");

        ServerPlayer brownBear = mortal(helper);
        PathStrengthService.grant(brownBear, BeastStrength.BROWN_BEAR);
        finish(brownBear, roll);
        assertEquals(List.of(BeastStrengthFamily.BEAR), ComboRound.getFamilies(brownBear),
                "the brown bear missed a 7% roll");
        assertTrue(BeastPhantomEntity.findOwned(brownBear, BeastStrengthFamily.BEAR) != null,
                "the landed roll showed no bear phantom");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void allOutEffortShowsAPhantomEveryFinisher(GameTestHelper helper) {
        ServerPlayer player = mortal(helper);
        PathStrengthService.grant(player, BeastStrength.BEAR);
        player.addEffect(ModEffects.instance(ModEffects.ALL_OUT_EFFORT_GU, EFFECT_TICKS, RANK_FIVE_TIER));

        helper.runAfterDelay(SETTLE_TICKS, () -> {
            assertTrue(finish(player, MISS) > 0.0F, "the first finisher released nothing");
            assertTrue(BeastPhantomEntity.findOwned(player, BeastStrengthFamily.BEAR) != null, "no first phantom");
            ComboRound.complete(player, false);
            assertTrue(finish(player, MISS) > 0.0F, "the next finisher released nothing");
            assertTrue(BeastPhantomEntity.findOwned(player, BeastStrengthFamily.BEAR) != null, "no second phantom");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void theEndOfAllOutEffortKeepsTheReleaseAndDropsOnlyItsCapacity(GameTestHelper helper) {
        ServerPlayer player = rankFive(helper);
        PathStrengthService.grant(player, BeastStrength.BEAR);
        player.addEffect(ModEffects.instance(ModEffects.ALL_OUT_EFFORT_GU, EFFECT_TICKS, RANK_FIVE_TIER));

        helper.runAfterDelay(SETTLE_TICKS, () -> {
            assertEquals(BEAR_POINTS * PathStrengthData.ATTACK_PER_POINT, finish(player, MISS), DELTA);
            assertEquals(3 * RANK_FIVE, StrengthService.getCapacity(player), "rank five All-Out did not triple");
            assertEquals(0, StrengthService.getOverloadPoints(StrengthService.getSettledPoints(player),
                    StrengthService.getCapacity(player)), "the lifted capacity did not hold the released bear");

            player.removeEffect(ModEffects.ALL_OUT_EFFORT_GU);
            assertEquals(BEAR_POINTS, ComboRound.getReleasedPoints(player), "the release left with the effect");
            assertEquals(List.of(BeastStrengthFamily.BEAR), ComboRound.getFamilies(player));
            assertEquals(2 * BEAR_POINTS, StrengthService.getSettledPoints(player), "the points left with the effect");
            assertEquals(2 * BEAR_POINTS - RANK_FIVE, StrengthService.getOverloadPoints(
                    StrengthService.getSettledPoints(player), StrengthService.getCapacity(player)),
                    "the lift outlived the effect");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void theFinishersReleaseJoinsTheBlowThatDrewItAndMarksItsSpill(GameTestHelper helper) {
        ServerPlayer player = mortal(helper);
        PathStrengthService.grant(player, BeastStrength.BROWN_BEAR);
        player.addEffect(ModEffects.instance(ModEffects.ALL_OUT_EFFORT_GU, EFFECT_TICKS, RANK_FIVE_TIER));
        float gain = (float) (BROWN_BEAR_POINTS * PathStrengthData.ATTACK_PER_POINT);

        helper.runAfterDelay(SETTLE_TICKS, () -> {
            Villager opener = villager(helper, new BlockPos(2, 1, 2));
            ComboRound.beginStrike(player, false);
            hit(player, opener);
            assertEquals(BLOW, TARGET_HEALTH - opener.getHealth(), DELTA);
            assertEquals(BLOW, ComboRound.getSpill(player, BLOW), DELTA);

            Villager first = villager(helper, new BlockPos(2, 1, 4));
            Villager second = villager(helper, new BlockPos(6, 1, 6));
            ComboRound.beginStrike(player, true);
            hit(player, first);
            assertEquals(BLOW + gain, TARGET_HEALTH - first.getHealth(), DELTA);
            assertEquals(BLOW + gain - TARGET_HEALTH, ComboRound.getSpill(player, BLOW), DELTA);
            assertEquals(2 * BROWN_BEAR_POINTS, StrengthService.getSettledPoints(player));

            hit(player, second);
            assertEquals(BLOW, TARGET_HEALTH - second.getHealth(), DELTA);
            assertEquals(BLOW + gain - TARGET_HEALTH, ComboRound.getSpill(player, BLOW), DELTA);
            assertEquals(1, BeastPhantomEntity.findOwned(player).size(), "a second target drew again");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void aBlowThatKillsLeavesItsSpillForTheAreaImpact(GameTestHelper helper) {
        ServerPlayer player = mortal(helper);
        PathStrengthService.grant(player, BeastStrength.BROWN_BEAR);
        player.addEffect(ModEffects.instance(ModEffects.ALL_OUT_EFFORT_GU, EFFECT_TICKS, RANK_FIVE_TIER));
        float gain = (float) (BROWN_BEAR_POINTS * PathStrengthData.ATTACK_PER_POINT);

        helper.runAfterDelay(SETTLE_TICKS, () -> {
            Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(2, 1, 2));
            float health = pig.getHealth();
            assertTrue(health < BLOW + gain, "the pig outlives the released blow");
            ComboRound.beginStrike(player, true);
            hit(player, pig);
            assertTrue(pig.isDeadOrDying(), "the released blow did not kill the pig");
            assertEquals(BLOW + gain - health, ComboRound.getSpill(player, BLOW), DELTA);
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void aLandedFistPaysTheOverloadOnceAStrike(GameTestHelper helper) {
        ServerPlayer player = vulnerable(helper);
        assertTrue(PathStrengthService.setHumanStrength(player, HumanStrength.TEN_JIN, 2), "the jin were refused");
        int capacity = StrengthService.getCapacity(player);
        assertEquals(StrengthService.MORTAL_CAPACITY, capacity, "the mortal capacity moved");
        float recoil = (2 * HumanStrength.TEN_JIN.getJin() - capacity) / (float) capacity * player.getMaxHealth()
                * 0.5F;
        assertEquals(2.5D, recoil, DELTA);
        float health = player.getHealth();

        ComboRound.beginStrike(player, false);
        assertEquals(health, player.getHealth(), DELTA);
        hit(player, villager(helper, new BlockPos(2, 1, 2)));
        assertEquals(health - recoil, player.getHealth(), DELTA);
        DamageSource source = player.getLastDamageSource();
        assertTrue(source != null && source.is(ModDamageTypes.BODY_OVERLOAD), "the recoil is not the body overload");
        assertTrue(source.is(DamageTypeTags.NO_KNOCKBACK), "the body overload may knock back");
        assertTrue(source.is(DamageTypeTags.BYPASSES_ARMOR), "the body overload stops at armor");

        hit(player, villager(helper, new BlockPos(6, 1, 2)));
        assertEquals(health - recoil, player.getHealth(), DELTA);

        ComboRound.beginStrike(player, true);
        Villager finished = villager(helper, new BlockPos(2, 1, 6));
        hit(player, finished);
        hit(player, finished);
        assertEquals(health - 2.0F * recoil, player.getHealth(), DELTA);
        ComboRound.complete(player, false);

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        ComboRound.beginStrike(player, false);
        hit(player, villager(helper, new BlockPos(6, 1, 6)));
        assertEquals(health - 2.0F * recoil, player.getHealth(), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theOverloadCountsTheReleaseOfTheBlowThatDrewIt(GameTestHelper helper) {
        ServerPlayer player = GameTestGu.awakened(vulnerable(helper), Rank.FIVE);
        PathStrengthService.grant(player, BeastStrength.BROWN_BEAR);
        player.addEffect(ModEffects.instance(ModEffects.ALL_OUT_EFFORT_GU, EFFECT_TICKS, RANK_FIVE_TIER));

        helper.runAfterDelay(SETTLE_TICKS, () -> {
            int capacity = 3 * RANK_FIVE;
            assertEquals(capacity, StrengthService.getCapacity(player), "rank five All-Out did not triple");
            float health = player.getHealth();
            float recoil = (2 * BROWN_BEAR_POINTS - capacity) / (float) capacity * player.getMaxHealth() * 0.5F;

            ComboRound.beginStrike(player, false);
            hit(player, villager(helper, new BlockPos(2, 1, 2)));
            assertEquals(health, player.getHealth(), DELTA);
            assertTrue(player.getLastDamageSource() == null, "96 points under a capacity of 168 paid an overload");

            ComboRound.beginStrike(player, true);
            hit(player, villager(helper, new BlockPos(2, 1, 4)));
            assertEquals(List.of(BeastStrengthFamily.BEAR), ComboRound.getFamilies(player));
            assertEquals(health - recoil, player.getHealth(), DELTA);
            hit(player, villager(helper, new BlockPos(6, 1, 6)));
            assertEquals(health - recoil, player.getHealth(), DELTA);
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void theOverloadStopsAtTwoHealthAndNeverKills(GameTestHelper helper) {
        ServerPlayer player = vulnerable(helper);
        PathStrengthService.grant(player, BeastStrength.TIGER);
        assertTrue(StrengthService.getSettledPoints(player) > 2 * StrengthService.getCapacity(player),
                "the tiger does not pass twice the mortal capacity");

        ComboRound.beginStrike(player, false);
        hit(player, villager(helper, new BlockPos(6, 1, 2)));
        assertEquals(2.0D, player.getHealth(), DELTA);
        assertEquals(2.0D, BodyOverload.HEALTH_FLOOR, 0.0D);
        ComboRound.beginStrike(player, false);
        hit(player, villager(helper, new BlockPos(6, 1, 4)));
        assertEquals(2.0D, player.getHealth(), DELTA);
        assertTrue(player.isAlive(), "the overload killed its puncher");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void threePhantomsStandRightLeftAndAtTheFanBaseBehind(GameTestHelper helper) {
        ServerPlayer player = mortal(helper);
        player.setYRot(TURNED_YAW);
        BeastPhantomEntity.show(player,
                List.of(BeastStrengthFamily.TIGER, BeastStrengthFamily.BEAR, BeastStrengthFamily.BOAR));

        assertSlot(player, BeastStrengthFamily.TIGER, BeastPhantomEntity.Slot.RIGHT);
        assertSlot(player, BeastStrengthFamily.BEAR, BeastPhantomEntity.Slot.LEFT);
        assertSlot(player, BeastStrengthFamily.BOAR, BeastPhantomEntity.Slot.FAN_RIGHT_4);
        assertEquals(new Vec3(BeastPhantomEntity.SIDE_OFFSET, 0.0D, BeastPhantomEntity.FORWARD_OFFSET),
                BeastPhantomEntity.getOffset(BeastPhantomEntity.Slot.RIGHT), "the right slot moved");
        assertEquals(new Vec3(BeastPhantomEntity.FAN_COLUMN * (BeastPhantomEntity.FAN_ROWS - 0.5D),
                        BeastPhantomEntity.FAN_BASE_HEIGHT, -BeastPhantomEntity.FAN_BACK_OFFSET),
                BeastPhantomEntity.getOffset(BeastPhantomEntity.Slot.FAN_RIGHT_4),
                "the fan base is not behind, low and outward");
        assertEquals(new Vec3(BeastPhantomEntity.FAN_COLUMN * 0.5D,
                        BeastPhantomEntity.FAN_BASE_HEIGHT + BeastPhantomEntity.FAN_ROW * (BeastPhantomEntity.FAN_ROWS - 1),
                        -BeastPhantomEntity.FAN_BACK_OFFSET),
                BeastPhantomEntity.getOffset(BeastPhantomEntity.Slot.FAN_RIGHT_1),
                "the fan apex is not behind, above and inward");
        Vec3 facing = Vec3.directionFromRotation(0.0F, player.getYRot());
        assertTrue(getAhead(player, BeastStrengthFamily.TIGER, facing) > 0.0D, "the ground slot is not ahead");
        assertTrue(getAhead(player, BeastStrengthFamily.BOAR, facing) < 0.0D, "the fan is not behind its owner");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void sixFamiliesFillTheGroundPairAndTheLowerFanRows(GameTestHelper helper) {
        ServerPlayer player = mortal(helper);
        BeastPhantomEntity.show(player, List.of(BeastStrengthFamily.OX, BeastStrengthFamily.TIGER,
                BeastStrengthFamily.BEAR, BeastStrengthFamily.BOAR, BeastStrengthFamily.HORSE,
                BeastStrengthFamily.TURTLE));

        assertEquals(6, BeastPhantomEntity.findOwned(player).size(), "six families did not all take shape");
        assertSlot(player, BeastStrengthFamily.OX, BeastPhantomEntity.Slot.RIGHT);
        assertSlot(player, BeastStrengthFamily.TIGER, BeastPhantomEntity.Slot.LEFT);
        assertSlot(player, BeastStrengthFamily.BEAR, BeastPhantomEntity.Slot.FAN_RIGHT_4);
        assertSlot(player, BeastStrengthFamily.BOAR, BeastPhantomEntity.Slot.FAN_LEFT_4);
        assertSlot(player, BeastStrengthFamily.HORSE, BeastPhantomEntity.Slot.FAN_RIGHT_3);
        assertSlot(player, BeastStrengthFamily.TURTLE, BeastPhantomEntity.Slot.FAN_LEFT_3);
        Vec3 outer = BeastPhantomEntity.getOffset(BeastPhantomEntity.Slot.FAN_LEFT_4);
        Vec3 inner = BeastPhantomEntity.getOffset(BeastPhantomEntity.Slot.FAN_LEFT_1);
        assertTrue(outer.x < inner.x && outer.y < inner.y, "the fan does not open outward and downward");
        assertEquals(BeastPhantomEntity.FAN_BASE_HEIGHT, outer.y, DELTA);
        for (BeastPhantomEntity.Slot slot : BeastPhantomEntity.Slot.values()) {
            if (slot.getRow() > 0) {
                assertTrue(Math.abs(BeastPhantomEntity.getOffset(slot).x) != BeastPhantomEntity.SIDE_OFFSET,
                        slot + " stands straight behind a ground slot");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void pickSlotTakesTheFirstFreeSlotInOrderAndNoneWhenFull(GameTestHelper helper) {
        assertEquals(BeastPhantomEntity.Slot.FAN_RIGHT_4, BeastPhantomEntity.pickSlot(
                EnumSet.of(BeastPhantomEntity.Slot.RIGHT, BeastPhantomEntity.Slot.LEFT)), "the fan base was skipped");
        assertEquals(BeastPhantomEntity.Slot.FAN_RIGHT_1, BeastPhantomEntity.pickSlot(EnumSet.complementOf(
                EnumSet.of(BeastPhantomEntity.Slot.FAN_RIGHT_1, BeastPhantomEntity.Slot.FAN_LEFT_1))),
                "the fan apex did not come last");
        assertEquals(BeastPhantomEntity.Slot.LEFT, BeastPhantomEntity.pickSlot(
                EnumSet.of(BeastPhantomEntity.Slot.RIGHT, BeastPhantomEntity.Slot.FAN_RIGHT_4)), "a freed ground slot was skipped");
        assertEquals(null, BeastPhantomEntity.pickSlot(EnumSet.allOf(BeastPhantomEntity.Slot.class)),
                "a full fan still found a slot");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aTurtleReleaseCountsAndShowsItsPhantom(GameTestHelper helper) {
        ServerPlayer player = rankFive(helper);
        PathStrengthService.grant(player, BeastStrength.STONE_TURTLE);

        assertEquals(TURTLE_POINTS * PathStrengthData.ATTACK_PER_POINT, finish(player, LAND), DELTA);
        assertEquals(List.of(BeastStrengthFamily.TURTLE), ComboRound.getFamilies(player), "the turtle was not drawn");
        assertEquals(2 * TURTLE_POINTS, StrengthService.getSettledPoints(player), "the turtle did not double");
        assertEquals(RANK_FIVE + 128, StrengthService.getCapacity(player), "the turtle's capacity is missing");
        BeastPhantomEntity turtle = BeastPhantomEntity.findOwned(player, BeastStrengthFamily.TURTLE);
        assertTrue(turtle != null, "the turtle took no shape");
        assertEquals(BeastPhantomEntity.Slot.RIGHT, turtle.getSlot(), "the turtle missed the first slot");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theSameFamilyIsRenewedInPlaceAndAnotherReplacesIt(GameTestHelper helper) {
        ServerPlayer player = mortal(helper);
        BeastPhantomEntity.show(player, List.of(BeastStrengthFamily.TIGER));
        BeastPhantomEntity tiger = BeastPhantomEntity.findOwned(player, BeastStrengthFamily.TIGER);
        assertTrue(tiger != null, "no tiger shown");

        int renewAt = BeastPhantomEntity.LIFETIME_TICKS - 5;
        helper.runAfterDelay(renewAt, () -> BeastPhantomEntity.show(player, List.of(BeastStrengthFamily.TIGER)));
        helper.runAfterDelay(BeastPhantomEntity.getShownTicks() + 2, () -> {
            assertTrue(tiger.isAlive(), "the renewed tiger went at its first lifetime");
            assertEquals(1, BeastPhantomEntity.findOwned(player).size(), "the renewal spawned another phantom");
        });
        helper.runAfterDelay(renewAt + BeastPhantomEntity.getShownTicks() + 3, () -> {
            assertFalse(tiger.isAlive(), "the renewed tiger outlived its renewed lifetime");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void aSteadyFistRenewsThePhantomAcrossFinishers(GameTestHelper helper) {
        ServerPlayer player = mortal(helper);
        PathStrengthService.grant(player, BeastStrength.BEAR);
        player.addEffect(ModEffects.instance(ModEffects.ALL_OUT_EFFORT_GU, EFFECT_TICKS, RANK_FIVE_TIER));

        helper.runAfterDelay(SETTLE_TICKS, () -> {
            finish(player, MISS);
            ComboRound.complete(player, false);
            BeastPhantomEntity bear = BeastPhantomEntity.findOwned(player, BeastStrengthFamily.BEAR);
            assertTrue(bear != null, "no bear shown");
            helper.runAfterDelay(BeastPhantomEntity.getShownTicks() - 2, () -> {
                assertTrue(finish(player, MISS) > 0.0F, "the next finisher released nothing");
                assertTrue(bear.isAlive(), "the bear went between two finishers");
                assertEquals(1, BeastPhantomEntity.findOwned(player).size(), "the next finisher respawned the bear");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void anotherFamilyReplacesThePhantom(GameTestHelper helper) {
        ServerPlayer player = mortal(helper);
        BeastPhantomEntity.show(player, List.of(BeastStrengthFamily.TIGER));
        BeastPhantomEntity tiger = BeastPhantomEntity.findOwned(player, BeastStrengthFamily.TIGER);
        assertTrue(tiger != null, "no tiger shown");

        BeastPhantomEntity.show(player, List.of(BeastStrengthFamily.BEAR));
        assertFalse(tiger.isAlive(), "the tiger outlived a round without it");
        BeastPhantomEntity bear = BeastPhantomEntity.findOwned(player, BeastStrengthFamily.BEAR);
        assertTrue(bear != null && bear.getSlot() == BeastPhantomEntity.Slot.RIGHT, "the bear did not take the right");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void thePhantomGoesAfterItsLifetime(GameTestHelper helper) {
        ServerPlayer player = mortal(helper);
        PathStrengthService.grant(player, BeastStrength.TIGER);
        finish(player, LAND);

        helper.runAfterDelay(BeastPhantomEntity.getShownTicks() - 2, () ->
                assertFalse(BeastPhantomEntity.findOwned(player).isEmpty(), "the phantom left before its linger ran out"));
        helper.runAfterDelay(BeastPhantomEntity.getShownTicks() + 2, () -> {
            assertTrue(BeastPhantomEntity.findOwned(player).isEmpty(), "the phantom outlived its lifetime");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void thePhantomAndTheRoundGoWithItsOwner(GameTestHelper helper) {
        ServerPlayer player = mortal(helper);
        PathStrengthService.grant(player, BeastStrength.BEAR);
        finish(player, LAND);
        BeastPhantomEntity phantom = BeastPhantomEntity.findOwned(player, BeastStrengthFamily.BEAR);
        assertTrue(phantom != null, "no phantom shown");

        player.kill();
        assertFalse(ComboRound.isFinishing(player), "the finisher outlived its owner");
        helper.runAfterDelay(2, () -> {
            assertFalse(phantom.isAlive(), "the phantom outlived its owner");
            helper.succeed();
        });
    }

    private static float finish(ServerPlayer player, float roll) {
        ComboRound.beginStrike(player, true);
        return ComboRound.strike(player, roll);
    }

    private static void punch(GameTestHelper helper, ServerPlayer player, BlockPos at) {
        hit(player, helper.spawnWithNoFreeWill(EntityType.PIG, at));
    }

    private static void hit(ServerPlayer player, LivingEntity target) {
        target.invulnerableTime = 0;
        assertTrue(target.hurt(player.damageSources().playerAttack(player), BLOW), "the punch did no damage");
    }

    private static Villager villager(GameTestHelper helper, BlockPos at) {
        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, at);
        villager.getAttribute(Attributes.MAX_HEALTH).setBaseValue(TARGET_HEALTH);
        villager.setHealth(TARGET_HEALTH);
        return villager;
    }

    private static ServerPlayer vulnerable(GameTestHelper helper) {
        return GameTestPlayers.survival(helper, "gzr-beast-phantom-gametest").pastSpawnInvulnerability().onGround();
    }

    private static void assertSlot(ServerPlayer player, BeastStrengthFamily family, BeastPhantomEntity.Slot slot) {
        BeastPhantomEntity phantom = BeastPhantomEntity.findOwned(player, family);
        assertTrue(phantom != null, "no " + family + " shown");
        assertEquals(slot, phantom.getSlot(), family + " is in the wrong slot");
        assertEquals(BeastPhantomEntity.getOffset(slot).lengthSqr(),
                phantom.position().distanceToSqr(player.position()), DELTA);
    }

    private static double getAhead(ServerPlayer player, BeastStrengthFamily family, Vec3 facing) {
        BeastPhantomEntity phantom = BeastPhantomEntity.findOwned(player, family);
        assertTrue(phantom != null, "no " + family + " shown");
        return phantom.position().subtract(player.position()).dot(facing);
    }

    private static ServerPlayer rankFive(GameTestHelper helper) {
        ServerPlayer player = GameTestApertures.awakenedMortal(helper);
        ApertureService.setRank(player, Rank.FIVE);
        return player;
    }
}
