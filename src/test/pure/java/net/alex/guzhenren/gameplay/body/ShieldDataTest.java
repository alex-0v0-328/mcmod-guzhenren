package net.alex.guzhenren.gameplay.body;

import static net.alex.guzhenren.TestBuffers.withBuffer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ShieldDataTest {

    private static final String CLIPS = "assets/guzhenren/animmodels/animations/biped/skill/";
    private static final ShieldLayer FULL = new ShieldLayer(ShieldType.HEAVENLY_MUGWORT, 180.0F, 180.0F, 600L);

    @Test
    @DisplayName("raising a kind puts it on top and replaces its older layer instead of stacking")
    void raiseReplacesItsKind() {
        ShieldData shield = ShieldData.DEFAULT.raise(FULL.withPoints(20.0F)).raise(FULL);

        assertEquals(List.of(FULL), shield.layers());
        assertEquals(FULL, shield.find(ShieldType.HEAVENLY_MUGWORT));
        assertNull(ShieldData.DEFAULT.find(ShieldType.HEAVENLY_MUGWORT));
        assertEquals(ShieldData.DEFAULT, shield.without(ShieldType.HEAVENLY_MUGWORT));
    }

    @Test
    @DisplayName("the totals and isUp skip a layer whose time ran out or whose points are spent")
    void totalsSkipLayersDown() {
        ShieldData shield = new ShieldData(List.of(FULL, FULL.withPoints(0.0F)));

        assertTrue(shield.isUp(599L));
        assertEquals(180.0F, shield.getPoints(599L));
        assertEquals(180.0F, shield.getMax(599L));
        assertFalse(shield.isUp(600L));
        assertEquals(0.0F, shield.getPoints(600L));
        assertEquals(ShieldData.DEFAULT, shield.onlyUp(600L));
    }

    @Test
    @DisplayName("the layers round-trip through the save codec and the stream codec in order")
    void roundTrips() {
        ShieldData shield = new ShieldData(List.of(FULL.withPoints(12.5F), FULL));

        Tag tag = ShieldData.CODEC.encodeStart(NbtOps.INSTANCE, shield).getOrThrow();
        assertEquals(shield, ShieldData.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow());

        withBuffer(buffer -> {
            ShieldData.encode(buffer, shield);
            assertEquals(shield, ShieldData.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        });
    }

    @Test
    @DisplayName("the shield clip ships: 0.6 s, chest and arms out and back, on a composite layer that leaves the legs alone")
    void theShieldClipKeepsItsTiming() throws IOException {
        JsonObject clip = read("shield_raise.json");
        for (String name : List.of("Chest", "Arm_R", "Hand_R", "Arm_L", "Hand_L")) {
            JsonObject joint = getJoint(clip, name);
            JsonArray times = joint.getAsJsonArray("time");
            JsonArray keys = joint.getAsJsonArray("transform");
            assertEquals(0.6D, times.get(times.size() - 1).getAsDouble(), 1.0E-9D);
            assertEquals(keys.get(0), keys.get(keys.size() - 1), name + " ends where it started");
            assertFalse(keys.get(0).equals(keys.get(keys.size() / 2)), name + " leaves its rest on the way");
        }

        JsonObject data = read("data/shield_raise.json");
        assertEquals("COMPOSITE_LAYER", data.get("layer").getAsString());
        assertEquals("HIGH", data.get("priority").getAsString());
        JsonArray masks = data.getAsJsonArray("masks");
        assertEquals(1, masks.size());
        assertEquals("ALL", masks.get(0).getAsJsonObject().get("livingmotion").getAsString());
        assertEquals("guzhenren:chest_arms", masks.get(0).getAsJsonObject().get("type").getAsString());

        List<String> joints = new java.util.ArrayList<>();
        read("../../../joint_mask/biped/chest_arms.json").getAsJsonArray("joints")
                .forEach(joint -> joints.add(joint.getAsString()));
        assertTrue(joints.containsAll(List.of("Chest", "Head", "Arm_R", "Hand_R", "Arm_L", "Hand_L")));
        for (String kept : List.of("Root", "Torso", "Thigh_R", "Leg_R", "Thigh_L", "Leg_L")) {
            assertFalse(joints.contains(kept), kept + " stays with the base layer");
        }
    }

    private static JsonObject getJoint(JsonObject clip, String name) {
        for (JsonElement bone : clip.getAsJsonArray("animation")) {
            JsonObject joint = bone.getAsJsonObject();
            if (joint.get("name").getAsString().equals(name)) return joint;
        }
        throw new AssertionError("no " + name + " joint in the shield clip");
    }

    private static JsonObject read(String file) throws IOException {
        try (InputStream in = ShieldDataTest.class.getClassLoader().getResourceAsStream(CLIPS + file)) {
            assertNotNull(in, () -> file + " is not on the classpath");
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
