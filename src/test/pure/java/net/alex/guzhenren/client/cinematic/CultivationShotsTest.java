package net.alex.guzhenren.client.cinematic;

import net.alex.guzhenren.client.cinematic.CultivationShots.Pose;
import net.alex.guzhenren.client.cinematic.CultivationShots.Shot;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CultivationShotsTest {

    private static final float EPS = 1.0E-3F;
    private static final float DEGREES_EPS = 0.01F;
    private static final Vec3 ANCHOR = new Vec3(10.0, 64.0, -20.0);
    private static final float FACING = 37.0F;
    private static final double HALF_VERTICAL_FOV = 35.0;

    @Test
    @DisplayName("yaw 0 looks toward +Z and 90 toward -X; pitch is positive looking down")
    void aimingFollowsTheVanillaConvention() {
        assertEquals(0.0F, CultivationShots.getYaw(Vec3.ZERO, new Vec3(0.0, 0.0, 1.0)), EPS);
        assertEquals(90.0F, CultivationShots.getYaw(Vec3.ZERO, new Vec3(-1.0, 0.0, 0.0)), EPS);
        assertEquals(-90.0F, CultivationShots.getYaw(Vec3.ZERO, new Vec3(1.0, 0.0, 0.0)), EPS);
        assertEquals(45.0F, CultivationShots.getPitch(Vec3.ZERO, new Vec3(0.0, -1.0, 1.0)), EPS);
        assertEquals(-45.0F, CultivationShots.getPitch(Vec3.ZERO, new Vec3(0.0, 1.0, 1.0)), EPS);
        assertEquals(FACING, CultivationShots.getYaw(Vec3.ZERO, CultivationShots.getHorizontal(FACING)), DEGREES_EPS);
    }

    @Test
    @DisplayName("the orbit circles 3.5 blocks out and 2.1 up, turning 126 degrees over its 14 seconds")
    void orbitCirclesThePlayer() {
        Pose start = CultivationShots.getPose(Shot.ORBIT, ANCHOR, FACING, Float.NaN, 1, 0.0F);
        Pose end = CultivationShots.getPose(Shot.ORBIT, ANCHOR, FACING, Float.NaN, 1, 1.0F);
        assertEquals(3.5, horizontalDistance(start.eye(), ANCHOR), EPS);
        assertEquals(2.1, start.eye().y - ANCHOR.y, EPS);
        assertEquals(FACING + 30.0F, CultivationShots.getYaw(ANCHOR, start.eye()), DEGREES_EPS);
        assertEquals(FACING + 156.0F - 360.0F, CultivationShots.getYaw(ANCHOR, end.eye()), DEGREES_EPS);
        assertEquals(14 * 20, Shot.ORBIT.ticks);
    }

    @Test
    @DisplayName("one round of shots lasts 56 seconds")
    void oneRoundLasts56Seconds() {
        int ticks = 0;
        for (Shot shot : Shot.values()) ticks += shot.ticks;
        assertEquals(56 * 20, ticks);
        assertEquals(Shot.ORBIT, Shot.ZENITH.getNext());
    }

    @Test
    @DisplayName("every shot keeps the seated player inside the frame")
    void everyShotFramesThePlayer() {
        Vec3 chest = ANCHOR.add(0.0, CultivationShots.CHEST_HEIGHT, 0.0);
        for (Shot shot : Shot.values()) {
            for (float progress : new float[] {0.0F, 0.5F, 1.0F}) {
                Pose pose = CultivationShots.getPose(shot, ANCHOR, FACING, FACING + 90.0F, -1, progress);
                double angle = getAngle(pose.target().subtract(pose.eye()), chest.subtract(pose.eye()));
                assertTrue(angle < HALF_VERTICAL_FOV,
                        shot + " at " + progress + " puts the player " + angle + " deg off");
                assertTrue(pose.eye().distanceTo(chest) >= 1.0, shot + " sits inside the player");
            }
        }
    }

    @Test
    @DisplayName("the vista looks out the open way with the player below the center")
    void vistaLooksOutTheOpenWay() {
        float vistaYaw = FACING + 135.0F;
        Pose pose = CultivationShots.getPose(Shot.VISTA, ANCHOR, FACING, vistaYaw, 1, 0.5F);
        assertEquals(vistaYaw, CultivationShots.getYaw(ANCHOR, pose.target()), DEGREES_EPS);
        assertEquals(vistaYaw, CultivationShots.getYaw(pose.eye(), pose.target()), 6.0F);
        Vec3 chest = ANCHOR.add(0.0, CultivationShots.CHEST_HEIGHT, 0.0);
        assertTrue(CultivationShots.getPitch(pose.eye(), chest) > CultivationShots.getPitch(pose.eye(), pose.target()));
        assertTrue(pose.eye().y - ANCHOR.y >= 3.0);
    }

    @Test
    @DisplayName("the vista picks the farthest of the open directions at 16 blocks or more")
    void vistaPicksTheFarthestOpenDirection() {
        assertEquals(2, CultivationShots.pickVista(new double[] {3.0, 20.0, 48.0, 16.0, 1.0, 1.0, 1.0, 1.0}));
        assertEquals(3, CultivationShots.pickVista(new double[] {3.0, 15.9, 2.0, 16.0, 1.0, 1.0, 1.0, 1.0}));
        assertEquals(-1, CultivationShots.pickVista(new double[] {3.0, 15.9, 2.0, 8.0, 1.0, 1.0, 1.0, 1.0}));
    }

    @Test
    @DisplayName("cuts dip to black over 4 ticks on each side, except into the first shot")
    void cutsDipToBlack() {
        assertEquals(0.0F, CultivationShots.getFade(0.0F, 200, true), EPS);
        assertEquals(1.0F, CultivationShots.getFade(0.0F, 200, false), EPS);
        assertEquals(0.5F, CultivationShots.getFade(2.0F, 200, false), EPS);
        assertEquals(0.0F, CultivationShots.getFade(100.0F, 200, false), EPS);
        assertEquals(0.0F, CultivationShots.getFade(196.0F, 200, false), EPS);
        assertEquals(0.5F, CultivationShots.getFade(198.0F, 200, true), EPS);
        assertEquals(1.0F, CultivationShots.getFade(200.0F, 200, false), EPS);
    }

    @Test
    @DisplayName("the camera eases in over 1 second and the bars slide in over half of it")
    void easesInOverOneSecond() {
        assertEquals(0.0F, CultivationShots.getBlend(0.0F), EPS);
        assertEquals(0.5F, CultivationShots.getBlend(10.0F), EPS);
        assertEquals(1.0F, CultivationShots.getBlend(20.0F), EPS);
        assertEquals(1.0F, CultivationShots.getBars(10.0F), EPS);
        assertEquals(0.5F, CultivationShots.getBars(5.0F), EPS);
    }

    @Test
    @DisplayName("a fresh turn takes the short way round, then keeps going past the back instead of flipping")
    void yawTurnStaysContinuous() {
        assertEquals(-170.0F, CultivationShots.getContinuousTurn(190.0F, Float.NaN), EPS);
        assertEquals(181.0F, CultivationShots.getContinuousTurn(-179.0F, 179.0F), EPS);
        assertEquals(-181.0F, CultivationShots.getContinuousTurn(179.0F, -179.0F), EPS);
        assertEquals(10.0F, CultivationShots.getContinuousTurn(370.0F, 5.0F), EPS);
    }

    private static double horizontalDistance(Vec3 from, Vec3 to) {
        return Math.hypot(from.x - to.x, from.z - to.z);
    }

    private static double getAngle(Vec3 from, Vec3 to) {
        return Math.toDegrees(Math.acos(Math.min(from.normalize().dot(to.normalize()), 1.0)));
    }
}
