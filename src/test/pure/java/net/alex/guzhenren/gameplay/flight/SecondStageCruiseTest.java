package net.alex.guzhenren.gameplay.flight;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecondStageCruiseTest {

    private static final double EPS = 1.0E-6;
    private static final Vec3 EAST = new Vec3(1.0D, 0.0D, 0.0D);
    private static final Vec3 NORTH = new Vec3(0.0D, 0.0D, -1.0D);
    private static final String CLIPS = "assets/guzhenren/animmodels/animations/biped/";
    private static final int ROOT_Y = 7;

    @Test
    @DisplayName("a sprint fills the throttle from empty in 30 ticks and no further")
    void sprintFillsInThirtyTicks() {
        double throttle = 0.0D;
        for (int tick = 1; tick <= 29; tick++) throttle = SecondStageCruise.nextThrottle(throttle, true);
        assertTrue(throttle < 1.0D);
        throttle = SecondStageCruise.nextThrottle(throttle, true);
        assertEquals(1.0D, throttle, EPS);
        assertEquals(1.0D, SecondStageCruise.nextThrottle(throttle, true), EPS);
    }

    @Test
    @DisplayName("without a sprint a full throttle drains in 15 ticks and lands on exactly zero")
    void coastDrainsInFifteenTicks() {
        double throttle = 1.0D;
        for (int tick = 1; tick <= 14; tick++) throttle = SecondStageCruise.nextThrottle(throttle, false);
        assertTrue(throttle > 0.0D);
        assertEquals(0.0D, SecondStageCruise.nextThrottle(throttle, false));
    }

    @Test
    @DisplayName("an entry starts at sprint-flight speed and drains to zero in five ticks")
    void entryStartsAtSprintFlight() {
        assertEquals(2.0D * SecondStageCruise.PLAIN_FLIGHT_SPEED,
                SecondStageCruise.ENTRY_THROTTLE * SecondStageCruise.getTopSpeed(0.05F), EPS);
        double throttle = SecondStageCruise.ENTRY_THROTTLE;
        for (int tick = 1; tick <= 4; tick++) throttle = SecondStageCruise.nextThrottle(throttle, false);
        assertTrue(throttle > 0.0D);
        assertEquals(0.0D, SecondStageCruise.nextThrottle(throttle, false));
    }

    @Test
    @DisplayName("the top speed is six times plain flight, about 65 blocks a second, and follows the flying speed")
    void topSpeedIsSixTimesPlainFlight() {
        assertEquals(3.2667D, SecondStageCruise.getTopSpeed(0.05F), 1.0E-4);
        assertEquals(2.0D * SecondStageCruise.getTopSpeed(0.05F), SecondStageCruise.getTopSpeed(0.1F), EPS);
        assertEquals(1.6333D, SecondStageCruise.BARRIER_SPEED, 1.0E-4);
    }

    @Test
    @DisplayName("the velocity closes a quarter of the gap to the throttled look each tick")
    void velocityClosesAQuarter() {
        Vec3 next = SecondStageCruise.nextVelocity(Vec3.ZERO, EAST, 1.0D, 4.0D);
        assertEquals(1.0D, next.x, EPS);
        assertEquals(0.0D, next.lengthSqr() - next.x * next.x, EPS);
        Vec3 settled = Vec3.ZERO;
        for (int tick = 0; tick < 60; tick++) settled = SecondStageCruise.nextVelocity(settled, EAST, 0.5D, 4.0D);
        assertEquals(2.0D, settled.x, 1.0E-4);
    }

    @Test
    @DisplayName("a right-angle turn lands 90 percent of the way within 9 ticks")
    void turnSettlesWithinNineTicks() {
        Vec3 velocity = EAST.scale(3.0D);
        for (int tick = 0; tick < 9; tick++) velocity = SecondStageCruise.nextVelocity(velocity, NORTH, 1.0D, 3.0D);
        assertTrue(-velocity.z >= 0.9D * 3.0D, "northward " + -velocity.z);
        assertTrue(velocity.x <= 0.1D * 3.0D, "eastward " + velocity.x);
    }

    @Test
    @DisplayName("the look is normalized, so only the throttle sets the speed")
    void lookIsNormalized() {
        Vec3 settled = Vec3.ZERO;
        for (int tick = 0; tick < 60; tick++) {
            settled = SecondStageCruise.nextVelocity(settled, EAST.scale(5.0D), 1.0D, 3.0D);
        }
        assertEquals(3.0D, settled.length(), 1.0E-4);
    }

    @Test
    @DisplayName("a wall costs what an elytra pays: ten a block a tick lost, less three")
    void wallDamageMatchesTheElytra() {
        assertEquals(29.667F, SecondStageCruise.getWallDamage(3.2667D), 1.0E-3F);
        assertEquals(0.0F, SecondStageCruise.getWallDamage(0.3D), 1.0E-6F);
        assertTrue(SecondStageCruise.getWallDamage(0.2D) < 0.0F);
    }

    @Test
    @DisplayName("only losing the barrier speed in one hit is a crash")
    void crashNeedsTheBarrierSpeed() {
        assertTrue(SecondStageCruise.isCrash(SecondStageCruise.BARRIER_SPEED));
        assertTrue(SecondStageCruise.isCrash(3.2D));
        assertFalse(SecondStageCruise.isCrash(SecondStageCruise.BARRIER_SPEED - 0.01D));
    }

    @Test
    @DisplayName("a ram deals four a block a tick and shoves two blocks a tick along the flight, lifted 0.4")
    void ramDamageAndPush() {
        assertEquals(13.067F, SecondStageCruise.getRamDamage(3.2667D), 1.0E-3F);
        Vec3 push = SecondStageCruise.getRamPush(EAST.scale(3.0D));
        assertEquals(2.0D, push.x, EPS);
        assertEquals(0.4D, push.y, EPS);
        assertEquals(0.0D, push.z, EPS);
    }

    @Test
    @DisplayName("a block is fragile strictly below an explosion resistance of 0.5: glass and leaves, not dirt or sand")
    void fragileStopsBelowDirt() {
        assertTrue(SecondStageCruise.isFragile(0.0F), "slime, honey and scaffolding");
        assertTrue(SecondStageCruise.isFragile(0.3F), "glass");
        assertTrue(SecondStageCruise.isFragile(0.4F), "cactus");
        assertTrue(SecondStageCruise.isFragile(0.49F));
        assertFalse(SecondStageCruise.isFragile(0.5F), "dirt and sand");
        assertFalse(SecondStageCruise.isFragile(1.5F), "cobblestone");
        assertEquals(0.5F, SecondStageCruise.FRAGILE_RESISTANCE);
    }

    @Test
    @DisplayName("the server breaks fragile blocks three ticks ahead, about ten blocks at full speed")
    void breakLookaheadCoversTheClientLead() {
        assertEquals(3, SecondStageCruise.BREAK_LOOKAHEAD_TICKS);
        double reach = SecondStageCruise.BREAK_LOOKAHEAD_TICKS * SecondStageCruise.getTopSpeed(0.05F);
        assertEquals(9.8D, reach, 1.0E-4);
        assertTrue(reach > 2.0D * SecondStageCruise.getTopSpeed(0.05F), "at least two ticks of lead");
    }

    @Test
    @DisplayName("a crash craters at 1.2 a block a tick lost, between two and a TNT's four")
    void crashPowerFollowsTheLostSpeed() {
        assertEquals(2.0F, SecondStageCruise.getCrashPower(SecondStageCruise.BARRIER_SPEED), 1.0E-5F);
        assertEquals(2.4F, SecondStageCruise.getCrashPower(2.0D), 1.0E-5F);
        assertEquals(3.92F, SecondStageCruise.getCrashPower(3.2667D), 1.0E-3F);
        assertEquals(4.0F, SecondStageCruise.getCrashPower(10.0D), 1.0E-5F);
    }

    @Test
    @DisplayName("the flight clips ship: a seamless 0.8 s loop, a 0.4 s crash that slides back and returns")
    void theFlightClipsKeepTheirTiming() throws IOException {
        JsonObject loop = getRoot("living/second_stage_flight_loop.json");
        JsonArray loopKeys = loop.getAsJsonArray("transform");
        assertEquals(0.8D, getLastTime(loop), 1.0E-9D);
        assertEquals(loopKeys.get(0), loopKeys.get(loopKeys.size() - 1), "the loop's last key is its first");

        JsonObject crash = getRoot("skill/second_stage_flight_crash.json");
        JsonArray crashKeys = crash.getAsJsonArray("transform");
        assertEquals(0.4D, getLastTime(crash), 1.0E-9D);
        double start = crashKeys.get(0).getAsJsonArray().get(ROOT_Y).getAsDouble();
        double furthest = start;
        for (JsonElement key : crashKeys) furthest = Math.min(furthest, key.getAsJsonArray().get(ROOT_Y).getAsDouble());
        assertEquals(-0.34D, furthest - start, 1.0E-3D, "the body is thrown back");
        assertEquals(start, crashKeys.get(crashKeys.size() - 1).getAsJsonArray().get(ROOT_Y).getAsDouble(), 1.0E-9D,
                "the crash ends where the entity is");
    }

    private static double getLastTime(JsonObject joint) {
        JsonArray times = joint.getAsJsonArray("time");
        return times.get(times.size() - 1).getAsDouble();
    }

    private static JsonObject getRoot(String file) throws IOException {
        try (InputStream in = SecondStageCruiseTest.class.getClassLoader().getResourceAsStream(CLIPS + file)) {
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
