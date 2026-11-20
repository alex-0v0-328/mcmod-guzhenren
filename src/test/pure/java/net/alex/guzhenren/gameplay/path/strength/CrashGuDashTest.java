package net.alex.guzhenren.gameplay.path.strength;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.DoubleBinaryOperator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrashGuDashTest {

    private static final String CLIPS = "assets/guzhenren/animmodels/animations/biped/skill/";
    private static final String[] TIMES = {"0.0", "0.0833", "0.1167", "0.25", "0.35"};
    private static final int ROOT_Y = 7;
    private static final double DELTA = 1.0E-9D;
    private static final double TRAVEL_DELTA = 1.0E-3D;

    @Test
    @DisplayName("a dash with no source stays 4.5x the Epic Fight dodge vector (whole family, one tier)")
    void dashCoordScaleIsPinned() {
        assertEquals(4.5D, CrashDash.COORD_SCALE);
        assertEquals(4.5D, CrashDash.getScale(CrashDash.getMultiple(0, false, 0.0D)));
    }

    @Test
    @DisplayName("points, the two boars and the horse each add to the multiple, nothing doubles")
    void theThreeSourcesAddUp() {
        assertEquals(2.0D, CrashDash.getMultiple(100, false, 0.0D), DELTA, "root 100 points over 10 is +1");
        assertEquals(1.5D, CrashDash.getMultiple(0, true, 0.0D), DELTA);
        assertEquals(2.0D, CrashDash.getMultiple(0, false, 1.0D), DELTA);
        assertEquals(3.5D, CrashDash.getMultiple(100, true, 1.0D), DELTA, "a source doubled another");
        assertEquals(1.0D, CrashDash.getMultiple(-4, false, -1.0D), DELTA, "a negative source shortened it");
    }

    @Test
    @DisplayName("the path stops at 32 blocks however many sources stack")
    void thePathStopsAtThirtyTwoBlocks() {
        assertEquals(32.0D, CrashDash.getTravel(CrashDash.getScale(CrashDash.getMultiple(10_000, true, 1.0D))),
                TRAVEL_DELTA);
        assertEquals(27.24D, CrashDash.getTravel(CrashDash.getScale(CrashDash.getMultiple(400, true, 1.0D))),
                1.0E-2D, "the boars and the horse at 400 points left 27.2 blocks");
        assertEquals(21.06D, CrashDash.getTravel(CrashDash.getScale(CrashDash.getMultiple(96, true, 1.0D))),
                1.0E-2D, "the boars and the horse at 96 points left 21.1 blocks");
        assertEquals(10.58D, CrashDash.getTravel(CrashDash.getScale(CrashDash.getMultiple(56, false, 0.0D))),
                1.0E-2D, "a rank five body of 200 jin left the plan's 10.6 blocks");
    }

    @Test
    @DisplayName("the horse plays the clip half again as fast: 7, 5 and 4 ticks")
    void theHorseSpeedsTheClip() {
        assertEquals(1.0D, CrashDash.getPlaySpeed(0.0D), DELTA);
        assertEquals(1.5D, CrashDash.getPlaySpeed(0.5D), DELTA);
        assertEquals(2.0D, CrashDash.getPlaySpeed(1.0D), DELTA);
        assertEquals(1.0D, CrashDash.getPlaySpeed(-0.5D), DELTA, "a negative bonus slowed the clip");
        assertEquals(7, CrashDash.getClipTicks(1.0D));
        assertEquals(5, CrashDash.getClipTicks(1.5D));
        assertEquals(4, CrashDash.getClipTicks(2.0D));
    }

    @Test
    @DisplayName("no tick of a 32 block dash moves past 9 blocks, and the clip still covers the whole path")
    void noTickMovesPastTheServerLimit() throws IOException {
        DoubleBinaryOperator span = getRootSpan("dash_forward.json");
        double scale = CrashDash.MAX_TRAVEL / CrashDash.STEP_TRAVEL;
        assertTrue(span.applyAsDouble(0.0D, CrashDash.TICK_SECONDS * 2.0D) * scale > 10.0D,
                "the first tick no longer outruns the vanilla limit; the test proves nothing");

        float elapsed = 0.0F;
        double moved = 0.0D;
        int ticks = 0;
        while (elapsed < 0.35F - 1.0E-6F) {
            float speed = CrashDash.limitSpeed(2.0F, elapsed, scale, span);
            float next = Math.min(0.35F, elapsed + speed * CrashDash.TICK_SECONDS);
            double step = span.applyAsDouble(elapsed, next) * scale;
            assertTrue(step <= CrashDash.MAX_STEP + 1.0E-6D, "tick " + ticks + " moved " + step + " blocks");
            moved += step;
            elapsed = next;
            assertTrue(++ticks < 20, "the clip stalled under the speed limit");
        }
        assertEquals(span.applyAsDouble(0.0D, 0.35D) * scale, moved, 1.0E-3D);
        float free = CrashDash.limitSpeed(1.0F, 0.0F, CrashDash.COORD_SCALE, span);
        assertEquals(1.0F, free, "the plain 6 block dash was slowed");
    }

    @Test
    @DisplayName("the step travel bounds all four dash clips: about 6, 12 and 24 blocks of path")
    void stepTravelBoundsTheDashClips() throws IOException {
        double forward = getRootTravel("dash_forward.json");
        double backward = getRootTravel("dash_backward.json");
        assertEquals(1.342D, forward, TRAVEL_DELTA);
        assertEquals(1.344D, backward, TRAVEL_DELTA);
        assertEquals(forward, getRootTravel("dash_left.json"), DELTA, "the left dash travels another distance");
        assertEquals(forward, getRootTravel("dash_right.json"), DELTA, "the right dash travels another distance");
        assertTrue(CrashDash.STEP_TRAVEL >= Math.max(forward, backward) - DELTA, "a clip travels past the bound");
        assertEquals(6.0525D, CrashDash.getTravel(CrashDash.getScale(1.0D)), TRAVEL_DELTA);
        assertEquals(24.21D, CrashDash.getTravel(CrashDash.getScale(4.0D)), TRAVEL_DELTA);
    }

    private static DoubleBinaryOperator getRootSpan(String file) throws IOException {
        double[] times = new double[TIMES.length];
        double[] travel = new double[TIMES.length];
        JsonArray frames = getRootFrames(file);
        double first = frames.get(0).getAsJsonArray().get(ROOT_Y).getAsDouble();
        for (int i = 0; i < TIMES.length; i++) {
            times[i] = Double.parseDouble(TIMES[i]);
            travel[i] = Math.abs(frames.get(i).getAsJsonArray().get(ROOT_Y).getAsDouble() - first);
        }
        return (from, to) -> Math.abs(interpolate(times, travel, to) - interpolate(times, travel, from));
    }

    private static double interpolate(double[] times, double[] values, double time) {
        if (time <= times[0]) return values[0];
        for (int i = 1; i < times.length; i++) {
            if (time <= times[i]) {
                return values[i - 1] + (values[i] - values[i - 1]) * (time - times[i - 1]) / (times[i] - times[i - 1]);
            }
        }
        return values[values.length - 1];
    }

    private static double getRootTravel(String file) throws IOException {
        JsonArray frames = getRootFrames(file);
        double first = frames.get(0).getAsJsonArray().get(ROOT_Y).getAsDouble();
        double last = frames.get(frames.size() - 1).getAsJsonArray().get(ROOT_Y).getAsDouble();
        return Math.abs(last - first);
    }

    private static JsonArray getRootFrames(String file) throws IOException {
        try (InputStream in = CrashGuDashTest.class.getClassLoader().getResourceAsStream(CLIPS + file)) {
            assertNotNull(in, () -> file + " is not on the classpath");
            JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            for (JsonElement bone : json.getAsJsonArray("animation")) {
                JsonObject joint = bone.getAsJsonObject();
                if (!joint.get("name").getAsString().equals("Root")) continue;
                JsonArray frames = joint.getAsJsonArray("transform");
                JsonArray times = joint.getAsJsonArray("time");
                assertEquals(TIMES.length, times.size(), () -> file + " changed its keyframe count");
                for (int i = 0; i < TIMES.length; i++) {
                    assertEquals(TIMES[i], times.get(i).getAsString(), file + " changed its timing");
                }
                return frames;
            }
        }
        throw new AssertionError("no Root joint in " + file);
    }
}
