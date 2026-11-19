package net.alex.guzhenren.gametest;

import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.entity.beast.BeastPhantomEntity;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import net.alex.guzhenren.gameplay.path.strength.ComboRound;
import net.alex.guzhenren.gameplay.path.strength.HardshipStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.gameplay.path.strength.StrengthService;
import net.alex.guzhenren.item.gu.mortal.path.strength.AutoUse;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
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
 *   <li>前几段的拳不抽兽种；收尾招打中抽一次，同一招打多个目标不重抽；采矿模式的原版拳与持剑不开轮。</li>
 *   <li>收尾结束撤掉解放点数，解放兽种的移速借用到虚影显示期满；苦力下连续打完的轮数逐轮多抽一种，被打断即回到一种。</li>
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
        int settled = StrengthService.getUsablePoints(player);

        ComboRound.beginStrike(player, true);
        assertTrue(ComboRound.strike(player, LAND) > 0.0F, "a landed roll released nothing");
        assertTrue(StrengthService.getUsablePoints(player) > settled, "the finisher missed its release");
        ComboRound.complete(player, false);
        assertEquals(0, ComboRound.getReleasedExtra(player), "the release outlived its finisher");
        assertEquals(settled, StrengthService.getUsablePoints(player), "the points outlived the finisher");
        assertEquals(List.of(BeastStrengthFamily.HORSE), ComboRound.getFullyReleased(player),
                "the horse's move left with the finisher");

        helper.runAfterDelay(ComboRound.SHOWN_TICKS + 1, () -> {
            assertTrue(ComboRound.getFullyReleased(player).isEmpty(), "the horse's move outlived its phantom");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void hardshipDrawsOneMoreForEachFinishedRoundAndACutRoundStartsOver(GameTestHelper helper) {
        ServerPlayer player = GameTestGu.awakened(
                GameTestPlayers.survival(helper, "gzr-combo-round-gametest").onGround(), Rank.FIVE);
        PathStrengthService.grant(player, BeastStrength.BEAR);
        PathStrengthService.grant(player, BeastStrength.TIGER);
        PathStrengthService.grant(player, BeastStrength.STONE_TURTLE);
        inSlot(player, BAG_SLOT, ModItems.HARDSHIP_STRENGTH_GU_4.get(), HUNGER);
        player.setHealth(player.getMaxHealth() * 0.5F);
        AutoUse.tryStart(player);
        assertTrue(HardshipStrength.isActive(player), "hardship did not start");

        for (int round = 1; round <= 3; round++) {
            ComboRound.beginStrike(player, true);
            ComboRound.strike(player, MISS);
            assertEquals(round, ComboRound.getFamilies(player).size(), "round " + round + " drew the wrong count");
            ComboRound.complete(player, false);
        }
        ComboRound.beginStrike(player, true);
        ComboRound.beginStrike(player, false);
        assertEquals(0, ComboRound.getStreak(player), "a finisher cut by a new strike kept the streak");
        ComboRound.beginStrike(player, true);
        ComboRound.strike(player, MISS);
        assertEquals(1, ComboRound.getFamilies(player).size(), "the round after a cut did not draw one");
        ComboRound.complete(player, false);
        ComboRound.abandon(player);
        assertEquals(0, ComboRound.getStreak(player), "an abandoned combo kept the streak");
        ComboRound.beginStrike(player, true);
        ComboRound.complete(player, true);
        assertEquals(0, ComboRound.getStreak(player), "a finisher cut before it landed counted as finished");
        helper.succeed();
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

    private static void punch(GameTestHelper helper, ServerPlayer player, BlockPos at) {
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, at);
        assertTrue(pig.hurt(player.damageSources().playerAttack(player), 1.0F), "the punch did no damage");
    }
}
