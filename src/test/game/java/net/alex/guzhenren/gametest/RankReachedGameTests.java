package net.alex.guzhenren.gametest;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Optional;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.advancement.RankReachedTrigger;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishData;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishService;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.aperture.Stage;
import net.alex.guzhenren.gameplay.clock.Beat;
import net.alex.guzhenren.gameplay.clock.TimeFlow;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 境界进度测试。{@code reached_rank} 条件可往返编解码，先比转；修炼写入授予第一空窍[Aperture]当前及以下每档；
 * 命令式写入与第二空窍不授予进度。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RankReachedGameTests {

    private RankReachedGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void conditionsRoundtripWithTheFirstStageOmitted(GameTestHelper helper) {
        RankReachedTrigger.Instance peak = new RankReachedTrigger.Instance(Optional.empty(), Rank.THREE, Stage.PEAK);
        RankReachedTrigger.Instance two = new RankReachedTrigger.Instance(Optional.empty(), Rank.TWO, Stage.LOWEST);
        JsonElement peakJson = RankReachedTrigger.Instance.CODEC.encodeStart(JsonOps.INSTANCE, peak).getOrThrow();
        JsonElement twoJson = RankReachedTrigger.Instance.CODEC.encodeStart(JsonOps.INSTANCE, two).getOrThrow();
        helper.assertValueEqual(peakJson.toString(), "{\"rank\":\"three\",\"stage\":\"peak\"}",
                "peak conditions");
        helper.assertValueEqual(twoJson.toString(), "{\"rank\":\"two\"}", "rank conditions");
        helper.assertValueEqual(
                RankReachedTrigger.Instance.CODEC.parse(JsonOps.INSTANCE, peakJson).getOrThrow(), peak, "peak back");
        helper.assertValueEqual(
                RankReachedTrigger.Instance.CODEC.parse(JsonOps.INSTANCE, twoJson).getOrThrow(), two, "rank back");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void conditionsCompareRankFirst(GameTestHelper helper) {
        RankReachedTrigger.Instance peak = new RankReachedTrigger.Instance(Optional.empty(), Rank.THREE, Stage.PEAK);
        helper.assertTrue(peak.matches(Rank.THREE, Stage.PEAK), "the peak itself failed");
        helper.assertTrue(peak.matches(Rank.FOUR, Stage.INITIAL), "a higher rank failed at its first stage");
        helper.assertTrue(!peak.matches(Rank.THREE, Stage.UPPER), "one stage short passed");
        helper.assertTrue(!peak.matches(Rank.TWO, Stage.PEAK), "a lower rank passed at its peak");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void nourishedPeakAwardsEveryGradeBelow(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survival(helper, "gzr-realm-nourish").build();
        ApertureService.awaken(player);
        ApertureService.setRank(player, Rank.THREE);
        ApertureService.setStage(player, Stage.UPPER);
        helper.assertTrue(!isDone(helper, player, "rank_two"), "a command-style write awarded Rank II");

        ApertureService.set(player, ApertureData.PRIMARY, ApertureService.getAperture(player)
                .withNourishProgress(ApertureNourishData.FULL - 1).withFullEssence());
        ApertureNourishService.start(player, ApertureData.PRIMARY);
        ApertureNourishService.tickNourish(player, new Beat(0L, TimeFlow.NORMAL_RATE, player.getRandom()));
        helper.assertValueEqual(ApertureService.getStage(player), Stage.PEAK, "stage nourished");
        for (String name : List.of("rank_two", "rank_three", "rank_three_peak")) {
            helper.assertTrue(isDone(helper, player, name), name + " not awarded at Rank III Peak");
        }
        helper.assertTrue(!isDone(helper, player, "rank_four"), "Rank IV awarded at Rank III Peak");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void petrifiedConversionAwardsTheNextRank(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survival(helper, "gzr-realm-stone").build();
        ApertureService.awaken(player);
        ApertureService.setRank(player, Rank.THREE);
        ApertureNourishService.petrify(player, ApertureData.PRIMARY);
        helper.assertTrue(isDone(helper, player, "rank_three_peak"), "Stone Aperture Gu's peak not awarded");
        helper.assertTrue(!isDone(helper, player, "rank_four"), "Rank IV awarded on the stone peak");

        helper.assertTrue(ApertureNourishService.convertPetrifiedPressure(player), "conversion refused");
        helper.assertValueEqual(ApertureService.getRank(player), Rank.FOUR, "converted rank");
        helper.assertTrue(isDone(helper, player, "rank_four"), "Rank IV not awarded on conversion");
        helper.assertTrue(!isDone(helper, player, "rank_five"), "Rank V awarded at Rank IV");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void loneSecondApertureAwardsNothing(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survival(helper, "gzr-realm-second").build();
        ApertureService.openSecond(player, Rank.THREE);
        ApertureNourishService.petrify(player, ApertureData.PRIMARY);
        helper.assertValueEqual(ApertureService.getStage(player), Stage.PEAK, "second at peak");
        helper.assertTrue(!isDone(helper, player, "rank_two"), "a second aperture awarded Rank II");
        helper.assertTrue(!isDone(helper, player, "rank_three_peak"), "a second aperture awarded Rank III Peak");
        player.discard();
        helper.succeed();
    }

    private static boolean isDone(GameTestHelper helper, ServerPlayer player, String name) {
        AdvancementHolder holder = player.server.getAdvancements().get(Guzhenren.id(name));
        helper.assertTrue(holder != null, "advancement " + name + " is not loaded");
        return player.getAdvancements().getOrStartProgress(holder).isDone();
    }
}
