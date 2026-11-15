package net.alex.guzhenren.gametest;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.effect.timed.FlowerBoarGuEffect;
import net.alex.guzhenren.effect.timed.VitalityLeafGuEffect;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.Skin;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.item.gu.tending.GuUpkeep;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.alex.guzhenren.network.ClientIntents;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.alex.guzhenren.registry.item.ModDataComponents;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;
import static net.alex.guzhenren.gametest.GameTestGu.click;
import static net.alex.guzhenren.gametest.GameTestGu.hold;
import static net.alex.guzhenren.gametest.GameTestGu.hunger;
import static net.alex.guzhenren.gametest.GameTestGu.inSlot;
import static net.alex.guzhenren.gametest.GameTestPlayers.exhausted;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertFalse;
import static net.alex.guzhenren.gametest.UnitAsserts.assertNotNull;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 活玩家的功倍蛊[Amplify Effect Gu]。
 * <ul>
 *   <li>副手：主手蛊效果时长乘本蛊转数，自付真元[Essence]与一次饱食[Hunger]；高于本转或不请求增幅的蛊不增幅、不计费；饱
 *       食见底仍落地后死；自身催动被拒，副手点击放行。</li>
 *   <li>日切扣量后背包平滑石每块补 1 点，伤者先回血。</li>
 *   <li>轮盘（{@code ClientIntents.amplifyDrive}）：主手增幅蛊不经蓄力催动快捷栏蛊；一次性蛊从其格扣；池不足用随身元石
 *       [Primeval Stone]；高于转数、真元与元石不够付两蛊、手无增幅蛊或选当前格均不落地。</li>
 * </ul>
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AmplifyEffectGuGameTests {
    private AmplifyEffectGuGameTests() {}

    private static final int USE_HUNGER = 3;
    private static final int DAY_HUNGER = 1;

    @GameTest(template = "empty9x9x9")
    public static void anOffhandAmplifierTriplesABuffAndPaysItsOwnShare(GameTestHelper helper) {
        ServerPlayer player = awakened(helper, null);
        ItemStack boar = hold(player, InteractionHand.MAIN_HAND, ModItems.FLOWER_BOAR_GU_1.get(), 12);
        ItemStack amplifier = hold(player, InteractionHand.OFF_HAND, ModItems.AMPLIFY_EFFECT_GU_3.get(), 16);
        long essence = ApertureEssenceService.getSpendable(player);

        click(helper, player, boar);

        assertEquals(FlowerBoarGuEffect.DURATION_TICKS * 3, duration(player, ModEffects.FLOWER_BOAR_GU));
        assertEquals(essence - 40L - 2_560L, ApertureEssenceService.getSpendable(player));
        assertEquals(12 - USE_HUNGER, hunger(boar));
        assertEquals(16 - USE_HUNGER, hunger(amplifier));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aGuAboveItsRankLandsPlainAndLeavesTheAmplifierUnbilled(GameTestHelper helper) {
        ServerPlayer player = awakened(helper, null);
        PathStrengthService.grant(player, BeastStrength.WHITE_BOAR);
        ItemStack effort = hold(player, InteractionHand.MAIN_HAND, ModItems.ALL_OUT_EFFORT_GU_3.get(), 48);
        ItemStack amplifier = hold(player, InteractionHand.OFF_HAND, ModItems.AMPLIFY_EFFECT_GU_2.get(), 16);
        long essence = ApertureEssenceService.getSpendable(player);

        click(helper, player, effort);

        assertEquals(60 * Ticks.SECOND, duration(player, ModEffects.ALL_OUT_EFFORT_GU));
        assertEquals(essence - 2_560L, ApertureEssenceService.getSpendable(player));
        assertEquals(16, hunger(amplifier));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aTimedSkinRunsLongerAndAPermanentOneNeverBillsIt(GameTestHelper helper) {
        ServerPlayer jade = awakened(helper, null);
        ItemStack jadeSkin = hold(jade, InteractionHand.MAIN_HAND, ModItems.JADE_SKIN_GU_1.get(), 12);
        ItemStack jadeAmplifier = hold(jade, InteractionHand.OFF_HAND, ModItems.AMPLIFY_EFFECT_GU_2.get(), 16);

        click(helper, jade, jadeSkin);

        assertEquals(Skin.JADE_SKIN, BodyService.getSkin(jade));
        assertEquals(2L * 60 * Ticks.SECOND, BodyService.getSkinTicksLeft(jade));
        assertEquals(16 - USE_HUNGER, hunger(jadeAmplifier));
        assertFalse(((TendedGuItem) ModItems.BRONZE_SKIN_GU_1.get()).canBeAmplified(),
                "a permanent skin was billed to the amplifier");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aVitalityLeafAndAWatchGuRunFiveTimesAsLong(GameTestHelper helper) {
        ServerPlayer player = awakened(helper, null);
        ItemStack amplifier = hold(player, InteractionHand.OFF_HAND, ModItems.AMPLIFY_EFFECT_GU_5.get(), 128);
        ItemStack leaf = new ItemStack(ModItems.VITALITY_LEAF_GU_1.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, leaf);

        click(helper, player, leaf);
        ItemStack watch = hold(player, InteractionHand.MAIN_HAND, ModItems.THIRD_WATCH_GU_5.get(), 0);
        click(helper, player, watch);

        assertEquals(VitalityLeafGuEffect.DURATION_TICKS * 5, duration(player, ModEffects.VITALITY_LEAF_GU));
        assertEquals(5 * 5 * Ticks.MINUTE, duration(player, ModEffects.THIRD_WATCH_GU));
        assertEquals(128 - 2 * USE_HUNGER, hunger(amplifier));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void anAmplifierOnAnEmptyBarLandsTheEffectThenDies(GameTestHelper helper) {
        List<Component> inbox = new ArrayList<>();
        ServerPlayer player = awakened(helper, inbox);
        hold(player, InteractionHand.OFF_HAND, ModItems.AMPLIFY_EFFECT_GU_2.get(), 0);
        ItemStack leaf = new ItemStack(ModItems.VITALITY_LEAF_GU_1.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, leaf);

        click(helper, player, leaf);

        assertEquals(VitalityLeafGuEffect.DURATION_TICKS * 2, duration(player, ModEffects.VITALITY_LEAF_GU));
        assertTrue(player.getOffhandItem().isEmpty(), "the exhausted amplifier stayed in the offhand");
        assertTrue(exhausted(inbox), "no exhaustion message");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void itsOwnUseIsRefusedAndItsOffhandClickPasses(GameTestHelper helper) {
        ServerPlayer player = awakened(helper, null);
        ItemStack amplifier = hold(player, InteractionHand.MAIN_HAND, ModItems.AMPLIFY_EFFECT_GU_3.get(), 16);
        long essence = ApertureEssenceService.getSpendable(player);

        click(helper, player, amplifier);

        assertEquals(essence, ApertureEssenceService.getSpendable(player), "a refused use paid essence");
        assertEquals(16, hunger(amplifier));

        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifier);
        InteractionResult offhand = amplifier.getItem()
                .use(helper.getLevel(), player, InteractionHand.OFF_HAND).getResult();
        assertEquals(InteractionResult.PASS, offhand);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void smoothStoneInTheBagFeedsACarriedOneAfterTheDayOnePointAPiece(GameTestHelper helper) {
        ServerPlayer player = awakened(helper, null);
        ItemStack amplifier = hold(player, InteractionHand.MAIN_HAND, ModItems.AMPLIFY_EFFECT_GU_3.get(), 20);
        ItemStack stone = new ItemStack(Items.SMOOTH_STONE, 3);
        player.getInventory().setItem(9, stone);

        GuUpkeep.tickCarried(player, 1L);

        assertEquals(20 - DAY_HUNGER + 3, hunger(amplifier));
        assertTrue(stone.isEmpty(), "smooth stone left uneaten");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aHurtCarriedOneMendsBeforeItFeeds(GameTestHelper helper) {
        ServerPlayer player = awakened(helper, null);
        ItemStack amplifier = hold(player, InteractionHand.MAIN_HAND, ModItems.AMPLIFY_EFFECT_GU_3.get(), 20);
        amplifier.set(ModDataComponents.REFINED_GU_STATE, TendedGuItem.getState(amplifier).withDamageTaken(2));
        ItemStack stone = new ItemStack(Items.SMOOTH_STONE, 3);
        player.getInventory().setItem(9, stone);

        GuUpkeep.tickCarried(player, 1L);

        assertEquals(0, TendedGuItem.getState(amplifier).damageTaken(), "the wound was not mended first");
        assertEquals(20 - DAY_HUNGER + 1, hunger(amplifier));
        assertTrue(stone.isEmpty(), "smooth stone left uneaten");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aRadialPickDrivesAHotbarGuAndBillsTheMainHandAmplifier(GameTestHelper helper) {
        ServerPlayer player = awakened(helper, null);
        ItemStack amplifier = hold(player, InteractionHand.MAIN_HAND, ModItems.AMPLIFY_EFFECT_GU_3.get(), 16);
        ItemStack boar = inSlot(player, 2, ModItems.FLOWER_BOAR_GU_1.get(), 12);
        long essence = ApertureEssenceService.getSpendable(player);

        ClientIntents.amplifyDrive(player, 2);

        assertEquals(FlowerBoarGuEffect.DURATION_TICKS * 3, duration(player, ModEffects.FLOWER_BOAR_GU));
        assertEquals(essence - 40L - 2_560L, ApertureEssenceService.getSpendable(player));
        assertEquals(12 - USE_HUNGER, hunger(boar));
        assertEquals(16 - USE_HUNGER, hunger(amplifier));
        assertTrue(player.getCooldowns().isOnCooldown(boar.getItem()), "the driven Gu took no cooldown");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aRadialPickSpendsAOneShotGuFromItsSlot(GameTestHelper helper) {
        ServerPlayer player = awakened(helper, null);
        hold(player, InteractionHand.MAIN_HAND, ModItems.AMPLIFY_EFFECT_GU_2.get(), 16);
        ItemStack leaves = new ItemStack(ModItems.VITALITY_LEAF_GU_1.get(), 2);
        player.getInventory().setItem(4, leaves);

        ClientIntents.amplifyDrive(player, 4);

        assertEquals(VitalityLeafGuEffect.DURATION_TICKS * 2, duration(player, ModEffects.VITALITY_LEAF_GU));
        assertEquals(1, leaves.getCount());
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aRadialPickAboveItsRankOrShortOfEssenceLandsNothing(GameTestHelper helper) {
        ServerPlayer player = awakened(helper, null);
        PathStrengthService.grant(player, BeastStrength.WHITE_BOAR);
        ItemStack low = hold(player, InteractionHand.MAIN_HAND, ModItems.AMPLIFY_EFFECT_GU_2.get(), 16);
        inSlot(player, 1, ModItems.ALL_OUT_EFFORT_GU_3.get(), 48);
        long essence = ApertureEssenceService.getSpendable(player);

        ClientIntents.amplifyDrive(player, 1);

        assertFalse(player.hasEffect(ModEffects.ALL_OUT_EFFORT_GU), "a higher-rank Gu was driven");
        assertEquals(16, hunger(low));
        assertEquals(essence, ApertureEssenceService.getSpendable(player));

        ItemStack high = hold(player, InteractionHand.MAIN_HAND, ModItems.AMPLIFY_EFFECT_GU_5.get(), 16);
        ApertureEssenceService.consume(player, essence - 1_000L);
        ClientIntents.amplifyDrive(player, 1);

        assertFalse(player.hasEffect(ModEffects.ALL_OUT_EFFORT_GU), "driven without essence for both");
        assertEquals(16, hunger(high));
        assertEquals(1_000L, ApertureEssenceService.getSpendable(player));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aRadialPickOnAnEmptyPoolPaysBothFromCarriedStones(GameTestHelper helper) {
        ServerPlayer player = awakened(helper, null);
        hold(player, InteractionHand.MAIN_HAND, ModItems.AMPLIFY_EFFECT_GU_3.get(), 16);
        inSlot(player, 2, ModItems.FLOWER_BOAR_GU_1.get(), 12);
        ItemStack stones = new ItemStack(ModItems.PRIMEVAL_STONE.get(), 200);
        player.getInventory().setItem(9, stones);
        ApertureEssenceService.consume(player, ApertureEssenceService.getSpendable(player));

        ClientIntents.amplifyDrive(player, 2);

        assertEquals(FlowerBoarGuEffect.DURATION_TICKS * 3, duration(player, ModEffects.FLOWER_BOAR_GU));
        assertTrue(stones.isEmpty(), "the top-up did not pour every stone into the pool");
        assertEquals(200L * 100L - 40L - 2_560L, ApertureEssenceService.getSpendable(player));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aRadialPickNeedsTheAmplifierInHandAndAnotherSlot(GameTestHelper helper) {
        ServerPlayer player = awakened(helper, null);
        ItemStack boar = hold(player, InteractionHand.MAIN_HAND, ModItems.FLOWER_BOAR_GU_1.get(), 12);
        inSlot(player, 3, ModItems.FLOWER_BOAR_GU_1.get(), 12);

        ClientIntents.amplifyDrive(player, 3);
        ClientIntents.amplifyDrive(player, player.getInventory().selected);

        assertFalse(player.hasEffect(ModEffects.FLOWER_BOAR_GU), "driven with no amplifier in hand");
        assertEquals(12, hunger(boar));
        helper.succeed();
    }

    private static ServerPlayer awakened(GameTestHelper helper, @Nullable List<Component> inbox) {
        return GameTestGu.awakened(
                GameTestPlayers.survival(helper, "gzr-amplify-effect-gametest").inbox(inbox).onGround(), Rank.FIVE);
    }

    private static int duration(ServerPlayer player, DeferredHolder<MobEffect, ? extends MobEffect> effect) {
        MobEffectInstance instance = assertNotNull(player.getEffect(effect), () -> effect.getId() + " missing");
        return instance.getDuration();
    }
}
