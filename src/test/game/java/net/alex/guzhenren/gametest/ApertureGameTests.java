package net.alex.guzhenren.gametest;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.display.InfoModel;
import net.alex.guzhenren.gameplay.aperture.Aperture;
import net.alex.guzhenren.gameplay.aperture.ApertureChangedEvent;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishData;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishService;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.aperture.Stage;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorageService;
import net.alex.guzhenren.gameplay.clock.Beat;
import net.alex.guzhenren.gameplay.clock.TimeFlow;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.item.gu.stack.VitalGuState;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.GameTestApertures.awakenedMortal;
import static net.alex.guzhenren.gametest.GameTestApertures.mortal;

/**
 * 空窍[Aperture]布局与写入：先二后一放入、生命只取第一空窍、等级命令、面板标题行、温养[Nourish]、真元[Essence]级联。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ApertureGameTests {

    private ApertureGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void secondOnlyThenFirstApertureFlow(GameTestHelper helper) {
        ServerPlayer player = mortal(helper);

        ApertureService.openSecond(player, Rank.THREE);
        helper.assertValueEqual(ApertureService.get(player).count(), 1, "second-only aperture count");
        helper.assertTrue(ApertureService.getAperture(player, 0).second(), "lone aperture must be flagged second");
        helper.assertTrue(!ApertureService.isAwakened(player), "second-only holder must not read awakened");
        helper.assertTrue(ApertureService.hasAperture(player), "second-only holder must read hasAperture");

        ItemStack vital = new ItemStack(ModItems.WHITE_BOAR_GU_1.get());
        ApertureStorageService.setVital(player, 0, vital);
        ApertureStorageService.set(player, 0, List.of(new ItemStack(ModItems.WHITE_BOAR_GU_1.get())));
        ApertureNourishService.start(player, 0);
        helper.assertTrue(ApertureNourishService.isCultivating(player), "nourish started on the lone second");

        ApertureService.awaken(player, 80);
        ApertureData data = ApertureService.get(player);
        helper.assertValueEqual(data.count(), 2, "count after Hope Gu inserts the first aperture");
        helper.assertValueEqual(data.getFirstIndex(), 0, "first aperture takes position 0");
        helper.assertValueEqual(data.getSecondIndex(), 1, "second aperture slides to position 1");
        helper.assertTrue(data.get(0).rank() == Rank.ONE, "first aperture rank");
        helper.assertValueEqual(data.get(1).rank(), Rank.THREE, "second aperture kept its rank");
        helper.assertTrue(VitalGuState.getBoundAperture(ApertureStorageService.getVital(player, 1)) == 1,
                "vital gu binding followed the slide");
        helper.assertTrue(ApertureStorageService.getItems(player, 0).isEmpty(), "old store moved off position 0");
        helper.assertValueEqual(ApertureStorageService.getItems(player, 1).size(), 1,
                "store followed the slide");
        helper.assertTrue(ApertureNourishService.isCultivating(player)
                && ApertureNourishService.getTargetIndex(player) == 1, "nourish target followed the slide");

        ApertureService.openSecond(player, Rank.FIVE);
        ApertureData upgraded = ApertureService.get(player);
        helper.assertValueEqual(upgraded.count(), 2, "upgrade keeps two apertures");
        helper.assertValueEqual(upgraded.getSecondIndex(), 1, "upgrade overwrites in place");
        helper.assertValueEqual(upgraded.get(1).rank(), Rank.FIVE, "upgrade lands the gu rank");
        helper.assertValueEqual(upgraded.get(1).primaryPath(), GuPath.STRENGTH,
                "upgrade keeps the bound path");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void healthFollowsFirstApertureOnly(GameTestHelper helper) {
        ServerPlayer player = mortal(helper);

        helper.assertValueEqual(player.getMaxHealth(), 20.0F, "mortal max health");
        ApertureService.openSecond(player, Rank.THREE);
        helper.assertValueEqual(player.getMaxHealth(), 20.0F,
                "lone second aperture keeps mortal health");
        ApertureService.awaken(player, 80);
        helper.assertValueEqual(player.getMaxHealth(), 20.0F, "rank one first aperture keeps 20");
        ApertureService.setRank(player, ApertureData.PRIMARY, Rank.THREE);
        helper.assertValueEqual(player.getMaxHealth(), 60.0F, "first aperture rank three lifts to 60");
        ApertureService.openSecond(player, Rank.FIVE);
        helper.assertValueEqual(player.getMaxHealth(), 60.0F, "second aperture never touches health");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void apertureGradeCommandsSetAndShift(GameTestHelper helper) {
        ServerPlayer player = mortal(helper);
        ApertureService.awaken(player, 80);
        Consumer<String> run = command -> GameTestPlayers.command(player, "gzr aperture " + command);
        Supplier<Aperture> first = () -> ApertureService.getAperture(player,
                ApertureService.get(player).getFirstIndex());

        run.accept("rank set rank:three stage set stage:middle");
        helper.assertValueEqual(first.get().rank(), Rank.THREE, "rank set writes the named rank");
        helper.assertValueEqual(first.get().stage(), Stage.MIDDLE, "chained stage set lands in the same write");
        run.accept("stage up");
        helper.assertValueEqual(first.get().stage(), Stage.UPPER, "stage up shifts one stage");
        helper.assertValueEqual(first.get().rank(), Rank.THREE, "stage alone keeps the rank");
        run.accept("rank up stage down");
        helper.assertValueEqual(first.get().rank(), Rank.FOUR, "rank up shifts one rank");
        helper.assertValueEqual(first.get().stage(), Stage.MIDDLE, "chained stage down shifts one stage");
        run.accept("rank set rank:five");
        run.accept("rank up stage set stage:peak");
        helper.assertValueEqual(first.get().rank(), Rank.FIVE, "rank up clamps at five");
        helper.assertValueEqual(first.get().stage(), Stage.PEAK, "rank alone kept the stage, then set wrote peak");

        ApertureService.openSecond(player, Rank.TWO);
        run.accept("stage set stage:upper second");
        int second = ApertureService.get(player).getSecondIndex();
        helper.assertValueEqual(ApertureService.getAperture(player, second).stage(), Stage.UPPER,
                "second selector writes the second aperture");
        helper.assertValueEqual(first.get().stage(), Stage.PEAK, "second selector leaves the first aperture");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void infoModelAlwaysEmitsApertureTitleRows(GameTestHelper helper) {
        ServerPlayer player = mortal(helper);

        helper.assertValueEqual(titles(player), List.of(new InfoModel.ApertureIndex(1, 0)),
                "mortal keeps the clickable first-aperture title row");
        ApertureService.awaken(player, 80);
        helper.assertValueEqual(titles(player), List.of(new InfoModel.ApertureIndex(1, 0)),
                "first-only holder keeps the clickable title row");
        ApertureService.openSecond(player, Rank.THREE);
        helper.assertValueEqual(titles(player), List.of(new InfoModel.ApertureIndex(1, 0),
                new InfoModel.ApertureIndex(2, 1)), "two apertures keep both clickable title rows");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void emptyApertureCancelsStaleNourishing(GameTestHelper helper) {
        ServerPlayer player = mortal(helper);
        PlayerData.get(player).setNourish(ApertureNourishData.DEFAULT.withCultivating(true));
        helper.assertValueEqual(ApertureNourishService.getTargetIndex(player), ApertureData.PRIMARY,
                "empty aperture has a safe UI target");
        ApertureNourishService.tickNourish(player, new Beat(0L, TimeFlow.NORMAL_RATE, player.getRandom()));
        helper.assertTrue(!ApertureNourishService.isCultivating(player), "stale session canceled without apertures");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void nourishmentRejectsInvalidAndRepeatedStarts(GameTestHelper helper) {
        ServerPlayer player = awakenedMortal(helper);
        for (int index : new int[] { -1, Integer.MIN_VALUE, 1, Integer.MAX_VALUE }) {
            helper.assertTrue(!ApertureNourishService.canNourish(player, index), "invalid target must be rejected");
            ApertureNourishService.start(player, index);
            helper.assertTrue(!ApertureNourishService.isCultivating(player), "invalid start changed session");
        }
        ApertureNourishService.start(player, 0);
        ApertureNourishData session = ApertureNourishService.get(player).withStarvedSinceTick(0L);
        PlayerData.get(player).setNourish(session);
        ApertureNourishService.start(player, 0);
        helper.assertValueEqual(ApertureNourishService.get(player), session,
                "repeated start reset starvation anchor");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void nourishedSecondWritesTheApertureOnce(GameTestHelper helper) {
        ServerPlayer player = awakenedMortal(helper);
        Aperture start = ApertureService.getAperture(player);
        long cost = ApertureNourishService.getCostPerSecond(player, ApertureData.PRIMARY);
        ApertureNourishService.start(player, ApertureData.PRIMARY);
        int writes = countApertureWrites(player, () -> ApertureNourishService.tickNourish(player,
                new Beat(0L, TimeFlow.NORMAL_RATE, player.getRandom())));
        Aperture fed = ApertureService.getAperture(player);
        helper.assertValueEqual(writes, 1, "one billed second writes the aperture once");
        helper.assertValueEqual(fed.nourishProgress(),
                start.nourishProgress() + ApertureNourishService.PERCENT_PER_SECOND, "nourish progress");
        helper.assertValueEqual(fed.currentEssence(), start.currentEssence() - cost, "nourish cost");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void essenceCascadeWritesEachApertureOnce(GameTestHelper helper) {
        ServerPlayer player = awakenedMortal(helper);
        ApertureService.openSecond(player, Rank.ONE);
        Aperture first = ApertureService.getAperture(player, 0).withCurrentEssence(4L).withDistilledEssence(3L);
        ApertureService.set(player, 0, first);
        Aperture second = ApertureService.getAperture(player, 1);

        // 15: three distilled pay 6, the first pool 4, the second pool the last 5
        Aperture[] held = new Aperture[1];
        int deferredWrites = countApertureWrites(player,
                () -> held[0] = ApertureEssenceService.consumeDeferred(player, 15L, 0));
        helper.assertValueEqual(deferredWrites, 1, "only the second aperture is written");
        helper.assertValueEqual(ApertureService.getAperture(player, 0), first,
                "deferred aperture left unwritten");
        helper.assertValueEqual(held[0], first.withDistilledEssence(0L).withCurrentEssence(0L),
                "deferred aperture as the payment leaves it");
        helper.assertValueEqual(ApertureService.getAperture(player, 1).currentEssence(),
                second.currentEssence() - 5L, "second pool pays the rest");

        // 9: three distilled pay 6, the pool 3 -- both land in one write
        int writes = countApertureWrites(player, () -> ApertureEssenceService.consume(player, 9L));
        helper.assertValueEqual(writes, 1, "distilled and pool land in one write");
        helper.assertValueEqual(ApertureService.getAperture(player, 0).distilledEssence(), 0L,
                "distilled paid");
        helper.assertValueEqual(ApertureService.getAperture(player, 0).currentEssence(), 1L,
                "pool paid");
        helper.succeed();
    }

    private static List<InfoModel.ApertureIndex> titles(ServerPlayer player) {
        return InfoModel.buildApertureRows(player).stream().map(InfoModel.Row::entry)
                .filter(InfoModel.ApertureIndex.class::isInstance)
                .map(InfoModel.ApertureIndex.class::cast)
                .toList();
    }

    private static int countApertureWrites(ServerPlayer player, Runnable action) {
        int[] writes = { 0 };
        Consumer<ApertureChangedEvent> count = event -> {
            if (event.getPlayer() == player) writes[0]++;
        };
        GameTestWorld.whileListening(ApertureChangedEvent.class, count, action);
        return writes[0];
    }
}
