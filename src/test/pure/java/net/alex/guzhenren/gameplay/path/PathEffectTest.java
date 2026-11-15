package net.alex.guzhenren.gameplay.path;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import io.netty.handler.codec.DecoderException;
import java.util.List;
import java.util.Map;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.display.ModDisplayText;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.alex.guzhenren.gameplay.body.Race;
import net.alex.guzhenren.registry.effect.ModPathEffectTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static net.alex.guzhenren.TestBuffers.withBuffer;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 流派效果记账：条与点两计数及读法；记录在层数、时间、削减下的道痕[Path Marks]；存储单槽与不留空规则；两种编解码器；种
 * 族与体质的两个先天效果；活玩家生命周期见 {@code PathEffectGameTests}。
 */

class PathEffectTest {

    private static final PathEffectType<Unit> SHARED = new PathEffectType<>(Guzhenren.id("test_shared"),
            MapCodec.unit(Unit.INSTANCE), StreamCodec.unit(Unit.INSTANCE), PathEffectCategory.BENEFICIAL, false, null,
            effect -> Component.empty());
    private static final ResourceLocation FIRST = Guzhenren.id("first_gu");
    private static final ResourceLocation SECOND = Guzhenren.id("second_gu");

    @Test
    @DisplayName("whole marks and points are two counters that never convert, each saturating and flooring alone")
    void countersStayApart() {
        PathMarks points = PathMarks.ofPoints(10_000L).add(PathMarks.ofPoints(10_000L));
        assertEquals(new PathMarks(0L, 20_000L), points);
        assertEquals(new PathMarks(10L, 3L), PathMarks.ofMarks(10L).add(PathMarks.ofPoints(3L)));
        assertEquals(new PathMarks(Long.MAX_VALUE, 1L), PathMarks.ofMarks(Long.MAX_VALUE).add(new PathMarks(5L, 1L)));
        assertEquals(new PathMarks(0L, 7L), new PathMarks(2L, 9L).minus(new PathMarks(5L, 2L)));
        assertTrue(new PathMarks(-3L, -4L).isEmpty());
    }

    @Test
    @DisplayName("a count reads as marks and points with thousands separators, dropping whichever is zero")
    void marksReadWithoutTheZeroCount() {
        assertKey("guzhenren.display.marks.both", ModDisplayText.formatMarks(new PathMarks(10L, 3L)), "10", "3");
        assertKey("guzhenren.display.marks.points", ModDisplayText.formatMarks(PathMarks.ofPoints(12_345L)),
                "12,345");
        assertKey("guzhenren.display.marks.marks", ModDisplayText.formatMarks(PathMarks.ofMarks(1_000L)), "1,000");
    }

    @Test
    @DisplayName("an effect drops paths with no marks, and one with no path left is empty")
    void emptyPathsArePruned() {
        PathEffect<Unit> effect = shared(FIRST, Map.of(GuPath.STRENGTH, PathMarks.ofPoints(12L),
                GuPath.RULE, PathMarks.NONE));
        assertEquals(Map.of(GuPath.STRENGTH, PathMarks.ofPoints(12L)), effect.paths());
        assertFalse(effect.isEmpty());
        assertTrue(shared(FIRST, Map.of()).isEmpty());
    }

    @Test
    @DisplayName("a longer time adds no marks, a layer adds its marks, and amplifier points ride on the effect")
    void timeLayersAndAddedPaths() {
        PathEffect<Unit> effect = shared(FIRST, Map.of(GuPath.STRENGTH, PathMarks.ofPoints(12L))).withEndTick(400L);
        PathEffect<Unit> extended = effect.withEndTick(800L);
        assertEquals(effect.paths(), extended.paths());
        assertEquals(800L, extended.endTick());

        Map<GuPath, PathMarks> layer = Map.of(GuPath.STRENGTH, PathMarks.ofPoints(12L));
        PathEffect<Unit> layered = effect.addLayer(layer).addLayer(layer);
        assertEquals(3, layered.layers());
        assertEquals(PathMarks.ofPoints(36L), layered.getMarks(GuPath.STRENGTH));

        PathEffect<Unit> amplified = layered.addMarks(Map.of(GuPath.RULE, PathMarks.ofPoints(3L)));
        assertEquals(PathMarks.ofPoints(36L), amplified.getMarks(GuPath.STRENGTH));
        assertEquals(PathMarks.ofPoints(3L), amplified.getMarks(GuPath.RULE));
        assertEquals(PathMarks.ofPoints(39L), amplified.getTotal());
    }

