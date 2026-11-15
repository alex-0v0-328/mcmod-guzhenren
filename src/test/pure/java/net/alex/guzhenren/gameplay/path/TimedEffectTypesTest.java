package net.alex.guzhenren.gameplay.path;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Map;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.body.Skin;
import net.alex.guzhenren.gameplay.path.timed.TimedBuff;
import net.alex.guzhenren.registry.effect.ModPathEffectTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 限时蛊施于身体的五种流派效果：ID、槽、原版效果投射、存档往返；活玩家见 {@code TimedEffectGameTests}。 */

class TimedEffectTypesTest {

    private static final ResourceLocation BOAR = Guzhenren.id("flower_boar_gu_1");
    private static final Map<GuPath, PathMarks> STRENGTH = Map.of(GuPath.STRENGTH, PathMarks.ofPoints(3L));

    @Test
    @DisplayName("each timed type is registered under its id; only the skin is one slot per person")
    void slots() {
        List<PathEffectType<?>> types = List.of(ModPathEffectTypes.BUFF, ModPathEffectTypes.TIMED_SKIN,
                ModPathEffectTypes.MUGWORT_SHIELD, ModPathEffectTypes.VITALITY_LEAF, ModPathEffectTypes.WATCH_RATE);
        for (PathEffectType<?> type : types) assertSame(type, ModPathEffectTypes.get(type.getId()));
        assertTrue(ModPathEffectTypes.TIMED_SKIN.isExclusive());
        assertFalse(ModPathEffectTypes.BUFF.isExclusive());
        assertFalse(ModPathEffectTypes.WATCH_RATE.isExclusive());
        assertFalse(ModPathEffectTypes.MUGWORT_SHIELD.isExclusive());
    }

    @Test
    @DisplayName("the skin and the shield project nothing themselves: their state's service keeps the icon")
    void projections() {
        assertFalse(ModPathEffectTypes.TIMED_SKIN.projects());
        assertFalse(ModPathEffectTypes.MUGWORT_SHIELD.projects());
        assertTrue(ModPathEffectTypes.BUFF.projects());
        assertTrue(ModPathEffectTypes.WATCH_RATE.projects());
        assertTrue(ModPathEffectTypes.VITALITY_LEAF.projects());
        assertNull(ModPathEffectTypes.BUFF.getIcon());
    }

    @Test
    @DisplayName("two buffs of different Gu sit side by side and a repeat of the same source shares a slot")
    void storeSlots() {
        PathEffect<TimedBuff> boar = PathEffect.of(ModPathEffectTypes.BUFF, EffectCarrier.BODY, BOAR, STRENGTH, 600L,
                new TimedBuff(Guzhenren.id("flower_boar_gu_1"), 0));
        PathEffect<TimedBuff> beetle = PathEffect.of(ModPathEffectTypes.BUFF, EffectCarrier.BODY,
                Guzhenren.id("brute_force_longhorn_beetle_gu_1"), STRENGTH, 600L,
                new TimedBuff(Guzhenren.id("brute_force_longhorn_beetle_gu_1"), 0));
        assertFalse(boar.sharesSlot(beetle));
        assertTrue(boar.sharesSlot(boar.withEndTick(900L)));
        assertEquals(PathMarks.ofPoints(6L), new PathEffectData(List.of(boar, beetle)).getMarks(GuPath.STRENGTH));
    }

    @Test
    @DisplayName("a lengthened effect keeps its points and a watch layer adds one layer's worth")
    void lengthAndLayers() {
        PathEffect<ResourceLocation> watch = PathEffect.of(ModPathEffectTypes.WATCH_RATE, EffectCarrier.BODY,
                Guzhenren.id("second_watch_gu_4"), Map.of(GuPath.TIME, PathMarks.ofPoints(192L)), 600L,
                Guzhenren.id("second_watch_gu_4"));

        assertEquals(PathMarks.ofPoints(192L), watch.withEndTick(5_000L).getMarks(GuPath.TIME));
        PathEffect<ResourceLocation> stacked = watch.addLayer(watch.paths());
        assertEquals(2, stacked.layers());
        assertEquals(PathMarks.ofPoints(384L), stacked.getMarks(GuPath.TIME));
    }

    @Test
    @DisplayName("a buff, a skin, a watch form and a shield keep their payload, points and end tick through the save")
    void saveRoundTrip() {
        PathEffect<TimedBuff> buff = PathEffect.of(ModPathEffectTypes.BUFF, EffectCarrier.BODY, BOAR, STRENGTH, 1_234L,
                new TimedBuff(Guzhenren.id("charging_crash_gu"), 3)).addMarks(
                Map.of(GuPath.RULE, PathMarks.ofPoints(48L)));
        PathEffect<Skin> skin = PathEffect.of(ModPathEffectTypes.TIMED_SKIN, EffectCarrier.BODY,
                Guzhenren.id("white_jade_gu_2"), Map.of(GuPath.EARTH, PathMarks.ofPoints(12L)), 99L,
                Skin.WHITE_JADE_SKIN);
        PathEffect<ResourceLocation> watch = PathEffect.of(ModPathEffectTypes.WATCH_RATE, EffectCarrier.BODY,
                Guzhenren.id("third_watch_gu_5"), Map.of(GuPath.TIME, PathMarks.ofPoints(768L)), 700L,
                Guzhenren.id("third_watch_gu_5")).addLayer(Map.of(GuPath.TIME, PathMarks.ofPoints(768L)));
        PathEffect<Unit> shield = PathEffect.of(ModPathEffectTypes.MUGWORT_SHIELD, EffectCarrier.BODY,
                Guzhenren.id("heavenly_mugwort_gu_3"), Map.of(GuPath.WATER, PathMarks.ofPoints(48L)), 600L,
                Unit.INSTANCE);
        PathEffectData data = new PathEffectData(List.of(buff, skin, watch, shield));

        JsonElement saved = PathEffectData.CODEC.encodeStart(JsonOps.INSTANCE, data).result().orElseThrow();

        assertEquals(data, PathEffectData.CODEC.parse(JsonOps.INSTANCE, saved).result().orElseThrow());
        assertEquals(2, watch.layers());
    }
}
