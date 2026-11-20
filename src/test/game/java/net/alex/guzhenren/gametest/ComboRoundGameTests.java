package net.alex.guzhenren.gametest;

import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.entity.beast.BeastPhantomEntity;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.path.strength.AttackDamageService;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import net.alex.guzhenren.gameplay.path.strength.ComboRound;
import net.alex.guzhenren.gameplay.path.strength.HardshipStrength;
import net.alex.guzhenren.gameplay.path.strength.HumanStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthData;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.gameplay.path.strength.StrengthCrit;
import net.alex.guzhenren.gameplay.path.strength.StrengthService;
import net.alex.guzhenren.item.gu.mortal.behavior.TimedEffects;
import net.alex.guzhenren.item.gu.mortal.path.rule.AmplifyEffectGuItem.Amplification;
import net.alex.guzhenren.item.gu.mortal.path.strength.AutoUse;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.ComboAttackAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import static net.alex.guzhenren.gametest.GameTestApertures.mortal;
import static net.alex.guzhenren.gametest.GameTestGu.inSlot;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertFalse;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 活玩家的连招轮[Combo Round]：一轮一套连招，只在收尾招结算。
 * <ul>
 *   <li>暴击率：兽力、人力共用平时 5%、棕熊本力 +5%；全力以赴三 / 四 / 五转 80% / 90% / 100%，不加兽力加成；未入两流
 *       为 0，力气蛊不加。</li>
 *   <li>前几段的拳不掷骰；收尾招一轮只掷一次暴击骰，掷定后 {@code rollsCrit}、{@code strike} 都复用；同一招打多个目标不
 *       重掷；采矿模式的原版拳与持剑不开轮。人力流暴击才算范围伤害（{@code rollsArea}），兽力流暴击只抽兽种。</li>
 *   <li>没暴击不抽、不出虚影（开苦力亦然）；兽力暴击抽 min(1 + 苦力档位, 兽种数) 种，不开苦力或 0 档抽 1 种；抽中的每种
 *       永久点数再计一份，{@code strike} 返回暴击增量 × 0.125（含苦力伤害倍率）。</li>
 *   <li>限时兽力不抽、人力无兽种暴击；收尾结束撤掉暴击点数与攻击属性，暴击兽种的移速借用到虚影显示期满。</li>
 *   <li>收尾招首次命中记下溢出量（本击伤害 − 目标挨打前生命），未命中时为整拳伤害；过载反噬每次出手放行一次，新出招或
 *       10 tick 后重新放行。</li>
 *   <li>Epic Fight 钩子：战斗模式下第三段连击、冲刺拳、空中拳起招即收尾，最后一段结束或连段清零即离开收尾，采矿模式不
 *       起招。</li>
 *   <li>Epic Fight 空手的收尾假设：三段连击都是连击动作，第三段、冲刺拳与空中拳各只有一段判定；月刃挥臂片段在服务端读得
 *       到 0.3 秒时长，续接窗口据此为 6 tick。</li>
 * </ul>
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ComboRoundGameTests {

    private ComboRoundGameTests() {}

    private static final float MISS = 0.99F;
    private static final float LAND = 0.0F;
    private static final int EFFECT_TICKS = 600;
    private static final int SETTLE_TICKS = 2;
    private static final int RANK_FIVE_TIER = 4;
    private static final int BAG_SLOT = 9;
    private static final int HUNGER = 98;
    private static final long THROW_TICKS = 6L;
    private static final int BEAR_POINTS = 40;
    private static final int HORSE_POINTS = 48;
    private static final int THREE_BEASTS = 40 + 64 + 48;
    private static final int RANK_THREE_TIER = 2;
    private static final int RANK_FOUR_TIER = 3;
    private static final double DELTA = 1.0E-4D;

    @GameTest(template = "empty9x9x9")
    public static void onlyTheFinisherDrawsAndOnlyOnce(GameTestHelper helper) {
        ServerPlayer player = mortal(helper);
        PathStrengthService.grant(player, BeastStrength.TIGER);
        PathStrengthService.grant(player, BeastStrength.BEAR);
        player.addEffect(ModEffects.instance(ModEffects.ALL_OUT_EFFORT_GU, EFFECT_TICKS, RANK_FIVE_TIER));

        helper.runAfterDelay(SETTLE_TICKS, () -> {
            ComboRound.beginStrike(player, false);
            punch(helper, player, new BlockPos(2, 1, 2));
            assertTrue(BeastPhantomEntity.findOwned(player).isEmpty(), "an opening punch drew");

            ComboRound.beginStrike(player, true);
            punch(helper, player, new BlockPos(2, 1, 4));
            List<BeastStrengthFamily> drawn = ComboRound.getFamilies(player);
            assertEquals(1, drawn.size(), "the All-Out finisher did not draw one family");
            BeastPhantomEntity phantom = BeastPhantomEntity.findOwned(player, drawn.getFirst());
            assertTrue(phantom != null, "the drawn family showed no phantom");

            punch(helper, player, new BlockPos(6, 1, 6));
            assertEquals(drawn, ComboRound.getFamilies(player));
            assertEquals(0.0D, ComboRound.strike(player, LAND), 0.0D);
            assertEquals(1, BeastPhantomEntity.findOwned(player).size(), "a second target drew again");
            assertTrue(phantom.isAlive(), "a second target replaced the phantom");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void aMiningModePunchOrASwordDrawsNothing(GameTestHelper helper) {
        ServerPlayer bare = mortal(helper);
        ServerPlayer armed = mortal(helper);
        for (ServerPlayer player : List.of(bare, armed)) {
            PathStrengthService.grant(player, BeastStrength.TIGER);
            player.addEffect(ModEffects.instance(ModEffects.ALL_OUT_EFFORT_GU, EFFECT_TICKS, RANK_FIVE_TIER));
        }
        armed.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));

        helper.runAfterDelay(SETTLE_TICKS, () -> {
            punch(helper, bare, new BlockPos(2, 1, 2));
            assertFalse(ComboRound.isFinishing(bare), "a punch outside a combo opened a round");
            ComboRound.beginStrike(armed, true);
            punch(helper, armed, new BlockPos(6, 1, 6));
            for (ServerPlayer player : List.of(bare, armed)) {
                assertTrue(ComboRound.getFamilies(player).isEmpty(), "a round drew without a fist finisher");
                assertTrue(BeastPhantomEntity.findOwned(player).isEmpty(), "a phantom showed without a fist finisher");
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void theFinishersEndTakesBackItsPointsAndTheMovesOutlastIt(GameTestHelper helper) {
        ServerPlayer player = GameTestApertures.awakenedMortal(helper);
        ApertureService.setRank(player, Rank.FIVE);
        PathStrengthService.grant(player, BeastStrength.GALLOPING_HORSE);
        assertEquals(HORSE_POINTS, StrengthService.getSettledPoints(player), "the horse does not settle 48 points");
        AttackDamageService.refresh(player);
        double attack = player.getAttributeValue(Attributes.ATTACK_DAMAGE);

        ComboRound.beginStrike(player, true);
        assertEquals(HORSE_POINTS * PathStrengthData.ATTACK_PER_POINT, ComboRound.strike(player, LAND), DELTA);
        assertEquals(HORSE_POINTS, ComboRound.getCritPoints(player), "the crit is not the horse's own points");
        assertEquals(2 * HORSE_POINTS, StrengthService.getSettledPoints(player), "the crit horse did not double");
        assertEquals(attack + HORSE_POINTS * PathStrengthData.ATTACK_PER_POINT,
                player.getAttributeValue(Attributes.ATTACK_DAMAGE), DELTA);
        ComboRound.complete(player);
        assertEquals(0, ComboRound.getCritPoints(player), "the crit outlived its finisher");
        assertEquals(HORSE_POINTS, StrengthService.getSettledPoints(player), "the points outlived the finisher");
        assertEquals(attack, player.getAttributeValue(Attributes.ATTACK_DAMAGE), DELTA);
        assertEquals(List.of(BeastStrengthFamily.HORSE), ComboRound.getCritFamilies(player),
                "the horse's move left with the finisher");

        helper.runAfterDelay(ComboRound.SHOWN_TICKS + 1, () -> {
            assertTrue(ComboRound.getCritFamilies(player).isEmpty(), "the horse's move outlived its phantom");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void aMissedRollDrawsNothingEvenUnderHardship(GameTestHelper helper) {
        ServerPlayer plain = mortal(helper);
        PathStrengthService.grant(plain, BeastStrength.TIGER);
        ServerPlayer hard = threeBeastsUnderHardship(helper);
        int settled = StrengthService.getSettledPoints(hard);

        for (ServerPlayer player : List.of(plain, hard)) {
            ComboRound.beginStrike(player, true);
            assertEquals(0.0D, ComboRound.strike(player, MISS), 0.0D);
            assertTrue(ComboRound.getFamilies(player).isEmpty(), "a missed roll drew a family");
            assertTrue(ComboRound.getCritFamilies(player).isEmpty(), "a missed roll crit a family");
            assertEquals(0, ComboRound.getCritPoints(player), "a missed roll added crit points");
            assertTrue(BeastPhantomEntity.findOwned(player).isEmpty(), "a missed roll showed a phantom");
            assertEquals(0.0D, ComboRound.strike(player, LAND), 0.0D);
            assertTrue(ComboRound.getFamilies(player).isEmpty(), "the same finisher rolled again");
        }
        assertEquals(settled, StrengthService.getSettledPoints(hard), "a missed roll changed the settlement");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aCritDrawsOneFamilyWithoutHardshipEveryRound(GameTestHelper helper) {
        ServerPlayer player = mortal(helper);
        PathStrengthService.grant(player, BeastStrength.BEAR);
        PathStrengthService.grant(player, BeastStrength.TIGER);
        PathStrengthService.grant(player, BeastStrength.STONE_TURTLE);

        for (int round = 1; round <= 3; round++) {
            ComboRound.beginStrike(player, true);
            float gain = ComboRound.strike(player, LAND);
            List<BeastStrengthFamily> drawn = ComboRound.getFamilies(player);
            assertEquals(1, drawn.size(), "round " + round + " without hardship did not draw one");
            int released = PathStrengthService.get(player).getBeastPoints(drawn.getFirst());
            assertEquals(released * PathStrengthData.ATTACK_PER_POINT, gain, DELTA);
            assertEquals(THREE_BEASTS + released, StrengthService.getSettledPoints(player));
            assertEquals(1, BeastPhantomEntity.findOwned(player).size(), "one draw did not show one phantom");
            ComboRound.complete(player);
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void underHardshipACritDrawsOneMoreFamilyForEachTier(GameTestHelper helper) {
        ServerPlayer player = threeBeastsUnderHardship(helper);
        float max = player.getMaxHealth();
        float[] health = { 0.8F, 0.7F, 0.5F, 0.35F };
        int[] tiers = { 0, 1, 2, 3 };
        int[] percents = { 100, 110, 120, 130 };

        for (int index = 0; index < health.length; index++) {
            player.setHealth(max * health[index]);
            String at = "at " + health[index];
            assertEquals(tiers[index], HardshipStrength.getTier(player), "the Hardship tier " + at);
            int plain = THREE_BEASTS * percents[index] / 100;
            assertEquals(plain, StrengthService.getSettledPoints(player), "the damage factor " + at);

            ComboRound.beginStrike(player, true);
            float gain = ComboRound.strike(player, LAND);
            List<BeastStrengthFamily> drawn = ComboRound.getFamilies(player);
            assertEquals(Math.min(1 + tiers[index], 3), drawn.size(), "the draw count " + at);
            int crit = StrengthCrit.getCritPoints(drawn, PathStrengthService.get(player));
            int settled = (THREE_BEASTS + crit) * percents[index] / 100;
            assertEquals(crit, ComboRound.getCritPoints(player), "a drawn family was not counted " + at);
            assertEquals(settled, StrengthService.getSettledPoints(player), "the settlement " + at);
            assertEquals((settled - plain) * PathStrengthData.ATTACK_PER_POINT, gain, DELTA);
            assertEquals(drawn, ComboRound.getCritFamilies(player), "a drawn family lent no move " + at);
            assertEquals(drawn.size(), BeastPhantomEntity.findOwned(player).size(), "a family showed no phantom " + at);
            ComboRound.complete(player);
        }
        assertEquals(THREE_BEASTS, StrengthCrit.getCritPoints(StrengthCrit.getAvailableFamilies(
                PathStrengthService.get(player)), PathStrengthService.get(player)), "three families do not sum to 152");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aRoundRollsItsCritOnceAndKeepsIt(GameTestHelper helper) {
        ServerPlayer player = mortal(helper);
        PathStrengthService.grant(player, BeastStrength.BEAR);

        ComboRound.beginStrike(player, false);
        assertFalse(ComboRound.rollsCrit(player, LAND), "an opening punch rolled a crit");
        ComboRound.beginStrike(player, true);
        assertTrue(ComboRound.rollsCrit(player, LAND), "a 0 roll missed a 5% crit");
        assertTrue(ComboRound.rollsCrit(player, MISS), "the round rolled a second time");
        assertTrue(ComboRound.rollsCrit(player), "the round's crit was not kept");
        assertEquals(BEAR_POINTS * PathStrengthData.ATTACK_PER_POINT, ComboRound.strike(player, MISS), DELTA);
        assertEquals(List.of(BeastStrengthFamily.BEAR), ComboRound.getFamilies(player),
                "the strike did not reuse the landed roll");
        ComboRound.complete(player);

        ComboRound.beginStrike(player, true);
        assertFalse(ComboRound.rollsCrit(player, MISS), "a 99% roll landed a 5% crit");
        assertFalse(ComboRound.rollsCrit(player, LAND), "the missed round rolled again");
        assertEquals(0.0D, ComboRound.strike(player, LAND), 0.0D);
        assertTrue(ComboRound.getFamilies(player).isEmpty(), "the strike did not reuse the missed roll");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void onlyAHumanCritCountsAsAnAreaDamage(GameTestHelper helper) {
        ServerPlayer human = mortal(helper);
        assertTrue(PathStrengthService.setHumanStrength(human, HumanStrength.JIN, 1), "the jin was refused");
        ServerPlayer beast = mortal(helper);
        PathStrengthService.grant(beast, BeastStrength.BEAR);
        ServerPlayer bare = mortal(helper);

        ComboRound.beginStrike(human, true);
        assertTrue(ComboRound.rollsCrit(human, LAND), "the human branch missed a 0 roll");
        assertTrue(ComboRound.rollsArea(human), "a human crit is no area damage");
        ComboRound.beginStrike(human, true);
        assertFalse(ComboRound.rollsCrit(human, MISS), "the human branch landed a 99% roll");
        assertFalse(ComboRound.rollsArea(human), "a human miss counted as an area damage");

        ComboRound.beginStrike(beast, true);
        assertTrue(ComboRound.rollsCrit(beast, LAND), "the beast branch missed a 0 roll");
        assertFalse(ComboRound.rollsArea(beast), "a beast crit counted as an area damage");

        ComboRound.beginStrike(bare, true);
        assertFalse(ComboRound.rollsCrit(bare, LAND), "a body with no branch landed a crit");
        assertFalse(ComboRound.rollsArea(bare), "a body with no branch rolled an area damage");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theCritChanceIsSharedAndAllOutEffortSetsIt(GameTestHelper helper) {
        ServerPlayer bare = mortal(helper);
        ServerPlayer human = mortal(helper);
        assertTrue(PathStrengthService.setHumanStrength(human, HumanStrength.JIN, 1), "the jin was refused");
        ServerPlayer bear = mortal(helper);
        PathStrengthService.grant(bear, BeastStrength.BEAR);
        ServerPlayer brownBear = mortal(helper);
        PathStrengthService.grant(brownBear, BeastStrength.BROWN_BEAR);

        assertEquals(0.0D, StrengthCrit.getChance(bare), DELTA);
        assertEquals(0.05D, StrengthCrit.getChance(human), DELTA);
        assertEquals(0.05D, StrengthCrit.getChance(bear), DELTA);
        assertEquals(0.10D, StrengthCrit.getChance(brownBear), DELTA);

        human.addEffect(ModEffects.instance(ModEffects.STRENGTH_QI_GU, EFFECT_TICKS, 0));
        assertEquals(0.05D, StrengthCrit.getChance(human), DELTA);
        bare.addEffect(ModEffects.instance(ModEffects.ALL_OUT_EFFORT_GU, EFFECT_TICKS, RANK_FIVE_TIER));
        assertEquals(0.0D, StrengthCrit.getChance(bare), DELTA);
        bear.addEffect(ModEffects.instance(ModEffects.ALL_OUT_EFFORT_GU, EFFECT_TICKS, RANK_FOUR_TIER));
        assertEquals(0.90D, StrengthCrit.getChance(bear), DELTA);
        human.addEffect(ModEffects.instance(ModEffects.ALL_OUT_EFFORT_GU, EFFECT_TICKS, RANK_FIVE_TIER));
        assertEquals(1.0D, StrengthCrit.getChance(human), DELTA);
        brownBear.addEffect(ModEffects.instance(ModEffects.ALL_OUT_EFFORT_GU, EFFECT_TICKS, RANK_THREE_TIER));
        assertEquals(0.80D, StrengthCrit.getChance(brownBear), DELTA);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void onlyPermanentBeastsAreDrawnAndHumanStrengthCritsNoFamily(GameTestHelper helper) {
        ServerPlayer beast = mortal(helper);
        PathStrengthService.grant(beast, BeastStrength.BEAR);
        TimedEffects.grantBuff(beast, (TendedGuItem) ModItems.FLOWER_BOAR_GU_1.get(), ModEffects.FLOWER_BOAR_GU, 0,
                EFFECT_TICKS, Amplification.NONE);
        int total = StrengthService.getStrengthPoints(beast);
        assertTrue(total > BEAR_POINTS, "the timed boar added no points");

        ServerPlayer human = mortal(helper);
        assertTrue(PathStrengthService.setHumanStrength(human, HumanStrength.JUN, 2), "the jun was refused");
        human.addEffect(ModEffects.instance(ModEffects.ALL_OUT_EFFORT_GU, EFFECT_TICKS, RANK_FIVE_TIER));

        helper.runAfterDelay(SETTLE_TICKS, () -> {
            for (int round = 0; round < 8; round++) {
                ComboRound.beginStrike(beast, true);
                assertEquals(BEAR_POINTS * PathStrengthData.ATTACK_PER_POINT, ComboRound.strike(beast, LAND), DELTA);
                assertEquals(List.of(BeastStrengthFamily.BEAR), ComboRound.getFamilies(beast),
                        "the timed boar was drawn");
                assertEquals(total + BEAR_POINTS, StrengthService.getSettledPoints(beast),
                        "the timed boar was doubled");
                ComboRound.complete(beast);
            }

            ComboRound.beginStrike(human, true);
            assertEquals(0.0D, ComboRound.strike(human, LAND), 0.0D);
            assertTrue(ComboRound.getFamilies(human).isEmpty(), "human strength drew a family");
            assertEquals(0, ComboRound.getCritPoints(human), "human strength added crit points");
            assertEquals(2 * HumanStrength.JUN.getJin(), StrengthService.getSettledPoints(human));
            assertTrue(BeastPhantomEntity.findOwned(human).isEmpty(), "human strength showed a phantom");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void theFirstLandedHitMarksTheSpillAndAMissLeavesTheWholeFist(GameTestHelper helper) {
        ServerPlayer player = mortal(helper);
        Pig target = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(4, 2, 4));
        assertEquals(10.0D, ComboRound.getSpill(player, 10.0F), 0.0D);
        ComboRound.markLanded(player, target, 10.0F, 4.0F);
        assertEquals(10.0D, ComboRound.getSpill(player, 10.0F), 0.0D);

        ComboRound.beginStrike(player, false);
        ComboRound.markLanded(player, target, 10.0F, 4.0F);
        assertEquals(10.0D, ComboRound.getSpill(player, 10.0F), 0.0D);

        ComboRound.beginStrike(player, true);
        assertEquals(10.0D, ComboRound.getSpill(player, 10.0F), 0.0D);
        ComboRound.markLanded(player, target, 10.0F, 4.0F);
        assertEquals(6.0D, ComboRound.getSpill(player, 10.0F), 0.0D);
        ComboRound.markLanded(player, target, 10.0F, 1.0F);
        assertEquals(6.0D, ComboRound.getSpill(player, 99.0F), 0.0D);
        ComboRound.complete(player);
        assertEquals(10.0D, ComboRound.getSpill(player, 10.0F), 0.0D);

        ComboRound.beginStrike(player, true);
        ComboRound.markLanded(player, target, 3.0F, 20.0F);
        assertEquals(-17.0D, ComboRound.getSpill(player, 3.0F), 0.0D);
        ComboRound.beginStrike(player, true);
        assertEquals(3.0D, ComboRound.getSpill(player, 3.0F), 0.0D);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theOverloadIsClaimedOnceAStrike(GameTestHelper helper) {
        ServerPlayer player = mortal(helper);
        assertTrue(ComboRound.claimOverload(player), "the first hit did not claim the overload");
        assertFalse(ComboRound.claimOverload(player), "a second hit of the same strike claimed again");
        ComboRound.beginStrike(player, false);
        assertTrue(ComboRound.claimOverload(player), "a new strike did not claim again");
        assertFalse(ComboRound.claimOverload(player), "the new strike claimed twice");
        ComboRound.beginStrike(player, true);
        assertTrue(ComboRound.claimOverload(player), "a finisher did not claim again");
        assertFalse(ComboRound.claimOverload(player), "the finisher claimed twice");

        helper.runAfterDelay(ComboRound.OVERLOAD_GATE_TICKS - 2, () ->
                assertFalse(ComboRound.claimOverload(player), "the gate opened before its ten ticks"));
        helper.runAfterDelay(ComboRound.OVERLOAD_GATE_TICKS + 1, () -> {
            assertTrue(ComboRound.claimOverload(player), "a punch after the gate did not claim");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void theEpicFightHooksMarkTheFinisherItsEndAndACut(GameTestHelper helper) {
        ServerPlayer player = mortal(helper);
        ServerPlayerPatch patch = EpicFightCapabilities.getServerPlayerPatch(player);
        assertTrue(patch != null, "Epic Fight player patch missing");

        patch.toVanillaMode(false);
        GameTestEpicFight.actionStarted(patch, Animations.FIST_AUTO3);
        assertFalse(ComboRound.isFinishing(player), "a mining-mode strike opened a finisher");

        patch.toEpicFightMode(false);
        GameTestEpicFight.actionStarted(patch, Animations.FIST_AUTO1);
        assertFalse(ComboRound.isFinishing(player), "the first combo attack finished a round");
        GameTestEpicFight.actionStarted(patch, Animations.FIST_AUTO3);
        assertTrue(ComboRound.isFinishing(player), "the third combo attack did not finish the round");
        GameTestEpicFight.attackPhaseEnded(patch, Animations.FIST_AUTO3, false);
        assertFalse(ComboRound.isFinishing(player), "the finisher outlived its last phase");

        for (AnimationManager.AnimationAccessor<? extends AttackAnimation> single
                : List.of(Animations.FIST_DASH, Animations.FIST_AIR_SLASH)) {
            GameTestEpicFight.actionStarted(patch, Animations.FIST_AUTO1);
            GameTestEpicFight.actionStarted(patch, single);
            GameTestEpicFight.comboCutBy(patch, 1, single);
            assertTrue(ComboRound.isFinishing(player), single + " cut its own finisher when it cut the combo");
            GameTestEpicFight.comboReset(patch, 2);
            assertFalse(ComboRound.isFinishing(player), "a combo cut kept " + single + " finishing");
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void epicFightsEmptyHandFinishersHitOnce(GameTestHelper helper) {
        for (AnimationManager.AnimationAccessor<? extends AttackAnimation> combo
                : List.of(Animations.FIST_AUTO1, Animations.FIST_AUTO2, Animations.FIST_AUTO3)) {
            assertTrue(combo.get() instanceof ComboAttackAnimation, combo + " is no combo attack");
        }
        for (AnimationManager.AnimationAccessor<? extends AttackAnimation> finisher
                : List.of(Animations.FIST_AUTO3, Animations.FIST_DASH, Animations.FIST_AIR_SLASH)) {
            assertEquals(1, finisher.get().phases.length, finisher + " hits more than once");
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theThrowClipsReadTheirLengthOnTheServer(GameTestHelper helper) {
        for (String clip : List.of("throw_sweep", "throw_punch_left", "throw_punch_right")) {
            AnimationManager.AnimationAccessor<StaticAnimation> animation =
                    AnimationManager.byKey(Guzhenren.id("biped/skill/" + clip));
            assertEquals(THROW_TICKS, Math.round(animation.get().getTotalTime() * 20.0F), clip + " length");
        }
        helper.succeed();
    }

    private static ServerPlayer threeBeastsUnderHardship(GameTestHelper helper) {
        ServerPlayer player = GameTestGu.awakened(
                GameTestPlayers.survival(helper, "gzr-combo-round-gametest").onGround(), Rank.FIVE);
        PathStrengthService.grant(player, BeastStrength.BEAR);
        PathStrengthService.grant(player, BeastStrength.TIGER);
        PathStrengthService.grant(player, BeastStrength.STONE_TURTLE);
        inSlot(player, BAG_SLOT, ModItems.HARDSHIP_STRENGTH_GU_4.get(), HUNGER);
        player.setHealth(player.getMaxHealth() * 0.5F);
        AutoUse.tryStart(player);
        assertTrue(HardshipStrength.isActive(player), "hardship did not start");
        return player;
    }

    private static void punch(GameTestHelper helper, ServerPlayer player, BlockPos at) {
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, at);
        assertTrue(Punches.land(player, pig, 1.0F), "the punch did no damage");
    }
}
