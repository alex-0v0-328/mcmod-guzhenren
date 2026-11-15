package net.alex.guzhenren.gameplay.path;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Map;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.body.Bone;
import net.alex.guzhenren.gameplay.body.Skin;
import net.alex.guzhenren.gameplay.body.Tendon;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.HumanStrength;
import net.alex.guzhenren.registry.effect.ModPathEffectTypes;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 灌注类蛊[Channeled Gu]施于身体的五种流派效果：ID、单槽、不投射图标、存档往返；活玩家见
 * {@code CarvingEffectGameTests}。
 */

class CarvingEffectTypesTest {

    private static final ResourceLocation SOURCE = Guzhenren.id("iron_bone_gu_3");

    @Test
    @DisplayName("each grade part is its own exclusive type, the strengths are per-source, and none projects an icon")
    void slotsAndIcons() {
        for (PathEffectType<?> grade : List.of(ModPathEffectTypes.BODY_SKIN, ModPathEffectTypes.BODY_TENDON,
                ModPathEffectTypes.BODY_BONE)) {
            assertTrue(grade.isExclusive(), grade.getId() + " lets two grades share a part");
        }
        assertFalse(ModPathEffectTypes.BEAST_STRENGTH.isExclusive());
        assertFalse(ModPathEffectTypes.HUMAN_STRENGTH.isExclusive());
        for (PathEffectType<?> type : List.of(ModPathEffectTypes.BODY_SKIN, ModPathEffectTypes.BODY_TENDON,
                ModPathEffectTypes.BODY_BONE, ModPathEffectTypes.BEAST_STRENGTH,
                ModPathEffectTypes.HUMAN_STRENGTH)) {
            assertNull(type.getIcon(), type.getId() + " projects an icon");
            assertSame(type, ModPathEffectTypes.get(type.getId()));
        }
    }

    @Test
    @DisplayName("a grade replaces the grade before it with its points; two beasts and two kinds sit side by side")
    void storeSlots() {
        Map<GuPath, PathMarks> metal = Map.of(GuPath.METAL, PathMarks.ofPoints(48L));
        PathEffect<Bone> three = PathEffect.of(ModPathEffectTypes.BODY_BONE, EffectCarrier.BODY, SOURCE, metal,
                PathEffect.PERMANENT, Bone.IRON_BONE_3);
        PathEffect<Bone> four = PathEffect.of(ModPathEffectTypes.BODY_BONE, EffectCarrier.BODY,
                Guzhenren.id("iron_bone_gu_4"), Map.of(GuPath.METAL, PathMarks.ofPoints(192L)),
                PathEffect.PERMANENT, Bone.IRON_BONE_4);
        assertTrue(three.sharesSlot(four));

        Map<GuPath, PathMarks> strength = Map.of(GuPath.STRENGTH, PathMarks.ofPoints(3L));
        PathEffect<BeastStrength> boar = PathEffect.of(ModPathEffectTypes.BEAST_STRENGTH, EffectCarrier.BODY,
                Guzhenren.id("white_boar_gu_1"), strength, PathEffect.PERMANENT, BeastStrength.WHITE_BOAR);
        PathEffect<BeastStrength> bear = PathEffect.of(ModPathEffectTypes.BEAST_STRENGTH, EffectCarrier.BODY,
                Guzhenren.id("bear_strength_gu_1"), strength, PathEffect.PERMANENT, BeastStrength.BEAR);
        assertFalse(boar.sharesSlot(bear));
        assertEquals(PathMarks.ofPoints(6L), new PathEffectData(List.of(boar, bear)).getMarks(GuPath.STRENGTH));
    }

    @Test
    @DisplayName("a layered human strength keeps its layers and points through the save codec")
    void saveRoundTrip() {
        Map<GuPath, PathMarks> layer = Map.of(GuPath.STRENGTH, PathMarks.ofPoints(12L));
        PathEffect<HumanStrength> human = PathEffect.of(ModPathEffectTypes.HUMAN_STRENGTH, EffectCarrier.BODY,
                Guzhenren.id("tens_jin_strength_gu_2"), layer, PathEffect.PERMANENT, HumanStrength.TEN_JIN)
                .addLayer(layer).addLayer(layer);
        PathEffect<Skin> skin = PathEffect.of(ModPathEffectTypes.BODY_SKIN, EffectCarrier.BODY,
                Guzhenren.id("iron_skin_gu_2"), Map.of(GuPath.METAL, PathMarks.ofPoints(12L)),
                PathEffect.PERMANENT, Skin.IRON_SKIN_2);
        PathEffect<Tendon> tendon = PathEffect.of(ModPathEffectTypes.BODY_TENDON, EffectCarrier.BODY,
                Guzhenren.id("steel_tendon_gu_3"), Map.of(GuPath.METAL, PathMarks.ofPoints(48L)),
                PathEffect.PERMANENT, Tendon.STEEL_TENDON_3);
        PathEffectData data = new PathEffectData(List.of(human, skin, tendon));

        JsonElement saved = PathEffectData.CODEC.encodeStart(JsonOps.INSTANCE, data).result().orElseThrow();

        assertEquals(3, human.layers());
        assertEquals(PathMarks.ofPoints(36L), human.getMarks(GuPath.STRENGTH));
        assertEquals(data, PathEffectData.CODEC.parse(JsonOps.INSTANCE, saved).result().orElseThrow());
    }
}
