package net.alex.guzhenren.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.display.InfoModel;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishService;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.Physique;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.gameplay.path.EffectCarrier;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.path.PathEffect;
import net.alex.guzhenren.gameplay.path.PathEffectCategory;
import net.alex.guzhenren.gameplay.path.PathEffectEndedEvent;
import net.alex.guzhenren.gameplay.path.PathEffectService;
import net.alex.guzhenren.gameplay.path.PathMarks;
import net.alex.guzhenren.gameplay.path.PathService;
import net.alex.guzhenren.gameplay.path.timed.TimedBuff;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.mortal.behavior.StateEffects;
import net.alex.guzhenren.lifecycle.Heartbeat;
import net.alex.guzhenren.lifecycle.PlayerDataService;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.alex.guzhenren.registry.effect.ModPathEffectTypes;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.GameTestPlayers.command;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertFalse;
import static net.alex.guzhenren.gametest.UnitAsserts.assertNotNull;
import static net.alex.guzhenren.gametest.UnitAsserts.assertNull;
import static net.alex.guzhenren.gametest.UnitAsserts.assertSame;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 在线玩家状态蛊效果测试。
 * <ul>
 *   <li>临时僵尸化与僵尸是有害肉身效果，经蛊、体质命令、恢复、点被削都随形态。</li>
 *   <li>酒虫[Liquor Worm]精炼真元以该效果为状态真相；石化空窍[Aperture]是空窍效果；随意蛊[Casual Gu]、
 *       恶念蛊[Malicious Thought Gu]的念头是脑海效果。</li>
 *   <li>各效果按时到期，喝奶不掉，死亡清空；力量不读这些道痕[Path Marks]。</li>
 * </ul>
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StateEffectGameTests {
    private StateEffectGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void aTempZombieCarriesATimedEffectAndTurningZombieDissipatesIt(GameTestHelper helper) {
        ServerPlayer player = survival(helper);
        long now = player.level().getGameTime();

        BodyService.enterTempZombie(player, 1, 1_200);
        PathEffect<Unit> half = PathEffectService.find(player, ModPathEffectTypes.ZOMBIE);
        assertNotNull(half, () -> "a temp zombie laid no effect");
        assertSame(EffectCarrier.BODY, half.carrier());
        assertSame(PathEffectCategory.HARMFUL, half.type().getCategory());
        assertEquals(Guzhenren.id("hairy_zombie_gu_3"), half.source());
        assertEquals(PathMarks.ofPoints(192L), half.getMarks(GuPath.TRANSFORMATION));
        assertEquals(now + 1_200L, half.endTick());

        List<PathEffectEndedEvent> ended = new ArrayList<>();
        GameTestWorld.whileListening(PathEffectEndedEvent.class, event -> {
            if (event.getPlayer() == player) ended.add(event);
        }, () -> BodyService.turnZombie(player, 1));
        assertTrue(BodyService.isZombie(player), "dissipating the points ended the form");
        assertNull(PathEffectService.find(player, ModPathEffectTypes.ZOMBIE));
        assertTrue(PathService.getMarks(player, GuPath.TRANSFORMATION).isEmpty(), "a zombie kept the points");
        assertEquals(1L, ended.size());
        assertSame(PathEffectEndedEvent.Reason.EXPIRED, ended.getFirst().getReason());
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aTempZombieRunningOutOrRecoveringTakesTheEffectWithIt(GameTestHelper helper) {
        ServerPlayer player = survival(helper);
        BodyService.enterTempZombie(player, 0, 1);
        BodyService.removePhysique(player, Physique.TEMP_ZOMBIE);
        assertNull(PathEffectService.find(player, ModPathEffectTypes.ZOMBIE));
        assertTrue(PathService.getMarks(player, GuPath.TRANSFORMATION).isEmpty(), "recovery kept the points");

        BodyService.turnZombie(player, 2);
        assertNull(PathEffectService.find(player, ModPathEffectTypes.ZOMBIE));
        BodyService.removePhysique(player, Physique.ZOMBIE);
        assertFalse(BodyService.isZombie(player), "recovery kept the form");
        assertNull(PathEffectService.find(player, ModPathEffectTypes.ZOMBIE));

        BodyService.enterTempZombie(player, 0, 0);
        player.tickCount = Heartbeat.HEARTBEAT_TICKS;
        Heartbeat.beat(player);
        assertFalse(BodyService.isTempZombie(player), "a run-out temp zombie stayed");
        assertNull(PathEffectService.find(player, ModPathEffectTypes.ZOMBIE));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theBodyCommandLaysTheEffectForATempZombieOnly(GameTestHelper helper) {
        ServerPlayer player = survival(helper);

        command(player, "gzr body physique add zombie");
        assertTrue(BodyService.isZombie(player), "the command set no zombie");
        assertNull(PathEffectService.find(player, ModPathEffectTypes.ZOMBIE));
        command(player, "gzr body physique remove zombie");
        assertNull(PathEffectService.find(player, ModPathEffectTypes.ZOMBIE));

        command(player, "gzr body physique add temp_zombie");
        assertNotNull(PathEffectService.find(player, ModPathEffectTypes.ZOMBIE), () -> "the command laid no half effect");
        command(player, "gzr body physique remove temp_zombie");
        assertNull(PathEffectService.find(player, ModPathEffectTypes.ZOMBIE));
        assertTrue(PathEffectService.getEffects(player).isEmpty());
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void milkCostsTheTempZombieNothingAndCuttingItsPointsEndsTheForm(GameTestHelper helper) {
        ServerPlayer player = survival(helper);
        BodyService.enterTempZombie(player, 0, 1_200);
        player.removeAllEffects();
        assertNotNull(PathEffectService.find(player, ModPathEffectTypes.ZOMBIE), () -> "milk took the zombie effect");
        assertTrue(BodyService.isTempZombie(player));

        PathEffectService.removeType(player, ModPathEffectTypes.ZOMBIE);
        assertFalse(BodyService.isTempZombie(player), "a removed effect left the form");
        assertNull(PathEffectService.find(player, ModPathEffectTypes.ZOMBIE));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void loginLaysTheEffectOfATempZombieSavedWithoutOne(GameTestHelper helper) {
        ServerPlayer player = survival(helper);
        long now = player.level().getGameTime();
        PlayerData.get(player).setBody(BodyService.get(player).with(Physique.TEMP_ZOMBIE)
                .withTempZombieEndTick(now + 1_200L).withZombieTier(3));
        assertNull(PathEffectService.find(player, ModPathEffectTypes.ZOMBIE));

        PlayerDataService.onJoin(player);

        PathEffect<Unit> zombie = PathEffectService.find(player, ModPathEffectTypes.ZOMBIE);
        assertNotNull(zombie, () -> "login lost the temp zombie effect");
        assertEquals(PathMarks.ofPoints(3_072L), zombie.getMarks(GuPath.TRANSFORMATION));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void loginDissipatesTheEffectAZombieWasSavedWith(GameTestHelper helper) {
        ServerPlayer player = survival(helper);
        PlayerData.get(player).setBody(BodyService.get(player).with(Physique.ZOMBIE).withZombieTier(3));
        PathEffectService.apply(player, PathEffect.of(ModPathEffectTypes.ZOMBIE, EffectCarrier.BODY,
                Guzhenren.id("heavenly_demon_zombie_gu_5"), Map.of(GuPath.TRANSFORMATION, PathMarks.ofPoints(768L)),
                PathEffect.PERMANENT, Unit.INSTANCE));

        PlayerDataService.onJoin(player);

        assertTrue(BodyService.isZombie(player), "login ended the form");
        assertNull(PathEffectService.find(player, ModPathEffectTypes.ZOMBIE));
        assertTrue(PathService.getMarks(player, GuPath.TRANSFORMATION).isEmpty(), "login kept the points");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void distillingIsABodyEffectOfADayThatMilkCannotEnd(GameTestHelper helper) {
        ServerPlayer player = survival(helper);
        MortalGuItem gu = (MortalGuItem) ModItems.LIQUOR_WORM_1.get();
        long now = player.level().getGameTime();

        ApertureEssenceService.beginDistilling(player);
        StateEffects.grantDistilling(player, gu, 1, Ticks.DAY);

        PathEffect<Integer> distilling = PathEffectService.find(player, ModPathEffectTypes.LIQUOR_DISTILLING);
        assertNotNull(distilling, () -> "distilling laid no effect");
        assertSame(EffectCarrier.BODY, distilling.carrier());
        assertEquals(PathMarks.ofPoints(12L), distilling.getMarks(GuPath.FOOD));
        assertEquals(now + Ticks.DAY, distilling.endTick());
        assertTrue(ApertureEssenceService.isDistilling(player));
        assertTrue(player.hasEffect(ModEffects.LIQUOR_WORM), "distilling projected no icon");

        player.removeAllEffects();
        assertTrue(ApertureEssenceService.isDistilling(player), "milk ended the distilling");
        PathEffectService.tick(player);
        assertTrue(player.hasEffect(ModEffects.LIQUOR_WORM), "the heartbeat did not put the icon back");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void distillingEndsWithItsEffectAndTheEssenceComesBack(GameTestHelper helper) {
        ServerPlayer player = survival(helper);
        MortalGuItem gu = (MortalGuItem) ModItems.LIQUOR_WORM_1.get();
        ApertureEssenceService.beginDistilling(player);
        StateEffects.grantDistilling(player, gu, 1, 0L);

        player.tickCount = Heartbeat.HEARTBEAT_TICKS;
        Heartbeat.beat(player);

        assertNull(PathEffectService.find(player, ModPathEffectTypes.LIQUOR_DISTILLING));
        assertFalse(ApertureEssenceService.isDistilling(player));
        assertTrue(PathService.getMarks(player, GuPath.FOOD).isEmpty(), "a run-out distilling kept its points");
        assertFalse(ApertureService.getAperture(player).distilling(), "the aperture kept distilling");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void distillingSpentDryStillEndsWithItsEffect(GameTestHelper helper) {
        ServerPlayer player = GameTestGu.awakened(survival(helper), Rank.ONE);
        ApertureEssenceService.beginDistilling(player);
        assertTrue(ApertureService.getAperture(player).distilling(), "distilling flagged no aperture");
        assertEquals(0L, ApertureEssenceService.getDistilledEssence(player));

        ApertureEssenceService.closeDistilling(player);

        assertFalse(ApertureService.getAperture(player).distilling(), "a dry distilling kept its aperture flagged");
        assertTrue(ApertureEssenceService.canDistill(player), "a dry distilling refused the next Liquor Worm");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aPetrifiedApertureCarriesAnApertureEffectUntilItIsConverted(GameTestHelper helper) {
        ServerPlayer player = survival(helper);
        ApertureService.awaken(player, 80);
        ApertureService.setRank(player, Rank.THREE);

        ApertureNourishService.petrify(player, ApertureData.PRIMARY);

        PathEffect<Unit> stone = PathEffectService.find(player, ModPathEffectTypes.STONE_APERTURE);
        assertNotNull(stone, () -> "a petrified aperture laid no effect");
        assertSame(EffectCarrier.APERTURE, stone.carrier());
        assertEquals(Guzhenren.id("stone_aperture_gu_3"), stone.source());
        assertEquals(PathMarks.ofPoints(192L), stone.getMarks(GuPath.EARTH));
        assertTrue(stone.isPermanent(), "a petrified aperture's effect ends");

        player.removeAllEffects();
        assertNotNull(PathEffectService.find(player, ModPathEffectTypes.STONE_APERTURE), () -> "milk took it");

        assertTrue(ApertureNourishService.convertPetrifiedPressure(player));
        assertNull(PathEffectService.find(player, ModPathEffectTypes.STONE_APERTURE));
        assertTrue(PathService.getMarks(player, GuPath.EARTH).isEmpty(), "a converted aperture kept the points");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void casualThoughtsAreAMindEffectOfTenSecondsThatRunsOut(GameTestHelper helper) {
        ServerPlayer player = survival(helper);
        MortalGuItem gu = (MortalGuItem) ModItems.CASUAL_GU_2.get();
        long now = player.level().getGameTime();

        StateEffects.grantThought(player, gu, ModPathEffectTypes.CASUAL_THOUGHT, ModEffects.CASUAL_GU, 1, 200L);

        PathEffect<TimedBuff> casual = PathEffectService.find(player, ModPathEffectTypes.CASUAL_THOUGHT);
        assertNotNull(casual, () -> "casual thoughts laid no effect");
        assertSame(EffectCarrier.MIND, casual.carrier());
        assertEquals(PathMarks.ofPoints(48L), casual.getMarks(GuPath.WISDOM));
        assertEquals(now + 200L, casual.endTick());
        assertTrue(player.hasEffect(ModEffects.CASUAL_GU), "no icon");
        assertEquals(1, player.getEffect(ModEffects.CASUAL_GU).getAmplifier());

        player.removeAllEffects();
        PathEffectService.tick(player);
        assertTrue(player.hasEffect(ModEffects.CASUAL_GU), "milk cost more than a beat");

        List<PathEffectEndedEvent> ended = new ArrayList<>();
        GameTestWorld.whileListening(PathEffectEndedEvent.class, event -> {
            if (event.getPlayer() == player) ended.add(event);
        }, () -> {
            StateEffects.grantThought(player, gu, ModPathEffectTypes.CASUAL_THOUGHT, ModEffects.CASUAL_GU, 1, 0L);
            player.tickCount = Heartbeat.HEARTBEAT_TICKS;
            Heartbeat.beat(player);
        });
        assertNull(PathEffectService.find(player, ModPathEffectTypes.CASUAL_THOUGHT));
        assertFalse(player.hasEffect(ModEffects.CASUAL_GU), "a run-out thought kept its icon");
        assertTrue(PathService.getMarks(player, GuPath.WISDOM).isEmpty(), "a run-out thought kept its points");
        assertSame(PathEffectEndedEvent.Reason.EXPIRED, ended.getLast().getReason());
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aMaliciousGuOfAnotherRankReplacesTheRunningOneAndListsUnderDebuffs(GameTestHelper helper) {
        ServerPlayer player = survival(helper);
        MortalGuItem low = (MortalGuItem) ModItems.MALICIOUS_THOUGHT_GU_2.get();
        MortalGuItem high = (MortalGuItem) ModItems.MALICIOUS_THOUGHT_GU_4.get();

        StateEffects.grantThought(player, low, ModPathEffectTypes.MALICIOUS_THOUGHT,
                ModEffects.MALICIOUS_THOUGHT_GU, 1, 240L);
        StateEffects.grantThought(player, high, ModPathEffectTypes.MALICIOUS_THOUGHT,
                ModEffects.MALICIOUS_THOUGHT_GU, 3, 240L);

        PathEffect<TimedBuff> malicious = PathEffectService.find(player, ModPathEffectTypes.MALICIOUS_THOUGHT);
        assertNotNull(malicious, () -> "no malicious thought effect");
        assertEquals(Guzhenren.id("malicious_thought_gu_4"), malicious.source());
        assertEquals(PathMarks.ofPoints(768L), PathService.getMarks(player, GuPath.WISDOM));
        assertEquals(1L, PathEffectService.getEffects(player).size());

        List<InfoModel.Entry> rows = InfoModel.buildPathRows(player).stream().map(InfoModel.Row::entry).toList();
        int header = rows.indexOf(new InfoModel.EffectCategoryHeader(PathEffectCategory.HARMFUL));
        assertTrue(header >= 0, () -> "no path debuff section: " + rows);
        assertEquals(new InfoModel.EffectCarrierRow(EffectCarrier.MIND), rows.get(header + 1));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aWipingDeathClearsEveryStateEffect(GameTestHelper helper) {
        ServerPlayer player = survival(helper);
        BodyService.enterTempZombie(player, 0, 1_200);
        StateEffects.grantThought(player, (MortalGuItem) ModItems.CASUAL_GU_1.get(),
                ModPathEffectTypes.CASUAL_THOUGHT, ModEffects.CASUAL_GU, 0, 200L);
        assertEquals(2L, PathEffectService.getEffects(player).size());

        PlayerDataService.resetAll(player);

        assertTrue(PathEffectService.getEffects(player).isEmpty(), "a wiping death kept a state effect");
        assertFalse(BodyService.isTempZombie(player), "a wiping death kept the form");
        helper.succeed();
    }

    private static ServerPlayer survival(GameTestHelper helper) {
        return GameTestPlayers.survival(helper, "gzr-state-effect-gametest").onGround();
    }
}
