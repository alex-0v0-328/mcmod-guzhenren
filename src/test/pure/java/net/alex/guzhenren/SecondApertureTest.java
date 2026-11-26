package net.alex.guzhenren;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.util.List;
import net.alex.guzhenren.gameplay.aperture.Aperture;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.aperture.ApertureStatus;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.aperture.Stage;
import net.alex.guzhenren.gameplay.aperture.Talent;
import net.alex.guzhenren.item.gu.mortal.path.earth.PetrifyBehavior;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecondApertureTest {

    @Test
    void apertureStatusHasOnlyNormalAndDead() {
        assertEquals(2, ApertureStatus.values().length);
        assertFalse(java.util.Arrays.stream(ApertureStatus.values()).anyMatch(value -> value.name().equals("STONE")));
    }

    private static Aperture pool(long current, long distilled) {
        return new Aperture(Rank.THREE, Stage.INITIAL, 80, current, null, null, distilled, 0, 0L, 0, false, false,
                false);
    }

    @Test
    @DisplayName("the second aperture lands Grade-A at 8/10, first stage, full pool, no physique")
    void secondOpenedShape() {
        Aperture opened = Aperture.createSecond(Rank.THREE);
        assertEquals(Rank.THREE, opened.rank());
        assertEquals(Stage.INITIAL, opened.stage());
        assertEquals(Aperture.SECOND_BASE, opened.baseEssence());
        assertSame(Talent.FIRST, opened.getTalent());
        assertEquals(opened.getMaxEssence(), opened.currentEssence());
        assertEquals(0L, opened.distilledEssence());
        assertTrue(opened.second());
    }

    @Test
    @DisplayName("a lone second aperture is not awakened; Hope Gu inserts the first one ahead of it")
    void awakeningOrderAndInsertFirst() {
        Aperture second = Aperture.createSecond(Rank.THREE);
        ApertureData lone = new ApertureData(List.of(second));
        assertTrue(lone.hasAperture());
        assertFalse(lone.isAwakened());
        assertEquals(0, lone.getSecondIndex());
        assertEquals(-1, lone.getFirstIndex());

        ApertureData both = lone.insertFirst(Aperture.createOpenedAt(80));
        assertTrue(both.isAwakened());
        assertEquals(0, both.getFirstIndex());
        assertEquals(1, both.getSecondIndex());
        assertSame(second, both.get(1));
        assertFalse(both.getPrimary().second());
    }

    @Test
    @DisplayName("insertFirst falls back to append once the first aperture exists")
    void insertFirstAppendsWhenFirstExists() {
        ApertureData one = new ApertureData(List.of(Aperture.createOpenedAt(80)));
        ApertureData two = one.insertFirst(Aperture.createSecond(Rank.THREE));
        assertFalse(two.get(0).second());
        assertEquals(1, two.getSecondIndex());
    }

    @Test
    @DisplayName("decoding heals a pre-flag save: the entry at position 1 becomes the second aperture")
    void codecHealsLegacyPositions() {
        JsonElement legacy = Aperture.CODEC.encodeStart(JsonOps.INSTANCE, Aperture.createSecond(Rank.THREE))
                .result().orElseThrow();
        ((JsonObject) legacy).remove("second");

        JsonArray list = new JsonArray();
        list.add(Aperture.CODEC.encodeStart(JsonOps.INSTANCE, Aperture.createOpenedAt(80)).result().orElseThrow());
        list.add(legacy);

        ApertureData healed = ApertureData.CODEC.parse(JsonOps.INSTANCE, list).result().orElseThrow();
        assertFalse(healed.get(0).second());
        assertTrue(healed.get(1).second());
    }

    @Test
    @DisplayName("spending walks PRIMARY distilled, then PRIMARY current, then the second aperture")
    void cascadeWalksPrimaryFirst() {
        long[][] plan = ApertureEssenceService.cascadeTake(50L, List.of(pool(100L, 10L), pool(200L, 20L)));
        assertArrayEquals(new long[] { 10L, 30L }, plan[0]);
        assertArrayEquals(new long[] { 0L, 0L }, plan[1]);
    }

    @Test
    @DisplayName("the distilled half rounds UP -- the last distilled point cannot pay for itself twice")
    void distilledRoundsUp() {
        long[][] plan = ApertureEssenceService.cascadeTake(5L, List.of(pool(100L, 10L)));
        assertArrayEquals(new long[] { 3L, 0L }, plan[0]);
    }

    @Test
    @DisplayName("an exhausted PRIMARY spills the remainder into the second aperture")
    void cascadeSpillsIntoSecond() {
        long[][] plan = ApertureEssenceService.cascadeTake(130L, List.of(pool(0L, 0L), pool(100L, 20L)));
        assertArrayEquals(new long[] { 0L, 0L }, plan[0]);
        assertArrayEquals(new long[] { 20L, 90L }, plan[1]);
    }

    @Test
    @DisplayName("a single pool that covers everything never wakes the second aperture")
    void cascadeStopsWhenCovered() {
        long[][] plan = ApertureEssenceService.cascadeTake(30L, List.of(pool(100L, 0L), pool(100L, 0L)));
        assertArrayEquals(new long[] { 0L, 30L }, plan[0]);
        assertArrayEquals(new long[] { 0L, 0L }, plan[1]);
    }

    @Test
    @DisplayName("石窍蛊 target: PRIMARY wins while NORMAL; only DEAD passes it on")
    void stoneTargetTable() {
        assertSame(ApertureData.PRIMARY, PetrifyBehavior.getStoneTarget(
                ApertureStatus.NORMAL, ApertureStatus.NORMAL));
        assertSame(ApertureData.SECOND, PetrifyBehavior.getStoneTarget(
                ApertureStatus.DEAD, ApertureStatus.NORMAL));
        assertSame(PetrifyBehavior.NO_TARGET, PetrifyBehavior.getStoneTarget(
                ApertureStatus.DEAD, ApertureStatus.DEAD));
    }
}
