package net.alex.guzhenren.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 通用蛊动作：哪些生效催动前伸（{@link EpicFightIntegration#playsReach}）；前伸、炼化举蛊动作及合成层数据。 */

class UseClipTest {

    private static final String CLIPS = "assets/guzhenren/animmodels/animations/biped/";

    @Test
    @DisplayName("a landed use reaches unless it is an auto use, a seated one or one with its own clip this tick")
    void aUseWithoutItsOwnClipReaches() {
        assertTrue(EpicFightIntegration.playsReach(false, false, null, 100L));
        assertTrue(EpicFightIntegration.playsReach(false, false, 99L, 100L), "an earlier shield or throw");
        assertFalse(EpicFightIntegration.playsReach(false, false, 100L, 100L), "the shield or throw of this use");
        assertFalse(EpicFightIntegration.playsReach(true, false, null, 100L), "an auto use");
        assertFalse(EpicFightIntegration.playsReach(false, true, null, 100L), "the Hope Gu sits instead");
    }

    @Test
    @DisplayName("the reach ships: 0.5 s out and back on a HIGH composite layer; the refining hold: a 2 s loop")
    void theClipsShip() throws IOException {
        JsonObject reach = read("skill/use_reach.json");
        for (String name : List.of("Chest", "Arm_R", "Hand_R", "Arm_L")) {
            JsonArray keys = keys(reach, name);
            assertEquals(0.5D, lastTime(reach, name), 1.0E-9D);
            assertEquals(keys.get(0), keys.get(keys.size() - 1), name + " ends where it started");
            assertNotEquals(keys.get(0), keys.get(keys.size() / 2), name + " leaves its rest on the way");
        }
        assertLayer(read("skill/data/use_reach.json"), "HIGH");

        JsonObject hold = read("living/refine_loop.json");
        for (String name : List.of("Chest", "Head", "Arm_R", "Hand_R")) {
            JsonArray keys = keys(hold, name);
            assertEquals(2.0D, lastTime(hold, name), 1.0E-9D);
            assertEquals(keys.get(0), keys.get(keys.size() - 1), name + " loops");
            assertNotEquals(keys.get(0), keys(reach, name).get(0), name + " is held away from the idle hang");
        }
        assertEquals(keys(reach, "Hand_L").get(0), keys(hold, "Hand_L").get(0), "the left arm hangs");
        assertLayer(read("living/data/refine_loop.json"), "MIDDLE");
    }

    private static void assertLayer(JsonObject data, String priority) {
        assertEquals("COMPOSITE_LAYER", data.get("layer").getAsString());
        assertEquals(priority, data.get("priority").getAsString());
        JsonArray masks = data.getAsJsonArray("masks");
        assertEquals(1, masks.size());
        assertEquals("ALL", masks.get(0).getAsJsonObject().get("livingmotion").getAsString());
        assertEquals("guzhenren:chest_arms", masks.get(0).getAsJsonObject().get("type").getAsString());
    }

    private static double lastTime(JsonObject clip, String name) {
        JsonArray times = getJoint(clip, name).getAsJsonArray("time");
        return times.get(times.size() - 1).getAsDouble();
    }

    private static JsonArray keys(JsonObject clip, String name) {
        return getJoint(clip, name).getAsJsonArray("transform");
    }

    private static JsonObject getJoint(JsonObject clip, String name) {
        for (JsonElement bone : clip.getAsJsonArray("animation")) {
            JsonObject joint = bone.getAsJsonObject();
            if (joint.get("name").getAsString().equals(name)) return joint;
        }
        throw new AssertionError("no " + name + " joint in the clip");
    }

    private static JsonObject read(String file) throws IOException {
        try (InputStream in = UseClipTest.class.getClassLoader().getResourceAsStream(CLIPS + file)) {
            assertNotNull(in, () -> file + " is not on the classpath");
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
