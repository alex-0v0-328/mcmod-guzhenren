package net.alex.guzhenren.gameplay.body;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SkinTest {

    @Test
    @DisplayName("each skin takes its flat points off a hit and never goes below zero")
    void flatReductionFloorsAtZero() {
        assertEquals(10.0F, Skin.NATIVE.reduce(10.0F));
        assertEquals(8.0F, Skin.JADE_SKIN.reduce(10.0F));
        assertEquals(6.0F, Skin.WHITE_JADE_SKIN.reduce(10.0F));
        assertEquals(0.0F, Skin.JADE_SKIN.reduce(2.0F));
        assertEquals(0.0F, Skin.WHITE_JADE_SKIN.reduce(3.5F));
        assertEquals(0.0F, Skin.WHITE_JADE_SKIN.reduce(0.0F));
        assertEquals(0.0F, Skin.JADE_SKIN.reduce(-5.0F));
        assertEquals(9.0F, Skin.BRONZE_SKIN_1.reduce(10.0F));
        assertEquals(5.0F, Skin.BRONZE_SKIN_4.reduce(10.0F));
        assertEquals(4.0F, Skin.IRON_SKIN_4.reduce(10.0F));
    }

    @Test
    @DisplayName("only the native skin is native, only the jade skins are timed, and each family climbs by rank")
    void familiesClimbByRank() {
        assertTrue(Skin.NATIVE.isNative());
        assertEquals(0, Skin.NATIVE.getDamageReduction());
        for (Skin skin : Skin.values()) {
            assertEquals(skin == Skin.NATIVE, skin.isNative(), skin.name());
            assertEquals(skin.getFamily() == Skin.Family.JADE, skin.isTimed(), skin.name());
            if (skin.isNative()) continue;

            Skin below = lowerOfSameFamily(skin);
            if (below != null) assertTrue(skin.getDamageReduction() > below.getDamageReduction(), skin.name());
        }
        assertEquals(1, Skin.BRONZE_SKIN_1.getDamageReduction());
        assertEquals(2, Skin.IRON_SKIN_1.getDamageReduction());
        assertEquals(4, Skin.IRON_SKIN_3.getDamageReduction());
        assertEquals(5, Skin.BRONZE_SKIN_4.getDamageReduction());
        assertEquals(6, Skin.IRON_SKIN_4.getDamageReduction());
    }

    @Test
    @DisplayName("any skin goes over a native one, a jade skin over nothing else")
    void jadeOnlyOverNative() {
        for (Skin skin : Skin.values()) {
            if (!skin.isNative()) assertTrue(skin.canReplace(Skin.NATIVE), skin.name());
        }
        assertFalse(Skin.JADE_SKIN.canReplace(Skin.JADE_SKIN));
        assertFalse(Skin.WHITE_JADE_SKIN.canReplace(Skin.JADE_SKIN));
        assertFalse(Skin.JADE_SKIN.canReplace(Skin.WHITE_JADE_SKIN));
        assertFalse(Skin.JADE_SKIN.canReplace(Skin.BRONZE_SKIN_1));
        assertFalse(Skin.WHITE_JADE_SKIN.canReplace(Skin.IRON_SKIN_4));
        assertFalse(Skin.BRONZE_SKIN_3.canReplace(Skin.JADE_SKIN));
    }

    @Test
    @DisplayName("a permanent skin replaces only a lower rank of its own family")
    void permanentSkinReplacesLowerRankOfItsFamily() {
        assertTrue(Skin.BRONZE_SKIN_2.canReplace(Skin.BRONZE_SKIN_1));
        assertTrue(Skin.BRONZE_SKIN_4.canReplace(Skin.BRONZE_SKIN_1));
        assertTrue(Skin.BRONZE_SKIN_4.canReplace(Skin.BRONZE_SKIN_3));
        assertTrue(Skin.IRON_SKIN_4.canReplace(Skin.IRON_SKIN_3));
        assertFalse(Skin.BRONZE_SKIN_1.canReplace(Skin.BRONZE_SKIN_1));
        assertFalse(Skin.BRONZE_SKIN_1.canReplace(Skin.BRONZE_SKIN_2));
        assertFalse(Skin.BRONZE_SKIN_3.canReplace(Skin.BRONZE_SKIN_4));
        assertFalse(Skin.IRON_SKIN_1.canReplace(Skin.BRONZE_SKIN_1));
        assertFalse(Skin.IRON_SKIN_3.canReplace(Skin.BRONZE_SKIN_1));
        assertFalse(Skin.BRONZE_SKIN_3.canReplace(Skin.IRON_SKIN_1));
        assertFalse(Skin.IRON_SKIN_4.canReplace(Skin.BRONZE_SKIN_4));
    }

    private static Skin lowerOfSameFamily(Skin skin) {
        Skin below = null;
        for (Skin other : Skin.values()) {
            if (other.getFamily() == skin.getFamily() && other.getRank() == skin.getRank() - 1) below = other;
        }
        return below;
    }
}
