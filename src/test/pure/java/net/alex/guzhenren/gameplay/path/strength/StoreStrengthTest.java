package net.alex.guzhenren.gameplay.path.strength;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.minecraft.util.RandomSource;
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
    private static final List<BeastStrengthFamily> THREE =
            List.of(BeastStrengthFamily.BOAR, BeastStrengthFamily.BEAR, BeastStrengthFamily.TIGER);
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
    @DisplayName("released families count twelve times, every other point once, with no capacity cut")
    void theMaxPointsReleaseTwelveTimes() {
        assertEquals(480, StoreStrength.getMaxPoints(40, 40), "a bear of 40 fully released");
        assertEquals(200, StoreStrength.getMaxPoints(200, 0), "human jin count once");
        assertEquals(504, StoreStrength.getMaxPoints(64, 40), "a bear released beside a flower boar of 24");
        assertEquals(Integer.MAX_VALUE, StoreStrength.getMaxPoints(Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

    @Test
    @DisplayName("the backlash is the points past the current capacity")
    void theBacklashIsThePointsPastTheCapacity() {
        assertEquals(424, StoreStrength.getBacklashPoints(480, 56));
        assertEquals(0, StoreStrength.getBacklashPoints(200, 224));
        assertEquals(11.43F, BodyOverload.getRecoil(64, 56, 20.0F), 0.01F);
    }

    @Test
    @DisplayName("one family is released at random, every held family under Hardship Strength")
    void hardshipReleasesEveryFamily() {
        RandomSource random = RandomSource.create(7L);
        assertEquals(1, StoreStrength.getReleasedFamilies(THREE, false, random).size());
        assertEquals(THREE, StoreStrength.getReleasedFamilies(THREE, true, random));
        assertTrue(StoreStrength.getReleasedFamilies(List.of(), false, random).isEmpty());
    }

    @Test
    @DisplayName("the range grows with the root of the max points from 4 blocks, half again when widened, up to 16")
    void rangeGrowsWithTheRootOfTheMaxPoints() {
        assertEquals(4.0D, StoreStrength.getRange(0, false), 1.0E-9D);
        assertEquals(10.0D, StoreStrength.getRange(400, false), 1.0E-9D);
        assertEquals(15.0D, StoreStrength.getRange(400, true), 1.0E-9D);
        assertEquals(StoreStrength.MAX_RANGE, StoreStrength.getRange(900, true), 1.0E-9D);
        assertEquals(StoreStrength.MAX_RANGE, StoreStrength.getRange(10_000, false), 1.0E-9D);
        assertEquals(5.0D, StoreStrength.getBurstRadius(400, false), 1.0E-9D);
        assertEquals(StoreStrength.MAX_RANGE / 2.0D, StoreStrength.getBurstRadius(10_000, true), 1.0E-9D);
    }

    @Test
    @DisplayName("a six-block fan reaches 45 degrees either side, a nine-block one 135 when widened")
    void theFanWidensUnderHardship() {
        double range = RANGE;
        double half = StoreStrength.getHalfAngle(false);
        assertTrue(StoreStrength.isInFan(FEET, HEIGHT, NORTH, range, half, mobAt(0.0D, 0.0D, -5.0D)));
        assertFalse(StoreStrength.isInFan(FEET, HEIGHT, NORTH, range, half, mobAt(0.0D, 0.0D, -7.0D)));
        assertTrue(StoreStrength.isInFan(FEET, HEIGHT, NORTH, range, half, mobAt(2.0D, 0.0D, -3.0D)));
        assertFalse(StoreStrength.isInFan(FEET, HEIGHT, NORTH, range, half, mobAt(3.0D, 0.0D, 0.0D)));

        double wide = WIDE_RANGE;
        double wideHalf = StoreStrength.getHalfAngle(true);
        assertTrue(StoreStrength.isInFan(FEET, HEIGHT, NORTH, wide, wideHalf, mobAt(3.0D, 0.0D, 0.0D)));
        assertTrue(StoreStrength.isInFan(FEET, HEIGHT, NORTH, wide, wideHalf, mobAt(0.0D, 0.0D, -8.5D)));
        assertFalse(StoreStrength.isInFan(FEET, HEIGHT, NORTH, wide, wideHalf, mobAt(0.0D, 0.0D, 3.0D)));
    }

    @Test
    @DisplayName("the fan spans from a block under the feet to two over the head")
    void theFanHasAHeightBand() {
        double range = RANGE;
        double half = StoreStrength.getHalfAngle(false);
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
        double half = StoreStrength.getHalfAngle(false);
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
        double half = StoreStrength.getHalfAngle(false);
        AABB onTop = boxAround(new Vec3(7.0D, 0.0D, -3.0D), 0.3D, 0.0D, 1.8D);
        Vec3 feet = new Vec3(7.0D, 0.0D, -3.0D);
        assertTrue(StoreStrength.isInFan(feet, HEIGHT, NORTH, range, half, onTop));
        assertTrue(StoreStrength.isInFan(feet, HEIGHT, Vec3.ZERO, range, half, onTop));
    }

    @Test
    @DisplayName("the half angle is inclusive and measured from the caster's feet, wherever they stand")
    void theFanAngleIsMeasuredFromTheFeet() {
        double range = RANGE;
        double half = StoreStrength.getHalfAngle(false);
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
        double half = StoreStrength.getHalfAngle(false);
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
