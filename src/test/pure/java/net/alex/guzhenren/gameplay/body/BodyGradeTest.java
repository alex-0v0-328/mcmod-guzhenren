package net.alex.guzhenren.gameplay.body;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BodyGradeTest {

    @Test
    @DisplayName("steel tendons climb by rank: each grade replaces the ones before it and nothing after")
    void tendonsClimb() {
        assertTrue(Tendon.NATIVE.isNative());
        assertEquals(0, Tendon.NATIVE.getCapacityBonus());
        assertEquals(128, Tendon.STEEL_TENDON_3.getCapacityBonus());
        assertEquals(256, Tendon.STEEL_TENDON_4.getCapacityBonus());
        assertTrue(Tendon.STEEL_TENDON_3.canReplace(Tendon.NATIVE));
        assertTrue(Tendon.STEEL_TENDON_4.canReplace(Tendon.NATIVE));
        assertTrue(Tendon.STEEL_TENDON_4.canReplace(Tendon.STEEL_TENDON_3));
        assertFalse(Tendon.STEEL_TENDON_3.canReplace(Tendon.STEEL_TENDON_3));
        assertFalse(Tendon.STEEL_TENDON_3.canReplace(Tendon.STEEL_TENDON_4));
        assertFalse(Tendon.NATIVE.canReplace(Tendon.NATIVE));
    }

    @Test
    @DisplayName("iron bones climb by rank and carry max health and knockback resistance")
    void bonesClimb() {
        assertTrue(Bone.NATIVE.isNative());
        assertEquals(0.0D, Bone.NATIVE.getMaxHealthBonus());
        assertEquals(0.0D, Bone.NATIVE.getKnockbackResistance());
        assertEquals(4.0D, Bone.IRON_BONE_3.getMaxHealthBonus());
        assertEquals(0.3D, Bone.IRON_BONE_3.getKnockbackResistance());
        assertEquals(10.0D, Bone.IRON_BONE_4.getMaxHealthBonus());
        assertEquals(0.6D, Bone.IRON_BONE_4.getKnockbackResistance());
        assertTrue(Bone.IRON_BONE_3.canReplace(Bone.NATIVE));
        assertTrue(Bone.IRON_BONE_4.canReplace(Bone.IRON_BONE_3));
        assertFalse(Bone.IRON_BONE_3.canReplace(Bone.IRON_BONE_3));
        assertFalse(Bone.IRON_BONE_3.canReplace(Bone.IRON_BONE_4));
    }

    @Test
    @DisplayName("the flesh is native only and replaces nothing")
    void fleshIsNativeOnly() {
        assertEquals(List.of(Flesh.NATIVE), List.of(Flesh.values()));
        assertTrue(Flesh.NATIVE.isNative());
        assertEquals(0, Flesh.NATIVE.getRank());
        assertFalse(Flesh.NATIVE.canReplace(Flesh.NATIVE));
    }

    @Test
    @DisplayName("each family's ranks run without a gap, so /gzr body names a grade by family and rank")
    void familyRanksRunWithoutAGap() {
        assertEquals(List.of(3, 4), getRanks(Arrays.stream(Tendon.values()).filter(grade -> !grade.isNative())));
        assertEquals(List.of(3, 4), getRanks(Arrays.stream(Bone.values()).filter(grade -> !grade.isNative())));
        assertEquals(List.of(1, 2, 3, 4), getRanks(Arrays.stream(Skin.values())
                .filter(skin -> skin.getFamily() == Skin.Family.BRONZE)));
        assertEquals(List.of(1, 2, 3, 4), getRanks(Arrays.stream(Skin.values())
                .filter(skin -> skin.getFamily() == Skin.Family.IRON)));
        assertEquals(0, Tendon.NATIVE.getRank());
        assertEquals(0, Bone.NATIVE.getRank());
        assertEquals(0, Skin.NATIVE.getRank());
    }

    @Test
    @DisplayName("the four parts save under their own translation prefixes")
    void partsHaveTheirOwnPrefixes() {
        assertEquals("guzhenren.enum.body.skin.bronze_skin_1", Skin.BRONZE_SKIN_1.getTranslationKey());
        assertEquals("guzhenren.enum.body.tendon.steel_tendon_4", Tendon.STEEL_TENDON_4.getTranslationKey());
        assertEquals("guzhenren.enum.body.bone.iron_bone_4", Bone.IRON_BONE_4.getTranslationKey());
        assertEquals("guzhenren.enum.body.flesh.native", Flesh.NATIVE.getTranslationKey());
    }

    private static List<Integer> getRanks(Stream<? extends BodyGrade<?>> grades) {
        return grades.map(BodyGrade::getRank).toList();
    }
}
