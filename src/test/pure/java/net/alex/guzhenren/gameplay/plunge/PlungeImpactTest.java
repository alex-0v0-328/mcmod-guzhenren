package net.alex.guzhenren.gameplay.plunge;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PlungeImpactTest {

    private static final double EPSILON = 1.0E-6D;
    private static final String CLIPS = "assets/guzhenren/animmodels/animations/biped/";
    private static final int ROOT_Y = 7;
    private static final int ROOT_HEIGHT = 11;

    @Test
    @DisplayName("the height is the drop from the fall's top, never negative")
    void heightIsTheDropFromTheTop() {
        assertEquals(25.0D, PlungeImpact.getHeight(90.0D, 65.0D), EPSILON);
        assertEquals(0.0D, PlungeImpact.getHeight(64.0D, 70.0D), EPSILON);
    }

    @Test
    @DisplayName("the damage is three times vanilla's fall damage, and no fall damage stays none")
    void damageTriplesVanilla() {
        assertEquals(21, PlungeImpact.getDamage(10.0D, 3.0D, 1.0D));
        assertEquals(93, PlungeImpact.getDamage(34.0D, 3.0D, 1.0D));
        assertEquals(3, PlungeImpact.getDamage(3.5D, 3.0D, 1.0D));
        assertEquals(0, PlungeImpact.getDamage(3.0D, 3.0D, 1.0D));
        assertEquals(0, PlungeImpact.getDamage(1.0D, 3.0D, 1.0D));
    }

    @Test
    @DisplayName("vanilla's safe distance and multiplier attributes shape the damage before it triples")
    void attributesShapeTheDamage() {
        assertEquals(12, PlungeImpact.getDamage(10.0D, 3.0D, 0.5D));
        assertEquals(15, PlungeImpact.getDamage(10.0D, 5.0D, 1.0D));
        assertEquals(0, PlungeImpact.getDamage(10.0D, 3.0D, 0.0D));
    }

    @Test
    @DisplayName("no crater below four blocks; two at four, one more every fifteen, six from sixty-four up")
    void craterPowerGrowsWithHeightToACap() {
        assertEquals(0.0F, PlungeImpact.getCraterPower(3.99D), EPSILON);
        assertEquals(2.0F, PlungeImpact.getCraterPower(4.0D), EPSILON);
        assertEquals(3.0F, PlungeImpact.getCraterPower(19.0D), EPSILON);
        assertEquals(6.0F, PlungeImpact.getCraterPower(64.0D), EPSILON);
        assertEquals(6.0F, PlungeImpact.getCraterPower(300.0D), EPSILON);
    }

    @Test
    @DisplayName("the impact shows from three blocks: two until the crater outgrows it, then the crater's power")
    void impactPowerShowsFromThreeBlocks() {
        assertEquals(0.0F, PlungeImpact.getImpactPower(2.99D), EPSILON);
        assertEquals(2.0F, PlungeImpact.getImpactPower(3.0D), EPSILON);
        assertEquals(2.0F, PlungeImpact.getImpactPower(4.0D), EPSILON);
        assertEquals(3.0F, PlungeImpact.getImpactPower(19.0D), EPSILON);
        assertEquals(6.0F, PlungeImpact.getImpactPower(300.0D), EPSILON);
    }

    @Test
    @DisplayName("the plunge clips ship: a seamless 0.4 s fall loop, a 0.8 s landing that crouches and moves nothing")
    void thePlungeClipsKeepTheirTiming() throws IOException {
        JsonObject fall = getRoot("living/plunge_fall.json");
        JsonArray fallKeys = fall.getAsJsonArray("transform");
        assertEquals(0.4D, getLastTime(fall), 1.0E-9D);
        assertEquals(fallKeys.get(0), fallKeys.get(fallKeys.size() - 1), "the loop's last key is its first");

        JsonObject land = getRoot("skill/plunge_land.json");
        JsonArray landKeys = land.getAsJsonArray("transform");
        assertEquals(0.8D, getLastTime(land), 1.0E-9D);
        assertEquals(fallKeys.get(0), landKeys.get(0), "the landing starts from the fall pose");
        double start = landKeys.get(0).getAsJsonArray().get(ROOT_Y).getAsDouble();
        double lowest = Double.MAX_VALUE;
        for (JsonElement key : landKeys) {
            assertEquals(start, key.getAsJsonArray().get(ROOT_Y).getAsDouble(), 1.0E-9D, "the landing has no travel");
            lowest = Math.min(lowest, key.getAsJsonArray().get(ROOT_HEIGHT).getAsDouble());
        }
        assertEquals(0.33D, lowest, 1.0E-3D, "the crouch is deep");
        assertEquals(0.752D, landKeys.get(landKeys.size() - 1).getAsJsonArray().get(ROOT_HEIGHT).getAsDouble(),
                1.0E-3D, "the landing ends standing");
    }

    private static double getLastTime(JsonObject joint) {
        JsonArray times = joint.getAsJsonArray("time");
        return times.get(times.size() - 1).getAsDouble();
    }

    private static JsonObject getRoot(String file) throws IOException {
        try (InputStream in = PlungeImpactTest.class.getClassLoader().getResourceAsStream(CLIPS + file)) {
            assertNotNull(in, () -> file + " is not on the classpath");
            JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            for (JsonElement bone : json.getAsJsonArray("animation")) {
                JsonObject joint = bone.getAsJsonObject();
                if (joint.get("name").getAsString().equals("Root")) return joint;
            }
        }
        throw new AssertionError("no Root joint in " + file);
    }
}
