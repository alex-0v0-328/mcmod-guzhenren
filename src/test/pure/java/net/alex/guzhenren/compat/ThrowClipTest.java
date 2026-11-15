package net.alex.guzhenren.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.alex.guzhenren.compat.EpicFightIntegration.ThrowClip;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 月刃投掷动作：按距上次投掷间隔选动作（{@link EpicFightIntegration#getThrowClip}）；三文件及合成层数据。 */

class ThrowClipTest {

    private static final String CLIPS = "assets/guzhenren/animmodels/animations/biped/skill/";

    @Test
    @DisplayName("a throw after a pause is the sweep; inside the window the arms alternate, left first")
    void aRunAlternatesArms() {
        long window = EpicFightIntegration.THROW_RUN_TICKS;
        for (ThrowClip last : ThrowClip.values()) {
            assertEquals(ThrowClip.SWEEP, EpicFightIntegration.getThrowClip(last, window + 1L));
            assertEquals(ThrowClip.SWEEP, EpicFightIntegration.getThrowClip(last, -1L), "a clock set back starts over");
        }
        assertEquals(ThrowClip.PUNCH_LEFT, EpicFightIntegration.getThrowClip(ThrowClip.SWEEP, 2L));
        assertEquals(ThrowClip.PUNCH_RIGHT, EpicFightIntegration.getThrowClip(ThrowClip.PUNCH_LEFT, 2L));
        assertEquals(ThrowClip.PUNCH_LEFT, EpicFightIntegration.getThrowClip(ThrowClip.PUNCH_RIGHT, window));
    }

    @Test
    @DisplayName("the throw clips ship: 0.3 s, chest and arms out and back, on the shield's composite layer and mask")
    void theThrowClipsKeepTheirTiming() throws IOException {
        for (String clip : List.of("throw_sweep", "throw_punch_right", "throw_punch_left")) {
            JsonObject json = read(clip + ".json");
            for (String name : List.of("Chest", "Arm_R", "Hand_R", "Arm_L", "Hand_L")) {
                JsonObject joint = getJoint(json, name);
                JsonArray times = joint.getAsJsonArray("time");
                JsonArray keys = joint.getAsJsonArray("transform");
                assertEquals(0.3D, times.get(times.size() - 1).getAsDouble(), 1.0E-9D, clip);
                assertFalse(keys.get(keys.size() - 1).equals(keys.get(keys.size() / 3)),
                        name + " leaves its rest on the way in " + clip);
            }
            assertEquals(getJoint(read("throw_sweep.json"), "Arm_R").getAsJsonArray("transform").get(0),
                    lastKey(json, "Arm_R"), clip + " ends with the right arm hanging");

            JsonObject data = read("data/" + clip + ".json");
            assertEquals("COMPOSITE_LAYER", data.get("layer").getAsString());
            assertEquals("HIGH", data.get("priority").getAsString());
            JsonArray masks = data.getAsJsonArray("masks");
            assertEquals(1, masks.size());
            assertEquals("ALL", masks.get(0).getAsJsonObject().get("livingmotion").getAsString());
            assertEquals("guzhenren:chest_arms", masks.get(0).getAsJsonObject().get("type").getAsString());
        }
    }

    private static JsonElement lastKey(JsonObject clip, String name) {
        JsonArray keys = getJoint(clip, name).getAsJsonArray("transform");
        return keys.get(keys.size() - 1);
    }

    private static JsonObject getJoint(JsonObject clip, String name) {
        for (JsonElement bone : clip.getAsJsonArray("animation")) {
            JsonObject joint = bone.getAsJsonObject();
            if (joint.get("name").getAsString().equals(name)) return joint;
        }
        throw new AssertionError("no " + name + " joint in the throw clip");
    }

    private static JsonObject read(String file) throws IOException {
        try (InputStream in = ThrowClipTest.class.getClassLoader().getResourceAsStream(CLIPS + file)) {
            assertNotNull(in, () -> file + " is not on the classpath");
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
