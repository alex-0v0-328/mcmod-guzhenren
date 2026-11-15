package net.alex.guzhenren;

import java.util.EnumSet;
import net.alex.guzhenren.gameplay.body.Anatomy;
import net.alex.guzhenren.gameplay.body.Appendage;
import net.alex.guzhenren.gameplay.body.BodyData;
import net.alex.guzhenren.gameplay.body.BodyPart;
import net.alex.guzhenren.gameplay.body.Bone;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.alex.guzhenren.gameplay.body.Flesh;
import net.alex.guzhenren.gameplay.body.Injury;
import net.alex.guzhenren.gameplay.body.Organ;
import net.alex.guzhenren.gameplay.body.Physique;
import net.alex.guzhenren.gameplay.body.Race;
import net.alex.guzhenren.gameplay.body.Skin;
import net.alex.guzhenren.gameplay.body.Tendon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BodyDataTest {

    @Test
    void yearConversionAndAgingDoNotWrapAtLongLimits() {
        assertEquals(Long.MAX_VALUE, BodyData.toParts(Long.MAX_VALUE));
        assertEquals(Long.MIN_VALUE, BodyData.toParts(Long.MIN_VALUE));
        BodyData body = BodyData.DEFAULT.withAgeParts(Long.MAX_VALUE).withLifespanParts(Long.MIN_VALUE);
        BodyData lived = body.withLived(1L, 1L);
        assertEquals(Long.MAX_VALUE, lived.ageParts());
        assertEquals(Long.MIN_VALUE, lived.lifespanParts());
    }

    @Test
    @DisplayName("physiques accumulate while zombie forms remain mutually exclusive")
    void physiquesAccumulateWithExclusiveZombieForms() {
        BodyData body = new BodyData(EnumSet.of(Physique.ZOMBIE, Physique.TEMP_ZOMBIE, Physique.EXTREME),
                ExtremePhysique.VERDANT_GREAT_SUN, Race.HUMAN, 1L, 2L, 3L, 5L, 6, 7L, Skin.NATIVE, 8L, Tendon.NATIVE,
                Bone.NATIVE, Flesh.NATIVE, Anatomy.DEFAULT);

        assertTrue(body.hasPhysique(Physique.ZOMBIE));
        assertFalse(body.hasPhysique(Physique.TEMP_ZOMBIE));
        assertTrue(body.hasPhysique(Physique.EXTREME));
        assertEquals(ExtremePhysique.VERDANT_GREAT_SUN, body.extremePhysique());
        assertEquals(6, body.zombieTier());
        assertEquals(5L, body.tempZombieEndTick());
    }

    @Test
    @DisplayName("revival clears zombie physiques, anchors and injuries but keeps Extreme and appendages")
    void revivalClearsZombieState() {
        Anatomy anatomy = Anatomy.DEFAULT.withInjury(BodyPart.RIGHT_LEG, Injury.CRIPPLED)
                .withInjury(Organ.EARS, Injury.LIGHT).withAppendage(Appendage.TAIL, true);
        BodyData body = new BodyData(EnumSet.of(Physique.ZOMBIE, Physique.EXTREME),
                ExtremePhysique.GREAT_STRENGTH_TRUE_MARTIAL, Race.HUMAN, 1L, 2L, 3L, 5L, 6, 7L, Skin.JADE_SKIN, 8L,
                Tendon.STEEL_TENDON_3, Bone.IRON_BONE_3, Flesh.NATIVE, anatomy);

        BodyData revived = body.withRevived();

        assertFalse(revived.hasPhysique(Physique.ZOMBIE));
        assertFalse(revived.hasPhysique(Physique.TEMP_ZOMBIE));
        assertTrue(revived.hasPhysique(Physique.EXTREME));
        assertEquals(ExtremePhysique.GREAT_STRENGTH_TRUE_MARTIAL, revived.extremePhysique());
        assertEquals(BodyData.UNTRACKED, revived.tempZombieEndTick());
        assertEquals(BodyData.NO_ZOMBIE_TIER, revived.zombieTier());
        assertEquals(Skin.NATIVE, revived.skin());
        assertEquals(BodyData.UNTRACKED, revived.skinEndTick());
        assertEquals(Tendon.STEEL_TENDON_3, revived.tendon());
        assertEquals(Bone.IRON_BONE_3, revived.bone());
        assertTrue(revived.anatomy().isUninjured());
        assertTrue(revived.anatomy().hasAppendage(Appendage.TAIL));
    }

    @Test
    @DisplayName("a permanent skin keeps no end tick, never runs out and survives revival")
    void permanentSkinStaysThroughRevival() {
        BodyData bronze = BodyData.DEFAULT.withSkin(Skin.BRONZE_SKIN_2, 500L);
        assertEquals(BodyData.UNTRACKED, bronze.skinEndTick());
        assertFalse(bronze.hasNativeSkin());
        assertFalse(bronze.hasSkinRunOut(Long.MAX_VALUE));
        assertEquals(0L, bronze.getSkinTicksLeft(0L));
        assertEquals(Skin.BRONZE_SKIN_2, bronze.withRevived().skin());

        BodyData tempered = BodyData.DEFAULT.withTendon(Tendon.STEEL_TENDON_4).withBone(Bone.IRON_BONE_4);
        assertEquals(Tendon.STEEL_TENDON_4, tempered.withRevived().tendon());
        assertEquals(Bone.IRON_BONE_4, tempered.withRevived().bone());
        assertEquals(Tendon.NATIVE, BodyData.DEFAULT.tendon());
        assertEquals(Bone.NATIVE, BodyData.DEFAULT.bone());
        assertEquals(Flesh.NATIVE, BodyData.DEFAULT.flesh());
    }

    @Test
    @DisplayName("a native skin keeps no end tick, a jade skin keeps its own and runs out on it")
    void skinKeepsItsEndTickOnlyWhileWorn() {
        BodyData bare = BodyData.DEFAULT.withSkin(Skin.NATIVE, 500L);
        assertEquals(BodyData.UNTRACKED, bare.skinEndTick());
        assertTrue(bare.hasNativeSkin());
        assertFalse(bare.hasSkinRunOut(1_000L));
        assertEquals(0L, bare.getSkinTicksLeft(0L));

        BodyData jade = BodyData.DEFAULT.withSkin(Skin.WHITE_JADE_SKIN, 500L);
        assertEquals(500L, jade.skinEndTick());
        assertFalse(jade.hasSkinRunOut(499L));
        assertTrue(jade.hasSkinRunOut(500L));
        assertEquals(200L, jade.getSkinTicksLeft(300L));
        assertEquals(0L, jade.getSkinTicksLeft(900L));
    }
}
