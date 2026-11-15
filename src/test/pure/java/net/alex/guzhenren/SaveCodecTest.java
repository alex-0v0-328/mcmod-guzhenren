package net.alex.guzhenren;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.alex.guzhenren.gameplay.aperture.Aperture;
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
import net.alex.guzhenren.gameplay.body.Skin;
import net.alex.guzhenren.gameplay.body.Tendon;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SaveCodecTest {

    @Test
    void bodyCodecWritesOnlyTheCurrentFields() {
        JsonObject encoded = BodyData.CODEC.encodeStart(JsonOps.INSTANCE, new BodyData(
                SetHolder.EXTREME, ExtremePhysique.VERDANT_GREAT_SUN, BodyData.DEFAULT.race(),
                BodyData.DEFAULT.ageParts(), BodyData.DEFAULT.lifespanParts(), BodyData.UNTRACKED,
                BodyData.UNTRACKED, BodyData.NO_ZOMBIE_TIER, BodyData.UNTRACKED, Skin.NATIVE, BodyData.UNTRACKED,
                Tendon.NATIVE, Bone.NATIVE, Flesh.NATIVE, Anatomy.DEFAULT))
                .getOrThrow().getAsJsonObject();

        assertTrue(encoded.has("physiques"));
        assertTrue(encoded.has("extreme_physique"));
        assertFalse(encoded.has("life_form"));
        assertFalse(encoded.has("death_qi_lifespan_lost"));
    }

    @Test
    void anatomyCodecKeepsOnlyInjuredEntries() {
        Anatomy anatomy = Anatomy.DEFAULT.withInjury(BodyPart.LEFT_ARM, Injury.SEVERE)
                .withInjury(BodyPart.HEAD, Injury.NONE).withInjury(Organ.EYES, Injury.CRIPPLED)
                .withAppendage(Appendage.WINGS, true);
        JsonObject encoded = Anatomy.CODEC.encodeStart(JsonOps.INSTANCE, anatomy).getOrThrow().getAsJsonObject();

        assertEquals(1, encoded.getAsJsonObject("limb_injuries").size());
        assertEquals("severe", encoded.getAsJsonObject("limb_injuries").get("left_arm").getAsString());
        assertEquals(anatomy, Anatomy.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow());
    }

    @Test
    void retiredBodyKeysAreIgnored() {
        JsonObject retired = new JsonObject();
        retired.addProperty("life_form", "zombie");
        retired.addProperty("death_qi_lifespan_lost", 30L);

        BodyData decoded = BodyData.CODEC.parse(JsonOps.INSTANCE, retired).getOrThrow();
        assertTrue(decoded.physiques().isEmpty());
        assertEquals(BodyData.DEFAULT.lifespanParts(), decoded.lifespanParts());
    }

    @Test
    void retiredApertureKeysAreIgnoredAndNeverWritten() {
        JsonObject retired = JsonParser.parseString("""
                {"base_essence":100,"extreme_physique":"verdant_great_sun","zombie_opened":true}
                """).getAsJsonObject();

        Aperture decoded = Aperture.CODEC.parse(JsonOps.INSTANCE, retired).getOrThrow();
        assertEquals(100, decoded.baseEssence());

        JsonObject encoded = Aperture.CODEC.encodeStart(JsonOps.INSTANCE, decoded).getOrThrow().getAsJsonObject();
        assertFalse(encoded.has("extreme_physique"));
        assertFalse(encoded.has("zombie_opened"));
    }

    private static final class SetHolder {

        private static final java.util.Set<Physique> EXTREME = java.util.Set.of(Physique.EXTREME);
    }
}
