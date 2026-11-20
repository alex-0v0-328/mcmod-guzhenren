package net.alex.guzhenren.gametest;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.display.InfoModel;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.alex.guzhenren.gameplay.body.Race;
import net.alex.guzhenren.gameplay.path.EffectCarrier;
import net.alex.guzhenren.gameplay.path.GuAttainment;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.path.PathEffect;
import net.alex.guzhenren.gameplay.path.PathEffectCategory;
import net.alex.guzhenren.gameplay.path.PathEffectEndedEvent;
import net.alex.guzhenren.gameplay.path.PathEffectService;
import net.alex.guzhenren.gameplay.path.PathEffectType;
import net.alex.guzhenren.gameplay.path.PathMarks;
import net.alex.guzhenren.gameplay.path.PathService;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import net.alex.guzhenren.gameplay.path.strength.HumanStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.gameplay.path.strength.StrengthPathBranch;
import net.alex.guzhenren.item.gu.mortal.behavior.TimedEffects;
import net.alex.guzhenren.item.gu.mortal.path.rule.AmplifyEffectGuItem.Amplification;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.alex.guzhenren.lifecycle.Heartbeat;
import net.alex.guzhenren.lifecycle.PlayerDataService;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.alex.guzhenren.registry.effect.ModPathEffectTypes;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.GameTestPlayers.CENTER;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertFalse;
import static net.alex.guzhenren.gametest.UnitAsserts.assertNotNull;
import static net.alex.guzhenren.gametest.UnitAsserts.assertNull;
import static net.alex.guzhenren.gametest.UnitAsserts.assertSame;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 在线玩家流派效果测试。
 * <ul>
 *   <li>限时效果随心跳到期；道痕[Path Marks]部分扣除与清零；牛奶与 {@code /effect clear} 只去图标。</li>
 *   <li>种族天赋、十绝体天赋作为效果；登录补回缺失天赋，清数据的死亡清空效果存储。</li>
 *   <li>B 面板各行，棕熊本力以自身名占熊位；带图标的测试类型不注册，不进存档。</li>
 * </ul>
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PathEffectGameTests {
    private PathEffectGameTests() {}

    private static final PathEffectType<Unit> ICONED = new PathEffectType<>(Guzhenren.id("test_iconed"),
            MapCodec.unit(Unit.INSTANCE), StreamCodec.unit(Unit.INSTANCE), PathEffectCategory.BENEFICIAL, false,
            () -> ModEffects.ALL_OUT_EFFORT_GU, effect -> Component.literal("test"));
    private static final PathEffectType<Unit> HARMFUL = new PathEffectType<>(Guzhenren.id("test_harmful"),
            MapCodec.unit(Unit.INSTANCE), StreamCodec.unit(Unit.INSTANCE), PathEffectCategory.HARMFUL, false, null,
            effect -> Component.literal("harm"));
    private static final ResourceLocation SOURCE = Guzhenren.id("all_out_effort_gu_3");

    @GameTest(template = "empty9x9x9")
    public static void aTimedEffectRunsOutOnTheHeartbeatWithItsPoints(GameTestHelper helper) {
        ServerPlayer player = survival(helper);
        long now = player.level().getGameTime();
        List<PathEffectEndedEvent> ended = new ArrayList<>();

        GameTestWorld.whileListening(PathEffectEndedEvent.class, event -> {
            if (event.getPlayer() == player) ended.add(event);
        }, () -> {
            PathEffectService.apply(player, iconed(Map.of(GuPath.STRENGTH, PathMarks.ofPoints(48L)), now));
            assertEquals(PathMarks.ofPoints(48L), PathService.getMarks(player, GuPath.STRENGTH));
            player.tickCount = Heartbeat.HEARTBEAT_TICKS;
            Heartbeat.beat(player);
        });

        assertNull(PathEffectService.find(player, ICONED));
        assertTrue(PathService.getMarks(player, GuPath.STRENGTH).isEmpty(), "a run-out effect kept its marks");
        assertFalse(player.hasEffect(ModEffects.ALL_OUT_EFFORT_GU), "a run-out effect kept its icon");
        assertEquals(1L, ended.size());
        assertSame(PathEffectEndedEvent.Reason.EXPIRED, ended.getFirst().getReason());
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aRemovedEffectTakesItsMarksAndIconOnEveryPath(GameTestHelper helper) {
        ServerPlayer player = survival(helper);
        PathEffectService.apply(player, iconed(Map.of(GuPath.STRENGTH, new PathMarks(1L, 48L),
                GuPath.RULE, PathMarks.ofPoints(3L)), PathEffect.PERMANENT));

        PathEffect<Unit> worn = PathEffectService.find(player, ICONED, SOURCE);
        assertNotNull(worn, () -> "the effect was not put on");
        assertEquals(new PathMarks(1L, 48L), PathService.getMarks(player, GuPath.STRENGTH));
        assertTrue(player.hasEffect(ModEffects.ALL_OUT_EFFORT_GU), "the effect projected no icon");

        PathEffectService.removeType(player, ICONED);
        assertNull(PathEffectService.find(player, ICONED));
        assertTrue(PathService.getMarks(player, GuPath.STRENGTH).isEmpty(), "a removed effect kept its marks");
        assertTrue(PathService.getMarks(player, GuPath.RULE).isEmpty(), "a removed effect kept its marks");
        assertFalse(player.hasEffect(ModEffects.ALL_OUT_EFFORT_GU), "a removed effect kept its icon");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void milkCostsTheIconForABeatAndNeverTheEffect(GameTestHelper helper) {
        ServerPlayer player = survival(helper);
        PathEffectService.apply(player, iconed(Map.of(GuPath.STRENGTH, PathMarks.ofPoints(48L)), PathEffect.PERMANENT));
        assertTrue(player.hasEffect(ModEffects.ALL_OUT_EFFORT_GU), "applying did not project the icon at once");

        player.removeAllEffects();
        assertFalse(player.hasEffect(ModEffects.ALL_OUT_EFFORT_GU));
        assertNotNull(PathEffectService.find(player, ICONED), () -> "milk took the path effect");
        assertEquals(PathMarks.ofPoints(48L), PathService.getMarks(player, GuPath.STRENGTH));

        PathEffectService.tick(player);
        assertTrue(player.hasEffect(ModEffects.ALL_OUT_EFFORT_GU), "the heartbeat did not put the icon back");
        assertTrue(player.getEffect(ModEffects.ALL_OUT_EFFORT_GU).isInfiniteDuration());

        assertEquals(1L, PathEffectService.removeType(player, ICONED));
        assertFalse(player.hasEffect(ModEffects.ALL_OUT_EFFORT_GU), "a removed effect kept its icon");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aRaceTalentIsABodyEffectReplacedWithTheRace(GameTestHelper helper) {
        ServerPlayer player = survival(helper);

        BodyService.setRace(player, Race.DRAGONMEN);
        PathEffect<Race> talent = PathEffectService.find(player, ModPathEffectTypes.RACE_TALENT);
        assertNotNull(talent, () -> "the race granted no talent effect");
        assertSame(EffectCarrier.BODY, talent.carrier());
        assertEquals(PathMarks.ofMarks(10L), PathService.getMarks(player, GuPath.ENSLAVEMENT));
        assertSame(GuAttainment.ORDINARY, PathService.getAttainment(player, GuPath.ENSLAVEMENT));

        BodyService.setRace(player, Race.MERMEN);
        assertTrue(PathService.getMarks(player, GuPath.ENSLAVEMENT).isEmpty(), "the old race kept its marks");
        assertSame(GuAttainment.NONE, PathService.getAttainment(player, GuPath.ENSLAVEMENT));
        assertEquals(PathMarks.ofMarks(10L), PathService.getMarks(player, GuPath.WATER));
        assertEquals(1L, PathEffectService.getEffects(player).size());

        BodyService.setRace(player, Race.HUMAN);
        assertTrue(PathEffectService.getEffects(player).isEmpty(), "a human kept a race talent");
        assertSame(GuAttainment.NONE, PathService.getAttainment(player, GuPath.WATER));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aTenExtremesTalentIsAnApertureEffectSplitAcrossItsPaths(GameTestHelper helper) {
        ServerPlayer player = survival(helper);

        BodyService.setExtremePhysique(player, ExtremePhysique.NORTHERN_DARK_ICE_SOUL);
        PathEffect<ExtremePhysique> talent = PathEffectService.find(player, ModPathEffectTypes.EXTREME_PHYSIQUE);
        assertNotNull(talent, () -> "the physique granted no talent effect");
        assertSame(EffectCarrier.APERTURE, talent.carrier());
        assertEquals(PathMarks.ofMarks(600L), PathService.getMarks(player, GuPath.ICE_SNOW));
        assertEquals(PathMarks.ofMarks(600L), PathService.getMarks(player, GuPath.SOUL));

        BodyService.setExtremePhysique(player, ExtremePhysique.VERDANT_GREAT_SUN);
        assertTrue(PathService.getMarks(player, GuPath.ICE_SNOW).isEmpty(), "the old physique kept its marks");
        assertEquals(PathMarks.ofMarks(1_200L), PathService.getMarks(player, GuPath.SPACE));

        BodyService.setExtremePhysique(player, ExtremePhysique.NONE);
        assertTrue(PathEffectService.getEffects(player).isEmpty(), "a cleared physique kept its talent");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void loginPutsAMissingTalentBackAndKeepsAHeldOne(GameTestHelper helper) {
        ServerPlayer player = survival(helper);
        BodyService.setRace(player, Race.DRAGONMEN);
        BodyService.setExtremePhysique(player, ExtremePhysique.VERDANT_GREAT_SUN);
        PathEffectService.removeType(player, ModPathEffectTypes.EXTREME_PHYSIQUE);

        PlayerDataService.onJoin(player);

        assertEquals(PathMarks.ofMarks(10L), PathService.getMarks(player, GuPath.ENSLAVEMENT));
        assertEquals(PathMarks.ofMarks(1_200L), PathService.getMarks(player, GuPath.SPACE));
        assertSame(GuAttainment.ORDINARY, PathService.getAttainment(player, GuPath.ENSLAVEMENT));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aWipingDeathClearsEveryEffectAndPool(GameTestHelper helper) {
        ServerPlayer player = survival(helper);
        BodyService.setRace(player, Race.DRAGONMEN);
        PathService.setBarePoints(player, GuPath.STRENGTH, 7L);
        PathService.setBareMarks(player, GuPath.STRENGTH, 2L);
        PathEffectService.apply(player, iconed(Map.of(GuPath.STRENGTH, PathMarks.ofPoints(48L)), PathEffect.PERMANENT));

        PlayerDataService.resetAll(player);

        assertTrue(PathEffectService.getEffects(player).isEmpty(), "a wiping death kept a path effect");
        assertTrue(PathService.getMarks(player, GuPath.STRENGTH).isEmpty(), "a wiping death kept bare marks");
        assertTrue(PathService.getMarks(player, GuPath.ENSLAVEMENT).isEmpty(), "a wiping death kept a race talent");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void thePathsTabTotalsTheMarksThenListsEffectsByCarrier(GameTestHelper helper) {
        ServerPlayer player = survival(helper);
        assertEquals(List.of(new InfoModel.Row(0, new InfoModel.PathsHeader(true)),
                new InfoModel.Row(0, new InfoModel.EffectsHeader(true))), InfoModel.buildPathRows(player));
        BodyService.setRace(player, Race.DRAGONMEN);
        PathService.setBarePoints(player, GuPath.ENSLAVEMENT, 5L);
        PathService.setBareMarks(player, GuPath.ENSLAVEMENT, 2L);
        BodyService.setExtremePhysique(player, ExtremePhysique.VERDANT_GREAT_SUN);

        List<InfoModel.Entry> rows = InfoModel.buildPathRows(player).stream().map(InfoModel.Row::entry).toList();

        assertEquals(List.of(new InfoModel.PathsHeader(false),
                new InfoModel.PathRow(GuPath.SPACE, GuAttainment.NONE, PathMarks.ofMarks(1_200L)),
                new InfoModel.PathRow(GuPath.ENSLAVEMENT, GuAttainment.ORDINARY, new PathMarks(12L, 5L)),
                new InfoModel.EffectsHeader(false),
                new InfoModel.EffectCategoryHeader(PathEffectCategory.BENEFICIAL),
                new InfoModel.EffectCarrierRow(EffectCarrier.APERTURE),
                new InfoModel.PathEffectRow(ApertureService.getTalentEffect(ExtremePhysique.VERDANT_GREAT_SUN),
                        PathEffect.PERMANENT),
                new InfoModel.EffectCarrierRow(EffectCarrier.BODY),
                new InfoModel.PathEffectRow(BodyService.getRaceTalent(Race.DRAGONMEN), PathEffect.PERMANENT)), rows);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void strengthsReadUnderStrengthAttainmentThreeToARowAndNeverAsPathBuffs(GameTestHelper helper) {
        // Mark Interference keeps the two branches on separate bodies.
        ServerPlayer beast = survival(helper);
        PathStrengthService.grant(beast, BeastStrength.WHITE_BOAR);
        PathStrengthService.grant(beast, BeastStrength.BLACK_BOAR);
        PathStrengthService.grant(beast, BeastStrength.BEAR);
        TimedEffects.grantBuff(beast, (TendedGuItem) ModItems.BRUTE_FORCE_LONGHORN_BEETLE_GU_1.get(),
                ModEffects.BRUTE_FORCE_LONGHORN_BEETLE_GU, 0, 40L * Ticks.SECOND, Amplification.NONE);
        TimedEffects.grantBuff(beast, (TendedGuItem) ModItems.FLOWER_BOAR_GU_1.get(), ModEffects.FLOWER_BOAR_GU, 0,
                18L * Ticks.SECOND, Amplification.NONE);
        assertStrengthRows(beast, List.of(new InfoModel.StrengthPathAchieveHeader(),
                new InfoModel.StrengthPathBranchRow(StrengthPathBranch.BEAST_STRENGTH_PHANTOM, 0),
                new InfoModel.StrengthReadingRow(List.of(
                        new InfoModel.BeastReading(BeastStrengthFamily.BOAR, 2, null, PathEffect.PERMANENT),
                        new InfoModel.BeastReading(BeastStrengthFamily.BOAR, 1, null, 18L),
                        new InfoModel.BeastReading(BeastStrengthFamily.OX, 1, null, 40L))),
                new InfoModel.StrengthReadingRow(List.of(
                        new InfoModel.BeastReading(BeastStrengthFamily.BEAR, 1, null, PathEffect.PERMANENT)))),
                new InfoModel.StrengthPathBranchRow(StrengthPathBranch.HUMAN_JUN_STRENGTH, 0));

        ServerPlayer human = GameTestPlayers.survival(helper, "gzr-path-effect-human-gametest").at(CENTER);
        PathStrengthService.setHumanStrength(human, HumanStrength.JIN, 9);
        PathStrengthService.setHumanStrength(human, HumanStrength.TEN_JIN, 9);
        PathStrengthService.setHumanStrength(human, HumanStrength.JUN, 30);
        PathStrengthService.setHumanStrength(human, HumanStrength.TEN_JUN, 30);
        PathStrengthService.setHumanStrength(human, HumanStrength.HUNDRED_JUN, 30);
        assertStrengthRows(human, List.of(new InfoModel.StrengthPathAchieveHeader(),
                new InfoModel.StrengthPathBranchRow(StrengthPathBranch.HUMAN_JUN_STRENGTH, 99_999),
                new InfoModel.StrengthReadingRow(List.of(new InfoModel.HumanReading(HumanStrength.JIN, 99),
                        new InfoModel.HumanReading(HumanStrength.JUN, 3_330)))),
                new InfoModel.StrengthPathBranchRow(StrengthPathBranch.BEAST_STRENGTH_PHANTOM, 0));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aBrownBearReadsAsItsInnateStrengthInTheBearsPlace(GameTestHelper helper) {
        ServerPlayer player = survival(helper);
        PathStrengthService.grant(player, BeastStrength.BEAR);
        PathStrengthService.grant(player, BeastStrength.BROWN_BEAR);
        assertStrengthRows(player, List.of(new InfoModel.StrengthPathAchieveHeader(),
                new InfoModel.StrengthPathBranchRow(StrengthPathBranch.BEAST_STRENGTH_PHANTOM, 0),
                new InfoModel.StrengthReadingRow(List.of(new InfoModel.BeastReading(BeastStrengthFamily.BEAR, 1,
                        BeastStrength.BROWN_BEAR, PathEffect.PERMANENT)))),
                new InfoModel.StrengthPathBranchRow(StrengthPathBranch.HUMAN_JUN_STRENGTH, 0));
        helper.succeed();
    }

    private static void assertStrengthRows(ServerPlayer player, List<InfoModel.Entry> expected,
                                           InfoModel.Entry absent) {
        List<InfoModel.Entry> rows = InfoModel.buildPathRows(player).stream().map(InfoModel.Row::entry).toList();

        int header = rows.indexOf(new InfoModel.StrengthPathAchieveHeader());
        assertTrue(header >= 0, () -> "no strength attainment: " + rows);
        assertEquals(expected, rows.subList(header, Math.min(rows.size(), header + expected.size())));
        assertFalse(rows.contains(absent), "the other branch was listed: " + absent);
        assertTrue(rows.stream().noneMatch(InfoModel.PathEffectRow.class::isInstance),
                () -> "a strength also listed as a path buff: " + rows);
    }

    @GameTest(template = "empty9x9x9")
    public static void aHarmfulEffectListsUnderPathDebuffs(GameTestHelper helper) {
        ServerPlayer player = survival(helper);
        PathEffectService.apply(player, PathEffect.of(HARMFUL, EffectCarrier.MIND, SOURCE,
                Map.of(GuPath.WISDOM, PathMarks.ofPoints(3L)), PathEffect.PERMANENT, Unit.INSTANCE));

        List<InfoModel.Entry> rows = InfoModel.buildPathRows(player).stream().map(InfoModel.Row::entry).toList();

        assertFalse(rows.contains(new InfoModel.EffectCategoryHeader(PathEffectCategory.BENEFICIAL)));
        int header = rows.indexOf(new InfoModel.EffectCategoryHeader(PathEffectCategory.HARMFUL));
        assertTrue(header >= 0, () -> "no path debuff section: " + rows);
        assertEquals(new InfoModel.EffectCarrierRow(EffectCarrier.MIND), rows.get(header + 1));
        helper.succeed();
    }

    private static PathEffect<Unit> iconed(Map<GuPath, PathMarks> paths, long endTick) {
        return PathEffect.of(ICONED, EffectCarrier.BODY, SOURCE, paths, endTick, Unit.INSTANCE);
    }

    private static ServerPlayer survival(GameTestHelper helper) {
        return GameTestPlayers.survival(helper, "gzr-path-effect-gametest").onGround();
    }
}
