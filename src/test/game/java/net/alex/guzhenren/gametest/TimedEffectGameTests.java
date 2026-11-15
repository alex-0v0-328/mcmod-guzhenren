package net.alex.guzhenren.gametest;

import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.effect.timed.BruteForceLonghornBeetleGuEffect;
import net.alex.guzhenren.effect.timed.FlowerBoarGuEffect;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.ShieldService;
import net.alex.guzhenren.gameplay.body.ShieldType;
import net.alex.guzhenren.gameplay.body.Skin;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.path.PathEffect;
import net.alex.guzhenren.gameplay.path.PathEffectService;
import net.alex.guzhenren.gameplay.path.PathMarks;
import net.alex.guzhenren.gameplay.path.PathService;
import net.alex.guzhenren.gameplay.path.strength.AttackDamageService;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthData;
import net.alex.guzhenren.gameplay.path.strength.StrengthService;
import net.alex.guzhenren.gameplay.path.timed.TimedBuff;
import net.alex.guzhenren.item.gu.mortal.behavior.TimedEffects;
import net.alex.guzhenren.item.gu.mortal.path.rule.AmplifyEffectGuItem.Amplification;
import net.alex.guzhenren.item.gu.mortal.path.rule.AmplifyEffectGuItem;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.alex.guzhenren.lifecycle.Heartbeat;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.alex.guzhenren.registry.effect.ModPathEffectTypes;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.EffectCures;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.GameTestGu.click;
import static net.alex.guzhenren.gametest.GameTestGu.hold;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertFalse;
import static net.alex.guzhenren.gametest.UnitAsserts.assertNotNull;
import static net.alex.guzhenren.gametest.UnitAsserts.assertNull;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 在线玩家限时蛊肉身效果测试。增益、玉皮、天蓬护盾、生机叶、更蛊形态：催动铺点，结束收回；效果是状态真相，喝奶不清；
 * 再催延长时长，不加点；层数叠加，高档替换低档，移除效果即结束状态；功倍蛊[Amplify Effect Gu]律道点只加一次。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TimedEffectGameTests {
    private TimedEffectGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void aBuffGuLaysItsPointsAndProjectsItsIcon(GameTestHelper helper) {
        ServerPlayer player = awakened(helper);
        ItemStack boar = hold(player, InteractionHand.MAIN_HAND, ModItems.FLOWER_BOAR_GU_1.get(), 12);

        click(helper, player, boar);

        PathEffect<TimedBuff> effect = assertNotNull(PathEffectService.find(player, ModPathEffectTypes.BUFF,
                Guzhenren.id("flower_boar_gu_1")), () -> "no buff effect");
        assertEquals(PathMarks.ofPoints(48L), effect.getMarks(GuPath.STRENGTH));
        assertEquals(FlowerBoarGuEffect.DURATION_TICKS, effect.getTicksLeft(player.level().getGameTime()));
        assertEquals(PathMarks.ofPoints(48L), PathService.getMarks(player, GuPath.STRENGTH));
        assertTrue(player.hasEffect(ModEffects.FLOWER_BOAR_GU), "the icon was not projected");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theBeetleGivesOneOxsStrengthForFortyFiveSeconds(GameTestHelper helper) {
        ServerPlayer player = awakened(helper);
        ItemStack beetle = hold(player, InteractionHand.MAIN_HAND, ModItems.BRUTE_FORCE_LONGHORN_BEETLE_GU_1.get(), 12);

        click(helper, player, beetle);

        MobEffectInstance ox = assertNotNull(player.getEffect(ModEffects.BRUTE_FORCE_LONGHORN_BEETLE_GU),
                () -> "the icon was not projected");
        assertEquals(45L * Ticks.SECOND, ox.getDuration());
        assertEquals(BruteForceLonghornBeetleGuEffect.POINTS, StrengthService.getStrengthPoints(player));
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        assertEquals(StrengthService.getUsablePoints(player) * PathStrengthData.ATTACK_PER_POINT,
                AttackDamageService.getBonus(player));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void noBeastBuffLeavesWeaknessWhenItRunsOut(GameTestHelper helper) {
        ServerPlayer player = awakened(helper);
        for (Holder<MobEffect> buff : List.<Holder<MobEffect>>of(ModEffects.FLOWER_BOAR_GU,
                ModEffects.BRUTE_FORCE_LONGHORN_BEETLE_GU)) {
            player.removeAllEffects();
            MobEffectInstance instance = new MobEffectInstance(buff, 2);
            while (instance.tick(player, () -> {})) {
                assertFalse(player.hasEffect(MobEffects.WEAKNESS), buff.getRegisteredName() + " left weakness");
            }
            assertFalse(player.hasEffect(MobEffects.WEAKNESS), buff.getRegisteredName() + " left weakness");
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void milkCuresNoneOfTheModsEffectsAndStillCuresVanillaOnes(GameTestHelper helper) {
        ServerPlayer player = awakened(helper);
        TimedEffects.grantBuff(player, (TendedGuItem) ModItems.FLOWER_BOAR_GU_1.get(), ModEffects.FLOWER_BOAR_GU, 0,
                600L, Amplification.NONE);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600));

        player.removeEffectsCuredBy(EffectCures.MILK);

        assertTrue(player.hasEffect(ModEffects.FLOWER_BOAR_GU), "milk cured a projected buff");
        assertFalse(player.hasEffect(MobEffects.MOVEMENT_SPEED), "milk spared a vanilla effect");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void whenABuffRunsOutItsEffectAndPointsGoTogether(GameTestHelper helper) {
        ServerPlayer player = awakened(helper);
        TimedEffects.grantBuff(player, (TendedGuItem) ModItems.FLOWER_BOAR_GU_1.get(), ModEffects.FLOWER_BOAR_GU, 0,
                20L, Amplification.NONE);
        PathEffectService.update(player, ModPathEffectTypes.BUFF, Guzhenren.id("flower_boar_gu_1"),
                effect -> effect.withEndTick(player.level().getGameTime()));

        PathEffectService.tick(player);

        assertNull(PathEffectService.find(player, ModPathEffectTypes.BUFF));
        assertEquals(PathMarks.NONE, PathService.getMarks(player, GuPath.STRENGTH));
        assertFalse(player.hasEffect(ModEffects.FLOWER_BOAR_GU), "the icon outlived the effect");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void milkCannotClearABuffTheHeartbeatPutsItBack(GameTestHelper helper) {
        ServerPlayer player = awakened(helper);
        TimedEffects.grantBuff(player, (TendedGuItem) ModItems.CHARGING_CRASH_GU_4.get(),
                ModEffects.CHARGING_CRASH_GU, 3, 600L, Amplification.NONE);

        player.removeAllEffects();
        assertFalse(player.hasEffect(ModEffects.CHARGING_CRASH_GU));
        player.tickCount = Heartbeat.HEARTBEAT_TICKS;
        Heartbeat.beat(player);

        MobEffectInstance icon = assertNotNull(player.getEffect(ModEffects.CHARGING_CRASH_GU), () -> "not put back");
        assertEquals(3L, icon.getAmplifier());
        assertNotNull(PathEffectService.find(player, ModPathEffectTypes.BUFF), () -> "the effect went with the milk");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void usingABuffAgainLengthensItAndAddsNoPoints(GameTestHelper helper) {
        ServerPlayer player = awakened(helper);
        TendedGuItem boar = (TendedGuItem) ModItems.FLOWER_BOAR_GU_1.get();
        TimedEffects.grantBuff(player, boar, ModEffects.FLOWER_BOAR_GU, 0, 200L, Amplification.NONE);
        TimedEffects.grantBuff(player, boar, ModEffects.FLOWER_BOAR_GU, 0, 900L, Amplification.NONE);

        PathEffect<TimedBuff> effect = assertNotNull(PathEffectService.find(player, ModPathEffectTypes.BUFF,
                Guzhenren.id("flower_boar_gu_1")), () -> "no buff effect");
        assertEquals(900L, effect.getTicksLeft(player.level().getGameTime()));
        assertEquals(PathMarks.ofPoints(48L), effect.getMarks(GuPath.STRENGTH));
        assertEquals(1L, PathEffectService.getEffects(player).size());
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aHigherCrashGuTakesTheLowerOnesEffectAndPoints(GameTestHelper helper) {
        ServerPlayer player = awakened(helper);
        TimedEffects.grantBuff(player, (TendedGuItem) ModItems.CHARGING_CRASH_GU_4.get(),
                ModEffects.CHARGING_CRASH_GU, 3, 600L, Amplification.NONE);
        TimedEffects.grantBuff(player, (TendedGuItem) ModItems.CHARGING_CRASH_GU_5.get(),
                ModEffects.CHARGING_CRASH_GU, 4, 600L, Amplification.NONE);

        assertEquals(1L, PathEffectService.getEffects(player).size());
        PathEffect<TimedBuff> effect = PathEffectService.getEffects(player).stream().map(e -> {
            @SuppressWarnings("unchecked") PathEffect<TimedBuff> buff = (PathEffect<TimedBuff>) e;
            return buff;
        }).findFirst().orElseThrow();
        assertEquals(Guzhenren.id("charging_crash_gu_5"), effect.source());
        assertEquals(PathMarks.ofPoints(3_072L), PathService.getMarks(player, GuPath.STRENGTH));
        assertEquals(4L, player.getEffect(ModEffects.CHARGING_CRASH_GU).getAmplifier());
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aWatchFormStacksOneLayerOfPointsPerUseUpToItsLimit(GameTestHelper helper) {
        ServerPlayer player = awakened(helper);
        TendedGuItem watch = (TendedGuItem) ModItems.SECOND_WATCH_GU_4.get();

        for (int use = 1; use <= 3; use++) {
            TimedEffects.grantWatch(player, watch, ModEffects.SECOND_WATCH_GU, 600L, Amplification.NONE);
            long layers = Math.min(use, 2);
            PathEffect<?> effect = PathEffectService.getEffects(player).getFirst();
            assertEquals(layers, effect.layers());
            assertEquals(PathMarks.ofPoints(768L * layers), PathService.getMarks(player, GuPath.TIME));
            assertEquals(layers - 1L, player.getEffect(ModEffects.SECOND_WATCH_GU).getAmplifier());
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aJadeSkinCarriesEarthPointsAndWhiteJadeReplacesThem(GameTestHelper helper) {
        ServerPlayer player = awakened(helper);
        ItemStack jade = hold(player, InteractionHand.MAIN_HAND, ModItems.JADE_SKIN_GU_1.get(), 12);

        click(helper, player, jade);

        PathEffect<?> effect = assertNotNull(PathEffectService.find(player, ModPathEffectTypes.TIMED_SKIN),
                () -> "no skin effect");
        assertEquals(Guzhenren.id("jade_skin_gu_1"), effect.source());
        assertEquals(PathMarks.ofPoints(12L), PathService.getMarks(player, GuPath.EARTH));

        BodyService.wearSkin(player, Skin.WHITE_JADE_SKIN, 90 * Ticks.SECOND);

        assertEquals(1L, PathEffectService.getEffects(player).size());
        assertEquals(Guzhenren.id("white_jade_gu_2"),
                PathEffectService.find(player, ModPathEffectTypes.TIMED_SKIN).source());
        assertEquals(PathMarks.ofPoints(48L), PathService.getMarks(player, GuPath.EARTH));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aSkinThatIsShedTakesItsEffectAndTheOtherWayAround(GameTestHelper helper) {
        ServerPlayer player = awakened(helper);
        BodyService.wearSkin(player, Skin.JADE_SKIN, 600);
        assertNotNull(PathEffectService.find(player, ModPathEffectTypes.TIMED_SKIN), () -> "no skin effect");

        BodyService.shedSkin(player);
        assertNull(PathEffectService.find(player, ModPathEffectTypes.TIMED_SKIN));
        assertEquals(PathMarks.NONE, PathService.getMarks(player, GuPath.EARTH));

        BodyService.wearSkin(player, Skin.JADE_SKIN, 600);
        PathEffectService.removeType(player, ModPathEffectTypes.TIMED_SKIN);
        assertEquals(Skin.NATIVE, BodyService.getSkin(player));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theMugwortShieldCarriesWaterPointsUntilItIsSpentOrRunsOut(GameTestHelper helper) {
        ServerPlayer player = awakened(helper);
        ItemStack mugwort = hold(player, InteractionHand.MAIN_HAND, ModItems.HEAVENLY_MUGWORT_GU_3.get(), 12);

        click(helper, player, mugwort);

        PathEffect<?> effect = assertNotNull(PathEffectService.find(player, ModPathEffectTypes.MUGWORT_SHIELD),
                () -> "no shield effect");
        assertEquals(PathMarks.ofPoints(192L), effect.getMarks(GuPath.WATER));
        assertEquals(30L * Ticks.SECOND, effect.getTicksLeft(player.level().getGameTime()));

        ShieldService.guard(player, new DamageContainer(player.damageSources().generic(), 1_000.0F));
        assertNull(PathEffectService.find(player, ModPathEffectTypes.MUGWORT_SHIELD));
        assertEquals(PathMarks.NONE, PathService.getMarks(player, GuPath.WATER));

        ShieldService.raise(player, ShieldType.HEAVENLY_MUGWORT, 16.0F, 600);
        PathEffectService.removeType(player, ModPathEffectTypes.MUGWORT_SHIELD);
        assertFalse(ShieldService.isUp(player), "the shield outlived its effect");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aVitalityLeafCarriesWoodPointsForItsDuration(GameTestHelper helper) {
        ServerPlayer player = awakened(helper);
        ItemStack leaf = new ItemStack(ModItems.VITALITY_LEAF_GU_1.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, leaf);

        click(helper, player, leaf);

        PathEffect<?> effect = assertNotNull(PathEffectService.find(player, ModPathEffectTypes.VITALITY_LEAF),
                () -> "no leaf effect");
        assertEquals(PathMarks.ofPoints(12L), effect.getMarks(GuPath.WOOD));
        assertTrue(player.hasEffect(ModEffects.VITALITY_LEAF_GU), "the icon was not projected");
        assertTrue(TimedEffects.hasLeaf(player), "the gate would let a second leaf in");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void anAmplifierAddsItsRulePointsToTheEffectOnce(GameTestHelper helper) {
        ServerPlayer player = awakened(helper);
        ItemStack boar = hold(player, InteractionHand.MAIN_HAND, ModItems.FLOWER_BOAR_GU_1.get(), 12);
        hold(player, InteractionHand.OFF_HAND, ModItems.AMPLIFY_EFFECT_GU_3.get(), 16);

        click(helper, player, boar);

        PathEffect<?> effect = PathEffectService.find(player, ModPathEffectTypes.BUFF);
        assertEquals(PathMarks.ofPoints(48L), effect.getMarks(GuPath.STRENGTH));
        assertEquals(PathMarks.ofPoints(192L), effect.getMarks(GuPath.RULE));

        TimedEffects.markAmplified(player, ModPathEffectTypes.BUFF, Guzhenren.id("flower_boar_gu_1"),
                AmplifyEffectGuItem.bill(player,
                        (TendedGuItem) ModItems.FLOWER_BOAR_GU_1.get()));
        assertEquals(PathMarks.ofPoints(192L),
                PathEffectService.find(player, ModPathEffectTypes.BUFF).getMarks(GuPath.RULE));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void removingABuffEndsItWithItsMarksAndIcon(GameTestHelper helper) {
        ServerPlayer player = awakened(helper);
        TendedGuItem boar = (TendedGuItem) ModItems.FLOWER_BOAR_GU_1.get();
        TimedEffects.grantBuff(player, boar, ModEffects.FLOWER_BOAR_GU, 0, 600L, Amplification.NONE);

        assertEquals(PathMarks.ofPoints(48L), PathService.getMarks(player, GuPath.STRENGTH));

        PathEffectService.removeType(player, ModPathEffectTypes.BUFF);
        assertNull(PathEffectService.find(player, ModPathEffectTypes.BUFF));
        assertTrue(PathService.getMarks(player, GuPath.STRENGTH).isEmpty(), "the marks outlived the effect");
        assertFalse(player.hasEffect(ModEffects.FLOWER_BOAR_GU), "the icon outlived the effect");
        helper.succeed();
    }

    private static ServerPlayer awakened(GameTestHelper helper) {
        return GameTestGu.awakened(GameTestPlayers.survival(helper, "gzr-timed-effect-gametest").onGround(), Rank.FIVE);
    }
}
