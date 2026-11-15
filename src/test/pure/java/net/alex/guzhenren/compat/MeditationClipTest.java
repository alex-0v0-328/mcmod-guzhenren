package net.alex.guzhenren.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import net.alex.guzhenren.compat.EpicFightIntegration.MeditationCue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 温养[Nourish]与开窍[Awakening]共用的打坐动作：服务端何时播坐下、起身
 * （{@link EpicFightIntegration#getMeditationCue}）；三文件首尾相接。
 */

class MeditationClipTest {

    private static final String CLIPS = "assets/guzhenren/animmodels/animations/biped/";
    private static final int ROOT_HEIGHT = 11;

    @Test
    @DisplayName("the seat turning on sits down, turning off stands up; a first sight and no change play nothing")
    void aChangeOfSeatPlaysOneClip() {
        assertEquals(MeditationCue.SIT, EpicFightIntegration.getMeditationCue(false, true));
        assertEquals(MeditationCue.STAND, EpicFightIntegration.getMeditationCue(true, false));
        assertEquals(MeditationCue.NONE, EpicFightIntegration.getMeditationCue(true, true));
        assertEquals(MeditationCue.NONE, EpicFightIntegration.getMeditationCue(false, false));
        assertEquals(MeditationCue.NONE, EpicFightIntegration.getMeditationCue(null, true), "a login into a run");
        assertEquals(MeditationCue.NONE, EpicFightIntegration.getMeditationCue(null, false));
    }

    @Test
    @DisplayName("the clips ship: 0.8 s down into the 4 s loop's first key, 1.8 s up from it, the seat on the ground")
    void theClipsJoinEndToEnd() throws IOException {
        JsonObject sit = read("skill/meditate_sit.json");
        JsonObject loop = read("living/meditate_loop.json");
        JsonObject stand = read("skill/meditate_stand.json");
        assertEquals(0.8D, lastTime(sit), 1.0E-9D);
        assertEquals(4.0D, lastTime(loop), 1.0E-9D);
        assertEquals(1.8D, lastTime(stand), 1.0E-4D);

        for (String name : List.of("Root", "Thigh_R", "Leg_R", "Thigh_L", "Leg_L", "Chest", "Head", "Arm_R", "Hand_R",
                "Arm_L", "Hand_L")) {
            JsonArray looped = keys(loop, name);
            assertEquals(looped.get(0), looped.get(looped.size() - 1), name + " loops");
            JsonArray down = keys(sit, name);
            assertEquals(looped.get(0), down.get(down.size() - 1), name + " sits into the loop");
            JsonArray up = keys(stand, name);
            assertEquals(looped.get(0), up.get(0), name + " stands out of the loop");
            assertEquals(down.get(0), up.get(up.size() - 1), name + " stands up into the pose it sat down from");
        }

        double standing = height(keys(sit, "Root").get(0));
        double seated = height(keys(loop, "Root").get(0));
        assertTrue(standing > 0.7D && seated < 0.2D, "the Root drops from " + standing + " to " + seated);
        JsonArray breath = keys(loop, "Root");
        assertTrue(height(breath.get(breath.size() / 2)) > seated, "the breath lifts the seat a little");
    }

    private static double height(JsonElement key) { return key.getAsJsonArray().get(ROOT_HEIGHT).getAsDouble(); }

    private static double lastTime(JsonObject clip) {
        JsonArray times = getJoint(clip, "Root").getAsJsonArray("time");
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
        throw new AssertionError("no " + name + " joint in the meditation clip");
    }

    private static JsonObject read(String file) throws IOException {
        try (InputStream in = MeditationClipTest.class.getClassLoader().getResourceAsStream(CLIPS + file)) {
            assertNotNull(in, () -> file + " is not on the classpath");
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
