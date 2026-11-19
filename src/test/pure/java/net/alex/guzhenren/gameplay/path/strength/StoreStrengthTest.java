package net.alex.guzhenren.gameplay.path.strength;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StoreStrengthTest {

    private static final int RANK_THREE_ALL_OUT = StrengthService.ALL_OUT_LOWEST_TIER;
    private static final Vec3 FEET = Vec3.ZERO;
    private static final Vec3 NORTH = new Vec3(0.0D, 0.0D, -1.0D);
    private static final double HEIGHT = 1.8D;
    private static final double RANGE = 6.0D;
    private static final double WIDE_RANGE = 9.0D;
    private static final String CLIPS = "assets/guzhenren/animmodels/animations/biped/skill/";
    private static final int ROOT_Y = 7;

    private static AABB mobAt(double x, double y, double z) {
        return new AABB(x - 0.3D, y, z - 0.3D, x + 0.3D, y + 1.8D, z + 0.3D);
    }

    private static float fill(int ticks, int allOutTier) {
        float progress = 0.0F;
        for (int tick = 0; tick < ticks; tick++) progress = StoreStrength.fill(progress, allOutTier);
        return progress;
    }

    @Test
    @DisplayName("a charge fills in four seconds, or 70/60/50% of it under rank three/four/five All-Out Effort")
    void theChargeTimeFollowsAllOutEffort() {
        assertEquals(80, StoreStrength.getChargeTicks(StrengthService.NO_ALL_OUT));
        assertEquals(56, StoreStrength.getChargeTicks(RANK_THREE_ALL_OUT));
        assertEquals(48, StoreStrength.getChargeTicks(RANK_THREE_ALL_OUT + 1));
        assertEquals(40, StoreStrength.getChargeTicks(RANK_THREE_ALL_OUT + 2));
    }

    @Test
    @DisplayName("a charge counts as full two ticks early and never fills past one")
    void aChargeIsFullWithinTheSlack() {
        float progress = 0.0F;
        for (int tick = 0; tick < StoreStrength.CHARGE_TICKS - StoreStrength.RELEASE_SLACK_TICKS - 1; tick++) {
            progress = StoreStrength.fill(progress, StrengthService.NO_ALL_OUT);
        }
        assertFalse(StoreStrength.isFull(progress, StrengthService.NO_ALL_OUT));
        progress = StoreStrength.fill(progress, StrengthService.NO_ALL_OUT);
        assertTrue(StoreStrength.isFull(progress, StrengthService.NO_ALL_OUT));
        for (int tick = 0; tick < 10; tick++) progress = StoreStrength.fill(progress, StrengthService.NO_ALL_OUT);
        assertEquals(1.0F, progress);
    }

    @Test
    @DisplayName("a charge let go under a quarter is cancelled; from a quarter on it strikes")
    void aChargeStrikesFromAQuarter() {
        assertFalse(StoreStrength.isReady(fill(10, StrengthService.NO_ALL_OUT), StrengthService.NO_ALL_OUT),
                "half a second of the four struck");
        assertEquals(0.0F, StoreStrength.getFactor(fill(10, StrengthService.NO_ALL_OUT), StrengthService.NO_ALL_OUT));
        assertTrue(StoreStrength.isReady(fill(20, StrengthService.NO_ALL_OUT), StrengthService.NO_ALL_OUT),
                "one second of the four did not strike");
        assertTrue(StoreStrength.isReady(1.0F, StrengthService.NO_ALL_OUT));
        assertEquals(0.25F, StoreStrength.MIN_PROGRESS);
    }

    @Test
    @DisplayName("the quarter counts as reached two ticks early, at the least factor")
    void aChargeIsReadyWithinTheSlack() {
        int ready = StoreStrength.CHARGE_TICKS / 4 - StoreStrength.RELEASE_SLACK_TICKS;
        assertFalse(StoreStrength.isReady(fill(ready - 1, StrengthService.NO_ALL_OUT), StrengthService.NO_ALL_OUT));
        assertTrue(StoreStrength.isReady(fill(ready, StrengthService.NO_ALL_OUT), StrengthService.NO_ALL_OUT));
        assertEquals(1.5F, StoreStrength.getFactor(fill(ready, StrengthService.NO_ALL_OUT),
                StrengthService.NO_ALL_OUT), 1.0E-6F);
    }

    @Test
    @DisplayName("the charge factor grows in a line from 1.5 at a quarter to 3 when full")
    void theFactorGrowsWithTheProgress() {
        assertEquals(1.5F, StoreStrength.getFactor(0.25F), 1.0E-6F);
        assertEquals(2.0F, StoreStrength.getFactor(0.5F), 1.0E-6F);
        assertEquals(2.5F, StoreStrength.getFactor(0.75F), 1.0E-6F);
        assertEquals(3.0F, StoreStrength.getFactor(1.0F), 1.0E-6F);
        assertEquals(3.0F, StoreStrength.getFactor(4.0F), 1.0E-6F, "a factor past the full charge");
        assertEquals(3.0F, StoreStrength.MAX_FACTOR);

        int none = StrengthService.NO_ALL_OUT;
        assertEquals(1.5F, StoreStrength.getFactor(fill(20, none), none), 1.0E-4F);
        assertEquals(2.0F, StoreStrength.getFactor(fill(40, none), none), 1.0E-4F);
        assertEquals(2.5F, StoreStrength.getFactor(fill(60, none), none), 1.0E-4F);
        assertEquals(3.0F, StoreStrength.getFactor(fill(80, none), none));
    }

    @Test
    @DisplayName("the factor entering the settlement is rounded to a ten-thousandth; round factors stay as they are")
    void theExactFactorDropsTheFloatDrift() {
        assertEquals(2.0D, StoreStrength.getExactFactor(1.9999998F));
        assertEquals(1.9D, StoreStrength.getExactFactor(1.8999999F));
        assertEquals(2.0D, StoreStrength.getExactFactor(2.0000002F));
        assertEquals(1.5D, StoreStrength.getExactFactor(1.5F));
        assertEquals(3.0D, StoreStrength.getExactFactor(3.0F));
        assertEquals(199, StrengthService.getSettledPoints(100, 0, 1.9999998F), "the drift costs no point unrounded");
        assertEquals(200, StrengthService.getSettledPoints(100, 0, StoreStrength.getExactFactor(1.9999998F)));
    }

    @Test
    @DisplayName("a charge filled tick by tick settles the points its share of the charge time gives, never one less")
    void aTickedChargeSettlesItsExactShare() {
        int none = StrengthService.NO_ALL_OUT;
        double half = StoreStrength.getExactFactor(StoreStrength.getFactor(fill(40, none), none));
        assertEquals(2.0D, half);
        assertEquals(200, StrengthService.getSettledPoints(100, 0, half), "half a charge lost a point to the drift");

        for (int tier : new int[] { none, RANK_THREE_ALL_OUT, RANK_THREE_ALL_OUT + 1, RANK_THREE_ALL_OUT + 2 }) {
            int total = StoreStrength.getChargeTicks(tier);
            for (int ticks = total / 4; ticks < total - StoreStrength.RELEASE_SLACK_TICKS; ticks++) {
                double factor = StoreStrength.getExactFactor(StoreStrength.getFactor(fill(ticks, tier), tier));
                assertEquals(100 + 200 * ticks / total, StrengthService.getSettledPoints(100, 0, factor),
                        ticks + " of " + total + " ticks");
            }
        }
    }

    @Test
    @DisplayName("All-Out Effort shortens the quarter and every step of the factor by the same share")
    void allOutEffortKeepsTheSameShares() {
        int rankFive = RANK_THREE_ALL_OUT + 2;
        assertFalse(StoreStrength.isReady(fill(5, rankFive), rankFive), "an eighth of the short charge struck");
        assertEquals(1.5F, StoreStrength.getFactor(fill(10, rankFive), rankFive), 1.0E-4F);
        assertEquals(2.0F, StoreStrength.getFactor(fill(20, rankFive), rankFive), 1.0E-4F);
        assertEquals(3.0F, StoreStrength.getFactor(fill(40, rankFive), rankFive));
        assertEquals(1.5F, StoreStrength.getFactor(fill(14, RANK_THREE_ALL_OUT), RANK_THREE_ALL_OUT), 1.0E-4F);
        assertEquals(2.0F, StoreStrength.getFactor(fill(28, RANK_THREE_ALL_OUT), RANK_THREE_ALL_OUT), 1.0E-4F);
        assertEquals(3.0F, StoreStrength.getFactor(fill(56, RANK_THREE_ALL_OUT), RANK_THREE_ALL_OUT));
    }

    @Test
    @DisplayName("the charged punch rolls its own release: 70%, 75% with the brown bear, always under All-Out Effort")
    void theChance() {
        float brownBear = BeastStrength.BROWN_BEAR.getChanceBonus();
        assertEquals(0.70F, StoreStrength.getChance(false, 0.0F), 1.0E-6F);
        assertEquals(0.75F, StoreStrength.getChance(false, brownBear), 1.0E-6F);
        assertEquals(1.0F, StoreStrength.getChance(true, 0.0F));
        assertEquals(1.0F, StoreStrength.getChance(true, brownBear), "All-Out Effort passed a sure roll");
    }

    @Test
    @DisplayName("the range grows with the root of the points from 4 blocks up to 16; widened it is half again, "
            + "up to 24")
    void rangeGrowsWithTheRootOfThePoints() {
        assertEquals(4.0D, StoreStrength.getRange(0, false), 1.0E-9D);
        assertEquals(10.0D, StoreStrength.getRange(400, false), 1.0E-9D);
        assertEquals(15.0D, StoreStrength.getRange(400, true), 1.0E-9D);
        assertEquals(16.0D, StoreStrength.getRange(10_000, false), 1.0E-9D);
        assertEquals(19.5D, StoreStrength.getRange(900, true), 1.0E-9D, "the widened range stopped at 16");
        assertEquals(24.0D, StoreStrength.getRange(10_000, true), 1.0E-9D);
        assertEquals(5.0D, StoreStrength.getBurstRadius(400, false), 1.0E-9D);
        assertEquals(12.0D, StoreStrength.getBurstRadius(10_000, true), 1.0E-9D);
    }

    @Test
    @DisplayName("a six-block fan reaches 45 degrees either side, a widened nine-block one 135, 270 in all")
    void theFanWidensUnderHardship() {
        double range = RANGE;
        double half = AreaImpact.getHalfAngle(false);
        assertEquals(45.0D, half);
        assertTrue(StoreStrength.isInFan(FEET, HEIGHT, NORTH, range, half, mobAt(0.0D, 0.0D, -5.0D)));
        assertFalse(StoreStrength.isInFan(FEET, HEIGHT, NORTH, range, half, mobAt(0.0D, 0.0D, -7.0D)));
        assertTrue(StoreStrength.isInFan(FEET, HEIGHT, NORTH, range, half, mobAt(2.0D, 0.0D, -3.0D)));
        assertFalse(StoreStrength.isInFan(FEET, HEIGHT, NORTH, range, half, mobAt(3.0D, 0.0D, 0.0D)));

        double wide = WIDE_RANGE;
        double wideHalf = AreaImpact.getHalfAngle(true);
        assertEquals(135.0D, wideHalf);
        assertTrue(StoreStrength.isInFan(FEET, HEIGHT, NORTH, wide, wideHalf, mobAt(3.0D, 0.0D, 0.0D)));
        assertTrue(StoreStrength.isInFan(FEET, HEIGHT, NORTH, wide, wideHalf, mobAt(0.0D, 0.0D, -8.5D)));
        assertTrue(StoreStrength.isInFan(FEET, HEIGHT, NORTH, wide, wideHalf, mobAt(3.0D, 0.0D, 2.0D)),
                "124 degrees off the axis");
        assertTrue(StoreStrength.isInFan(FEET, HEIGHT, NORTH, wide, wideHalf, mobAt(-3.0D, 0.0D, 2.0D)));
        assertFalse(StoreStrength.isInFan(FEET, HEIGHT, NORTH, wide, wideHalf, mobAt(2.0D, 0.0D, 3.0D)),
                "146 degrees off the axis");
        assertFalse(StoreStrength.isInFan(FEET, HEIGHT, NORTH, wide, wideHalf, mobAt(0.0D, 0.0D, 3.0D)));
    }

    @Test
    @DisplayName("the fan spans from a block under the feet to two over the head")
    void theFanHasAHeightBand() {
        double range = RANGE;
        double half = AreaImpact.getHalfAngle(false);
        assertTrue(StoreStrength.isInFan(FEET, HEIGHT, NORTH, range, half, mobAt(0.0D, -2.5D, -3.0D)));
        assertFalse(StoreStrength.isInFan(FEET, HEIGHT, NORTH, range, half, mobAt(0.0D, -3.0D, -3.0D)));
        assertTrue(StoreStrength.isInFan(FEET, HEIGHT, NORTH, range, half, mobAt(0.0D, 3.8D, -3.0D)));
        assertFalse(StoreStrength.isInFan(FEET, HEIGHT, NORTH, range, half, mobAt(0.0D, 3.9D, -3.0D)));
    }

    @Test
    @DisplayName("the level axis drops the vertical and stays zero for a straight-up look")
    void theLevelAxisIsFlat() {
        Vec3 axis = StoreStrength.getLevelAxis(new Vec3(3.0D, 5.0D, 4.0D));
        assertEquals(0.0D, axis.y);
        assertEquals(1.0D, axis.length(), 1.0E-9D);
        assertEquals(Vec3.ZERO, StoreStrength.getLevelAxis(new Vec3(0.0D, 1.0D, 0.0D)));
    }

    @Test
    @DisplayName("a tier past the table keeps the last entry, and one below rank three charges the full four seconds")
    void theChargeTableEndsAtFiftyPercent() {
        assertEquals(40, StoreStrength.getChargeTicks(RANK_THREE_ALL_OUT + 9));
        assertEquals(80, StoreStrength.getChargeTicks(RANK_THREE_ALL_OUT - 1));
    }

    private static AABB boxAround(Vec3 center, double halfWidth, double bottom, double top) {
        return new AABB(center.x - halfWidth, bottom, center.z - halfWidth, center.x + halfWidth, top,
                center.z + halfWidth);
    }

    @Test
    @DisplayName("a mob whose box just touches the reach or the height band is in the fan, a hair past is out")
    void theFanEdgesAreInclusive() {
        double range = RANGE;
        double half = AreaImpact.getHalfAngle(false);
        double height = 2.0D;
        assertTrue(StoreStrength.isInFan(FEET, height, NORTH, range, half,
                boxAround(new Vec3(0.0D, 0.0D, -6.5D), 0.5D, 0.0D, 1.0D)), "centre at range plus half the width");
        assertFalse(StoreStrength.isInFan(FEET, height, NORTH, range, half,
                boxAround(new Vec3(0.0D, 0.0D, -6.51D), 0.5D, 0.0D, 1.0D)));
        assertTrue(StoreStrength.isInFan(FEET, height, NORTH, range, half,
                boxAround(new Vec3(0.0D, 0.0D, -3.0D), 0.5D, -2.0D, -1.0D)), "top one block under the feet");
        assertFalse(StoreStrength.isInFan(FEET, height, NORTH, range, half,
                boxAround(new Vec3(0.0D, 0.0D, -3.0D), 0.5D, -2.0D, -1.01D)));
        assertTrue(StoreStrength.isInFan(FEET, height, NORTH, range, half,
                boxAround(new Vec3(0.0D, 0.0D, -3.0D), 0.5D, 4.0D, 5.0D)), "bottom two blocks over the head");
        assertFalse(StoreStrength.isInFan(FEET, height, NORTH, range, half,
                boxAround(new Vec3(0.0D, 0.0D, -3.0D), 0.5D, 4.01D, 5.0D)));
    }

    @Test
    @DisplayName("a mob standing on the caster is in the fan whichever way the caster looks")
    void aMobOnTheCasterIsAlwaysIn() {
        double range = RANGE;
        double half = AreaImpact.getHalfAngle(false);
        AABB onTop = boxAround(new Vec3(7.0D, 0.0D, -3.0D), 0.3D, 0.0D, 1.8D);
        Vec3 feet = new Vec3(7.0D, 0.0D, -3.0D);
        assertTrue(StoreStrength.isInFan(feet, HEIGHT, NORTH, range, half, onTop));
        assertTrue(StoreStrength.isInFan(feet, HEIGHT, Vec3.ZERO, range, half, onTop));
    }

    @Test
    @DisplayName("the half angle is inclusive and measured from the caster's feet, wherever they stand")
    void theFanAngleIsMeasuredFromTheFeet() {
        double range = RANGE;
        double half = AreaImpact.getHalfAngle(false);
        Vec3 feet = new Vec3(10.0D, 64.0D, -20.0D);
        assertTrue(StoreStrength.isInFan(feet, HEIGHT, NORTH, range, half, mobAt(13.0D, 64.0D, -23.0D)),
                "exactly 45 degrees right of north");
        assertTrue(StoreStrength.isInFan(feet, HEIGHT, NORTH, range, half, mobAt(7.0D, 64.0D, -23.0D)),
                "exactly 45 degrees left of north");
        assertFalse(StoreStrength.isInFan(feet, HEIGHT, NORTH, range, half, mobAt(13.1D, 64.0D, -23.0D)));
        assertFalse(StoreStrength.isInFan(feet, HEIGHT, NORTH, range, half, mobAt(6.9D, 64.0D, -23.0D)));
    }

    @Test
    @DisplayName("the fan follows any level axis, not only the compass ones")
    void theFanFollowsADiagonalAxis() {
        double range = RANGE;
        double half = AreaImpact.getHalfAngle(false);
        Vec3 axis = new Vec3(0.6D, 0.0D, -0.8D);
        Vec3 feet = new Vec3(-4.0D, 10.0D, 5.0D);
        assertTrue(StoreStrength.isInFan(feet, HEIGHT, axis, range, half, mobAt(-2.2D, 10.0D, 2.6D)), "dead ahead");
        assertTrue(StoreStrength.isInFan(feet, HEIGHT, axis, range, half, mobAt(-1.6D, 10.0D, 3.2D)), "16 degrees off");
        assertFalse(StoreStrength.isInFan(feet, HEIGHT, axis, range, half, mobAt(-5.8D, 10.0D, 7.4D)), "behind");
        assertFalse(StoreStrength.isInFan(feet, HEIGHT, axis, range, half, mobAt(-6.4D, 10.0D, 3.2D)),
                "90 degrees off");
        assertFalse(StoreStrength.isInFan(feet, HEIGHT, axis, range, half, mobAt(-1.6D, 10.0D, 6.8D)),
                "90 degrees off the other way");
    }

    @Test
    @DisplayName("a look flatter than a thousandth of a block counts as straight up, anything more has a direction")
    void theLevelAxisHasAFloor() {
        assertEquals(Vec3.ZERO, StoreStrength.getLevelAxis(new Vec3(0.0009D, 1.0D, 0.0D)));
        assertEquals(new Vec3(1.0D, 0.0D, 0.0D), StoreStrength.getLevelAxis(new Vec3(0.0011D, 1.0D, 0.0D)));
    }

    @Test
    @DisplayName("the charge clips ship: half-second hold and strike, 0.2 s cancel, only the strike steps in")
    void theChargeClipsKeepTheirTiming() throws IOException {
        assertEquals(0.5D, getRootKey("charge_hold.json", "time", 0), 1.0E-9D);
        assertEquals(0.5D, getRootKey("charge_strike.json", "time", 0), 1.0E-9D);
        assertEquals(0.2D, getRootKey("charge_cancel.json", "time", 0), 1.0E-9D);
        assertEquals(0.0D, getRootTravel("charge_hold.json"), 1.0E-9D, "the hold travels");
        assertEquals(0.0D, getRootTravel("charge_cancel.json"), 1.0E-9D, "the cancel travels");
        assertEquals(0.384D, getRootTravel("charge_strike.json"), 1.0E-3D);
    }

    private static double getRootTravel(String file) throws IOException {
        JsonArray frames = getRoot(file).getAsJsonArray("transform");
        return getRootKey(file, "transform", ROOT_Y) - frames.get(0).getAsJsonArray().get(ROOT_Y).getAsDouble();
    }

    private static double getRootKey(String file, String track, int index) throws IOException {
        JsonArray keys = getRoot(file).getAsJsonArray(track);
        JsonElement last = keys.get(keys.size() - 1);
        return last.isJsonArray() ? last.getAsJsonArray().get(index).getAsDouble() : last.getAsDouble();
    }

    private static JsonObject getRoot(String file) throws IOException {
        try (InputStream in = StoreStrengthTest.class.getClassLoader().getResourceAsStream(CLIPS + file)) {
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
