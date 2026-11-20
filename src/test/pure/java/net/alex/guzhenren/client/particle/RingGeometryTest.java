package net.alex.guzhenren.client.particle;

import net.alex.guzhenren.particle.RingConeEmitter;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link RingGeometry} 测试：平铺与垂直四边形、镜头倾斜、画布宽度缩放；{@link #FACE_ON} 为远处正对方向，镜头沿环法线
 * 看，永不倾斜；{@link #quadNormal} 由角点算法线 {@code (c1-c0) x (c2-c1)} 再归一化，符号无关。
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

class RingGeometryTest {

    private static final float SIZE = 0.9F;
    private static final float EPS = 1.0E-5F;
    private static final float SIN_MIN_OPENING =
            (float) Math.sin(Math.toRadians(RingGeometry.MIN_OPENING_DEGREES));
    private static final float COS_MIN_OPENING =
            (float) Math.cos(Math.toRadians(RingGeometry.MIN_OPENING_DEGREES));
    private static final Vec3 FACE_ON = new Vec3(0.3D, 0.5D, -0.8D);

    @Test
    @DisplayName("ground quad is world-flat: four corners on y=0 at the size radius")
    void groundQuadIsFlat() {
        Vector3f[] corners = RingGeometry.getGround(SIZE);
        assertEquals(4, corners.length);
        for (Vector3f corner : corners) {
            assertEquals(0.0F, corner.y, EPS);
            assertEquals(SIZE, Math.abs(corner.x), EPS);
            assertEquals(SIZE, Math.abs(corner.z), EPS);
        }
    }

    @Test
    @DisplayName("facing quad is perpendicular to its direction, corners on the size circle")
    void facingQuadFacesTheDirection() {
        Vector3f expected = new Vector3f(0.3F, 0.5F, -0.8F).normalize();
        for (Vector3f corner : RingGeometry.getFacing(FACE_ON, FACE_ON, SIZE)) {
            assertEquals(0.0F, corner.dot(expected), EPS);
            assertEquals(SIZE * (float) Math.sqrt(2.0F), corner.length(), 1.0E-4F);
        }
    }

    @Test
    @DisplayName("a vertical direction never degenerates and lands on a flat ring")
    void verticalDirectionNeverDegenerates() {
        for (Vector3f corner : RingGeometry.getFacing(new Vec3(0.0D, 1.0D, 0.0D),
                new Vec3(0.0D, 1.0D, 0.0D), SIZE)) {
            assertTrue(Float.isFinite(corner.x) && Float.isFinite(corner.y) && Float.isFinite(corner.z));
            assertEquals(0.0F, corner.y, EPS);
        }
    }

    @Test
    @DisplayName("no direction falls back to the ground quad, corner for corner")
    void zeroDirectionFallsBackToGround() {
        Vector3f[] ground = RingGeometry.getGround(SIZE);
        Vector3f[] fallback = RingGeometry.getFacing(Vec3.ZERO, new Vec3(0.0D, 0.0D, 1.0D), SIZE);
        for (int i = 0; i < ground.length; i++) {
            assertEquals(ground[i].x, fallback[i].x, EPS);
            assertEquals(ground[i].y, fallback[i].y, EPS);
            assertEquals(ground[i].z, fallback[i].z, EPS);
        }
    }

    @Test
    @DisplayName("edge-on ring (motion across the view) tilts exactly the minimum opening toward the camera")
    void edgeOnRingTiltsTowardCamera() {
        Vec3 motion = new Vec3(1.0D, 0.0D, 0.0D);
        Vec3 view = new Vec3(0.0D, 0.0D, 1.0D);
        Vector3f normal = quadNormal(RingGeometry.getFacing(motion, view, SIZE));
        assertEquals(SIN_MIN_OPENING, Math.abs(normal.dot(new Vector3f(0.0F, 0.0F, 1.0F))), EPS);
        assertEquals(COS_MIN_OPENING, Math.abs(normal.dot(new Vector3f(1.0F, 0.0F, 0.0F))), EPS);
    }

    @Test
    @DisplayName("a ring already face-on or steep enough is never tilted")
    void faceOnRingIsNotTilted() {
        Vector3f view = new Vector3f(0.0F, 0.0F, 1.0F);
        Vector3f faceOn = quadNormal(RingGeometry.getFacing(new Vec3(0.0D, 0.0D, 1.0D),
                new Vec3(0.0D, 0.0D, 1.0D), SIZE));
        assertEquals(1.0F, Math.abs(faceOn.dot(view)), EPS);
        Vector3f steep = quadNormal(RingGeometry.getFacing(new Vec3(1.0D, 0.0D, 1.0D),
                new Vec3(0.0D, 0.0D, 1.0D), SIZE));
        assertEquals(Math.sqrt(0.5D), Math.abs(steep.dot(view)), 1.0E-4F);
    }

    @Test
    @DisplayName("frame scale follows the canvas width: the 23px ring spans the full 2.2 blocks")
    void frameScaleFollowsCanvasWidth() {
        assertEquals(RingGeometry.LARGEST_RING_SPAN,
                RingGeometry.scaleForWidth(RingGeometry.LARGEST_CANVAS) * 2.0F, EPS);
        float previous = Float.MAX_VALUE;
        for (int width : new int[] { 23, 18, 13, 8, 3 }) {
            float scale = RingGeometry.scaleForWidth(width);
            assertTrue(scale < previous, "scale must shrink with the canvas");
            previous = scale;
        }
    }

    @Test
    @DisplayName("the exit runs from the full bloom to despawn: none before, half way, all of it at the end")
    void exitProgressRunsFromBloomToDespawn() {
        assertEquals(0.0F, RingGeometry.getExitProgress(2.5F, 4, 5), EPS);
        assertEquals(0.0F, RingGeometry.getExitProgress(4.0F, 4, 5), EPS);
        assertEquals(0.5F, RingGeometry.getExitProgress(7.0F, 4, 5), EPS);
        assertEquals(1.0F, RingGeometry.getExitProgress(10.0F, 4, 5), EPS);
        assertEquals(1.0F, RingGeometry.getExitProgress(12.0F, 4, 5), EPS);
        assertEquals(0.25F, RingGeometry.getExitProgress(5.0F, 4, 3), EPS);
    }

    @Test
    @DisplayName("a fade holds full bright until its start, then falls to none at the end")
    void fadeHoldsThenFalls() {
        assertEquals(1.0F, RingGeometry.getFadeAlpha(0.3F, 0.5F), EPS);
        assertEquals(1.0F, RingGeometry.getFadeAlpha(0.5F, 0.5F), EPS);
        assertEquals(0.5F, RingGeometry.getFadeAlpha(0.75F, 0.5F), EPS);
        assertEquals(0.0F, RingGeometry.getFadeAlpha(1.0F, 0.5F), EPS);
        assertEquals(0.75F, RingGeometry.getFadeAlpha(0.25F, 0.0F), EPS);
    }

    @Test
    @DisplayName("a type's own span scales every frame: the sonic boom's 23px ring spans 4 blocks")
    void typeSpanScalesEveryFrame() {
        assertEquals(4.0F, RingGeometry.scaleForWidth(RingGeometry.LARGEST_CANVAS, 4.0F) * 2.0F, EPS);
        assertEquals(RingGeometry.scaleForWidth(13) * 4.0F / RingGeometry.LARGEST_RING_SPAN,
                RingGeometry.scaleForWidth(13, 4.0F), EPS);
    }

    @Test
    @DisplayName("an area damage ring reaches the cone's end the tick its bloom ends")
    void areaDamageRingStopsAsItsBloomEnds() {
        assertEquals(RingParticle.FRAME_COUNT - 1, RingConeEmitter.AREA_DAMAGE_TRAVEL_TICKS);
        assertEquals(4.0D, RingConeEmitter.getAreaDamageRingSpeed(5.0D) * RingConeEmitter.AREA_DAMAGE_TRAVEL_TICKS,
                EPS);
    }

    @Test
    @DisplayName("a sweeping ring keeps its bloom size through the bloom, then grows one frame step a tick")
    void sweepKeepsTheBloomRate() {
        float step = RingGeometry.scaleForWidth(8) - RingGeometry.scaleForWidth(3);
        float bloomEnd = RingGeometry.scaleForWidth(RingGeometry.LARGEST_CANVAS);
        assertEquals(1.0F, RingGeometry.getSweepSize(2.5F, 4, 1.0F, step), EPS);
        assertEquals(bloomEnd, RingGeometry.getSweepSize(4.0F, 4, bloomEnd, step), EPS);
        assertEquals(bloomEnd + 2.5F * step, RingGeometry.getSweepSize(6.5F, 4, bloomEnd, step), EPS);
        assertEquals(RingGeometry.scaleForWidth(13) - RingGeometry.scaleForWidth(8), step, EPS);
    }

    private static Vector3f quadNormal(Vector3f[] corners) {
        Vector3f edge1 = new Vector3f(corners[1]).sub(corners[0]);
        Vector3f edge2 = new Vector3f(corners[2]).sub(corners[1]);
        return edge1.cross(edge2).normalize();
    }
}