    @Test
    @DisplayName("a dropped layer takes that layer's marks with it and leaves marks added by other means")
    void dropsALayerWithItsMarks() {
        Map<GuPath, PathMarks> layer = Map.of(GuPath.STRENGTH, PathMarks.ofPoints(12L));
        PathEffect<Unit> effect = shared(FIRST, layer).addLayer(layer)
                .addMarks(Map.of(GuPath.RULE, PathMarks.ofPoints(3L)));

        PathEffect<Unit> dropped = effect.dropLayer(layer);
        assertEquals(1, dropped.layers());
        assertEquals(PathMarks.ofPoints(12L), dropped.getMarks(GuPath.STRENGTH));
        assertEquals(PathMarks.ofPoints(3L), dropped.getMarks(GuPath.RULE));
        assertEquals(effect.payload(), dropped.payload());
    }

    @Test
    @DisplayName("time runs out at the end tick, and a permanent effect never does")
    void runningOut() {
        PathEffect<Unit> timed = shared(FIRST, Map.of(GuPath.STRENGTH, PathMarks.ofPoints(1L))).withEndTick(100L);
        assertFalse(timed.hasRunOut(99L));
        assertTrue(timed.hasRunOut(100L));
        assertEquals(30L, timed.getTicksLeft(70L));
        PathEffect<Unit> permanent = shared(FIRST, Map.of(GuPath.STRENGTH, PathMarks.ofPoints(1L)));
        assertFalse(permanent.hasRunOut(Long.MAX_VALUE));
        assertTrue(permanent.isPermanent());
    }

    @Test
    @DisplayName("an exclusive type is one slot whatever the source; any other type is one slot per source")
    void slots() {
        PathEffect<Race> dragon = BodyService.getRaceTalent(Race.DRAGONMEN);
        PathEffect<Race> other = PathEffect.of(ModPathEffectTypes.RACE_TALENT, EffectCarrier.BODY, SECOND,
                Map.of(GuPath.WATER, PathMarks.ofMarks(1L)), PathEffect.PERMANENT, Race.MERMEN);
        PathEffect<Unit> first = shared(FIRST, Map.of(GuPath.STRENGTH, PathMarks.ofPoints(1L)));
        assertTrue(dragon.sharesSlot(other));
        assertTrue(first.sharesSlot(shared(FIRST, Map.of(GuPath.FIRE, PathMarks.ofPoints(2L)))));
        assertFalse(first.sharesSlot(shared(SECOND, Map.of(GuPath.FIRE, PathMarks.ofPoints(2L)))));
        assertFalse(dragon.sharesSlot(first));
    }

    @Test
    @DisplayName("the store drops an effect with no marks and keeps the later of two in one slot")
    void storeKeepsOneLiveEffectPerSlot() {
        PathEffect<Unit> first = shared(FIRST, Map.of(GuPath.STRENGTH, PathMarks.ofPoints(12L)));
        PathEffect<Unit> again = shared(FIRST, Map.of(GuPath.STRENGTH, PathMarks.ofPoints(48L)));
        PathEffect<Unit> second = shared(SECOND, Map.of(GuPath.STRENGTH, PathMarks.ofMarks(1L),
                GuPath.RULE, PathMarks.ofPoints(3L)));
        PathEffectData data = new PathEffectData(List.of(first, second, shared(SECOND, Map.of()), again));

        assertEquals(List.of(second, again), data.effects());
        assertEquals(new PathMarks(1L, 48L), data.getMarks(GuPath.STRENGTH));
        assertEquals(PathMarks.ofPoints(3L), data.getMarks(GuPath.RULE));
        assertSame(again, data.find(SHARED, FIRST));
        assertNull(data.find(ModPathEffectTypes.RACE_TALENT, null));
        assertEquals(List.of(again), data.without(effect -> effect.source().equals(SECOND)).effects());
    }

