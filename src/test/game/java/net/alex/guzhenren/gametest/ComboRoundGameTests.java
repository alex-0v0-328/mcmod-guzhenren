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
 *   <li>前几段的拳不抽兽种；收尾招打中掷一次骰，同一招打多个目标不重掷；采矿模式的原版拳与持剑不开轮。</li>
 *   <li>没掷中不抽、不出虚影（开苦力亦然）；掷中不开苦力抽 1 种，开苦力抽 min(连击数, 兽种数) 种，被打断即回到 1 种；抽中
 *       的每种永久点数再计一份，{@code strike} 返回解放增量 × 0.125（含苦力整拳倍率）。</li>
 *   <li>限时兽力不抽、人力无解放；收尾结束撤掉解放点数与攻击属性，解放兽种的移速借用到虚影显示期满。</li>
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
    private static final int HARDSHIP_PERCENT = 120;
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
        assertEquals(HORSE_POINTS, ComboRound.getReleasedPoints(player), "the release is not the horse's own points");
        assertEquals(2 * HORSE_POINTS, StrengthService.getSettledPoints(player), "the released horse did not double");
        assertEquals(attack + HORSE_POINTS * PathStrengthData.ATTACK_PER_POINT,
                player.getAttributeValue(Attributes.ATTACK_DAMAGE), DELTA);
        ComboRound.complete(player, false);
        assertEquals(0, ComboRound.getReleasedPoints(player), "the release outlived its finisher");
        assertEquals(HORSE_POINTS, StrengthService.getSettledPoints(player), "the points outlived the finisher");
        assertEquals(attack, player.getAttributeValue(Attributes.ATTACK_DAMAGE), DELTA);
        assertEquals(List.of(BeastStrengthFamily.HORSE), ComboRound.getFullyReleased(player),
                "the horse's move left with the finisher");

        helper.runAfterDelay(ComboRound.SHOWN_TICKS + 1, () -> {
            assertTrue(ComboRound.getFullyReleased(player).isEmpty(), "the horse's move outlived its phantom");
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
            assertTrue(ComboRound.getFullyReleased(player).isEmpty(), "a missed roll released a family");
            assertEquals(0, ComboRound.getReleasedPoints(player), "a missed roll released points");
            assertTrue(BeastPhantomEntity.findOwned(player).isEmpty(), "a missed roll showed a phantom");
            assertEquals(0.0D, ComboRound.strike(player, LAND), 0.0D);
            assertTrue(ComboRound.getFamilies(player).isEmpty(), "the same finisher rolled again");
        }
        assertEquals(settled, StrengthService.getSettledPoints(hard), "a missed roll changed the settlement");
        ComboRound.complete(hard, false);
        assertEquals(1, ComboRound.getStreak(hard), "a finished round without a draw did not count");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aLandedRollDrawsOneFamilyWithoutHardshipWhateverTheStreak(GameTestHelper helper) {
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
            ComboRound.complete(player, false);
            assertEquals(0, ComboRound.getStreak(player), "a round without hardship kept a streak");
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void hardshipDrawsOneMoreForEachFinishedRoundAndACutRoundStartsOver(GameTestHelper helper) {
        ServerPlayer player = threeBeastsUnderHardship(helper);
        int plain = THREE_BEASTS * HARDSHIP_PERCENT / 100;
        assertEquals(plain, StrengthService.getSettledPoints(player), "the second hardship tier is not on the fist");

        for (int round = 1; round <= 4; round++) {
            ComboRound.beginStrike(player, true);
            float gain = ComboRound.strike(player, LAND);
            List<BeastStrengthFamily> drawn = ComboRound.getFamilies(player);
            assertEquals(Math.min(round, 3), drawn.size(), "round " + round + " drew the wrong count");
            int released = ComboRound.getReleasedPoints(drawn, PathStrengthService.get(player));
            int settled = (THREE_BEASTS + released) * HARDSHIP_PERCENT / 100;
            assertEquals(released, ComboRound.getReleasedPoints(player), "a drawn family was not released");
            assertEquals(settled, StrengthService.getSettledPoints(player), "round " + round + " settled wrong");
            assertEquals((settled - plain) * PathStrengthData.ATTACK_PER_POINT, gain, DELTA);
            assertEquals(drawn, ComboRound.getFullyReleased(player), "a drawn family lent no move");
            assertEquals(drawn.size(), BeastPhantomEntity.findOwned(player).size(), "a drawn family showed no phantom");
            ComboRound.complete(player, false);
        }
        assertEquals(THREE_BEASTS, ComboRound.getReleasedPoints(ComboRound.getAvailableFamilies(
                PathStrengthService.get(player)), PathStrengthService.get(player)), "three families do not sum to 152");

        ComboRound.beginStrike(player, true);
        ComboRound.beginStrike(player, false);
        assertEquals(0, ComboRound.getStreak(player), "a finisher cut by a new strike kept the streak");
        ComboRound.beginStrike(player, true);
        ComboRound.strike(player, LAND);
        assertEquals(1, ComboRound.getFamilies(player).size(), "the round after a cut did not draw one");
        ComboRound.complete(player, false);
        ComboRound.abandon(player);
        assertEquals(0, ComboRound.getStreak(player), "an abandoned combo kept the streak");
        ComboRound.beginStrike(player, true);
        ComboRound.complete(player, true);
        assertEquals(0, ComboRound.getStreak(player), "a finisher cut before it landed counted as finished");
        ComboRound.beginStrike(player, true);
        ComboRound.strike(player, MISS);
        ComboRound.complete(player, true);
        assertEquals(1, ComboRound.getStreak(player), "a finisher cut after it landed did not count");
        ComboRound.breakStreak(player);
        assertEquals(0, ComboRound.getStreak(player), "a broken streak stayed");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void onlyPermanentBeastsAreDrawnAndHumanStrengthReleasesNothing(GameTestHelper helper) {
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
                ComboRound.complete(beast, false);
            }

            ComboRound.beginStrike(human, true);
            assertEquals(0.0D, ComboRound.strike(human, LAND), 0.0D);
            assertTrue(ComboRound.getFamilies(human).isEmpty(), "human strength drew a family");
            assertEquals(0, ComboRound.getReleasedPoints(human), "human strength was released");
            assertEquals(2 * HumanStrength.JUN.getJin(), StrengthService.getSettledPoints(human));
            assertTrue(BeastPhantomEntity.findOwned(human).isEmpty(), "human strength showed a phantom");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void theFirstLandedHitMarksTheSpillAndAMissLeavesTheWholeFist(GameTestHelper helper) {
        ServerPlayer player = mortal(helper);
        assertEquals(10.0D, ComboRound.getSpill(player, 10.0F), 0.0D);
        ComboRound.markLanded(player, 10.0F, 4.0F);
        assertEquals(10.0D, ComboRound.getSpill(player, 10.0F), 0.0D);

        ComboRound.beginStrike(player, false);
        ComboRound.markLanded(player, 10.0F, 4.0F);
        assertEquals(10.0D, ComboRound.getSpill(player, 10.0F), 0.0D);

        ComboRound.beginStrike(player, true);
        assertEquals(10.0D, ComboRound.getSpill(player, 10.0F), 0.0D);
        ComboRound.markLanded(player, 10.0F, 4.0F);
        assertEquals(6.0D, ComboRound.getSpill(player, 10.0F), 0.0D);
        ComboRound.markLanded(player, 10.0F, 1.0F);
        assertEquals(6.0D, ComboRound.getSpill(player, 99.0F), 0.0D);
        ComboRound.complete(player, false);
        assertEquals(10.0D, ComboRound.getSpill(player, 10.0F), 0.0D);

        ComboRound.beginStrike(player, true);
        ComboRound.markLanded(player, 3.0F, 20.0F);
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
        assertTrue(pig.hurt(player.damageSources().playerAttack(player), 1.0F), "the punch did no damage");
    }
}
