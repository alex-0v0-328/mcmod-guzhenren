package net.alex.guzhenren.gameplay.path;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Map;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.path.timed.TimedBuff;
import net.alex.guzhenren.registry.effect.ModPathEffectTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 状态蛊施于人的五种流派效果：ID、槽、类别、原版效果投射、存档往返；活玩家见 {@code StateEffectGameTests}。 */

class StateEffectTypesTest {

    private static final ResourceLocation ZOMBIE_GU = Guzhenren.id("hairy_zombie_gu_3");

    @Test
    @DisplayName("each state type is registered under its id; the zombie, distilling and stone are one slot per person")
    void slots() {
        List<PathEffectType<?>> types = List.of(ModPathEffectTypes.ZOMBIE, ModPathEffectTypes.LIQUOR_DISTILLING,
                ModPathEffectTypes.STONE_APERTURE, ModPathEffectTypes.CASUAL_THOUGHT,
                ModPathEffectTypes.MALICIOUS_THOUGHT);
        for (PathEffectType<?> type : types) assertSame(type, ModPathEffectTypes.get(type.getId()));
        assertTrue(ModPathEffectTypes.ZOMBIE.isExclusive());
        assertTrue(ModPathEffectTypes.LIQUOR_DISTILLING.isExclusive());
        assertTrue(ModPathEffectTypes.STONE_APERTURE.isExclusive());
        assertFalse(ModPathEffectTypes.CASUAL_THOUGHT.isExclusive());
        assertFalse(ModPathEffectTypes.MALICIOUS_THOUGHT.isExclusive());
    }

    @Test
    @DisplayName("the zombie and the malicious thought are harmful, the rest beneficial")
    void categories() {
        assertSame(PathEffectCategory.HARMFUL, ModPathEffectTypes.ZOMBIE.getCategory());
        assertSame(PathEffectCategory.HARMFUL, ModPathEffectTypes.MALICIOUS_THOUGHT.getCategory());
        assertSame(PathEffectCategory.BENEFICIAL, ModPathEffectTypes.CASUAL_THOUGHT.getCategory());
        assertSame(PathEffectCategory.BENEFICIAL, ModPathEffectTypes.LIQUOR_DISTILLING.getCategory());
        assertSame(PathEffectCategory.BENEFICIAL, ModPathEffectTypes.STONE_APERTURE.getCategory());
    }

    @Test
    @DisplayName("distilling and the thoughts project a vanilla effect; the zombie and the stone keep their own icons")
    void projections() {
        assertTrue(ModPathEffectTypes.LIQUOR_DISTILLING.projects());
        assertTrue(ModPathEffectTypes.CASUAL_THOUGHT.projects());
        assertTrue(ModPathEffectTypes.MALICIOUS_THOUGHT.projects());
        assertFalse(ModPathEffectTypes.ZOMBIE.projects());
        assertFalse(ModPathEffectTypes.STONE_APERTURE.projects());
    }

    @Test
    @DisplayName("a zombie, a distilling, a stone and two thoughts keep payload, points and end tick through the save")
    void saveRoundTrip() {
        PathEffect<Unit> zombie = PathEffect.of(ModPathEffectTypes.ZOMBIE, EffectCarrier.BODY, ZOMBIE_GU,
                Map.of(GuPath.TRANSFORMATION, PathMarks.ofPoints(48L)), 1_200L, Unit.INSTANCE);
        PathEffect<Integer> distilling = PathEffect.of(ModPathEffectTypes.LIQUOR_DISTILLING, EffectCarrier.BODY,
                Guzhenren.id("seven_fragrances_liquor_worm_3"), Map.of(GuPath.FOOD, PathMarks.ofPoints(48L)), 24_000L, 3);
        PathEffect<Unit> stone = PathEffect.of(ModPathEffectTypes.STONE_APERTURE, EffectCarrier.APERTURE,
                Guzhenren.id("stone_aperture_gu_3"), Map.of(GuPath.EARTH, PathMarks.ofPoints(48L)),
                PathEffect.PERMANENT, Unit.INSTANCE);
        PathEffect<TimedBuff> casual = PathEffect.of(ModPathEffectTypes.CASUAL_THOUGHT, EffectCarrier.MIND,
                Guzhenren.id("casual_gu_2"), Map.of(GuPath.WISDOM, PathMarks.ofPoints(12L)), 200L,
                new TimedBuff(Guzhenren.id("casual_gu"), 1));
        PathEffect<TimedBuff> malicious = PathEffect.of(ModPathEffectTypes.MALICIOUS_THOUGHT, EffectCarrier.MIND,
                Guzhenren.id("malicious_thought_gu_4"), Map.of(GuPath.WISDOM, PathMarks.ofPoints(192L)), 240L,
                new TimedBuff(Guzhenren.id("malicious_thought_gu"), 2));
        PathEffectData data = new PathEffectData(List.of(zombie, distilling, stone, casual, malicious));

        JsonElement saved = PathEffectData.CODEC.encodeStart(JsonOps.INSTANCE, data).result().orElseThrow();

        assertEquals(data, PathEffectData.CODEC.parse(JsonOps.INSTANCE, saved).result().orElseThrow());
        assertEquals(PathMarks.ofPoints(204L), data.getMarks(GuPath.WISDOM));
    }
}