    @Test
    @DisplayName("a race's talent is a permanent body effect carrying ten whole marks on the race's path")
    void raceTalentEffect() {
        PathEffect<Race> talent = BodyService.getRaceTalent(Race.DRAGONMEN);
        assertSame(ModPathEffectTypes.RACE_TALENT, talent.type());
        assertSame(EffectCarrier.BODY, talent.carrier());
        assertEquals(BodyService.RACE_TALENT_SOURCE, talent.source());
        assertEquals(Map.of(GuPath.ENSLAVEMENT, PathMarks.ofMarks(10L)), talent.paths());
        assertTrue(talent.isPermanent());
        assertSame(Race.DRAGONMEN, talent.payload());
        assertTrue(BodyService.getRaceTalent(Race.HUMAN).isEmpty());
    }

    @Test
    @DisplayName("a physique's talent is a permanent aperture effect, a thousand whole marks split across its paths")
    void physiqueTalentEffect() {
        PathEffect<ExtremePhysique> single = ApertureService.getTalentEffect(ExtremePhysique.VERDANT_GREAT_SUN);
        assertSame(EffectCarrier.APERTURE, single.carrier());
        assertEquals(PathMarks.ofMarks(1_000L), single.getTotal());
        assertEquals(1, single.paths().size());

        PathEffect<ExtremePhysique> dual = ApertureService.getTalentEffect(ExtremePhysique.NORTHERN_DARK_ICE_SOUL);
        assertEquals(Map.of(GuPath.ICE_SNOW, PathMarks.ofMarks(500L), GuPath.SOUL, PathMarks.ofMarks(500L)),
                dual.paths());
        assertSame(ExtremePhysique.NORTHERN_DARK_ICE_SOUL, dual.payload());
        assertTrue(ApertureService.getTalentEffect(ExtremePhysique.NONE).isEmpty());
    }

    @Test
    @DisplayName("the save codec round-trips every registered type by id and drops only an unknown one")
    void saveCodecRoundTrips() {
        PathEffectData data = new PathEffectData(List.of(BodyService.getRaceTalent(Race.DRAGONMEN),
                ApertureService.getTalentEffect(ExtremePhysique.NORTHERN_DARK_ICE_SOUL).withEndTick(1234L)));
        JsonElement saved = PathEffectData.CODEC.encodeStart(JsonOps.INSTANCE, data).result().orElseThrow();

        var race = saved.getAsJsonArray().get(0).getAsJsonObject();
        assertEquals("guzhenren:race_talent", race.get("type").getAsString());
        assertEquals("dragonmen", race.get("race").getAsString());
        assertEquals(10L, race.getAsJsonObject("paths").getAsJsonObject("enslavement").get("marks").getAsLong());
        assertEquals(data, PathEffectData.CODEC.parse(JsonOps.INSTANCE, saved).result().orElseThrow());

        race.addProperty("type", "guzhenren:no_such_effect");
        assertEquals(List.of(data.effects().get(1)),
                PathEffectData.CODEC.parse(JsonOps.INSTANCE, saved).result().orElseThrow().effects());
    }

    @Test
    @DisplayName("the sync codec round-trips the store and rejects an unknown type id as a malformed packet")
    void streamCodecRoundTrips() {
        PathEffectData data = new PathEffectData(List.of(BodyService.getRaceTalent(Race.SNOWMEN),
                ApertureService.getTalentEffect(ExtremePhysique.VERDANT_GREAT_SUN).withEndTick(99L)));
        withBuffer(buffer -> {
            PathEffectData.encode(buffer, data);
            assertEquals(data, PathEffectData.decode(buffer));
            assertEquals(0, buffer.readableBytes());

            buffer.writeVarInt(1);
            buffer.writeResourceLocation(Guzhenren.id("no_such_effect"));
            assertThrows(DecoderException.class, () -> PathEffectData.decode(buffer));
        });
    }

    private static PathEffect<Unit> shared(ResourceLocation source, Map<GuPath, PathMarks> paths) {
        return PathEffect.of(SHARED, EffectCarrier.BODY, source, paths, PathEffect.PERMANENT, Unit.INSTANCE);
    }

    private static void assertKey(String key, Component component, Object... args) {
        TranslatableContents contents = (TranslatableContents) component.getContents();
        assertEquals(key, contents.getKey());
        assertArrayEquals(args, contents.getArgs());
    }
}
