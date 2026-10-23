package net.alex.guzhenren.client.particle;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * The pure corner geometry behind {@link RingParticle}, kept free of Minecraft client classes so
 * the pure JVM tests can pin it. Corner order is shared by both orientations:
 * {@code -u-v, -u+v, +u+v, +u-v}, where GROUND takes u=(1,0,0), v=(0,0,1) (a world-flat XZ quad,
 * built by {@link #ground}, no camera involvement so it stays horizontal) and FACING_MOTION takes
 * an orthonormal basis of the plane perpendicular to the spawn direction ({@link #facing}).
 *
 * <p>{@link #LARGEST_CANVAS} is the canvas width of the largest hand-drawn ring; every frame's
 * world size scales off it through {@link #scaleForWidth} (the {@code quadSize}
 * half-span multiplier for a frame drawn on a canvas that wide). A direction shorter than
 * {@link #MIN_DIRECTION_LENGTH} is treated as "no direction" and falls back to GROUND.
 *
 * <p>{@link #MIN_OPENING_DEGREES} is the minimum opening toward the camera, in degrees: a ring
 * perpendicular to a motion that runs sideways across the view is perfectly
 * edge-on and invisible, so the normal is tilted toward the camera enough to keep the arc legible.
 * At 5° the big early frames of a sideways dash render as a
 * 0.19-block sliver and the trail reads small-to-large; 30° keeps the largest frame ~1.1 blocks
 * wide even edge-on. Rings already facing the viewer (punch, forward dash) sit far above the
 * threshold and are never touched.
 *
 * <p>{@link #facing} builds the quad perpendicular to {@code direction} (the ring faces along it,
 * like a shockwave riding a dash or a punch), applying the {@link #MIN_OPENING_DEGREES} tilt
 * toward {@code cameraForward} when the ring would otherwise be edge-on. A near-zero direction
 * falls back to {@link #ground(float)}; a (near-)vertical one switches the reference axis so the
 * basis never degenerates -- note a vertical normal lands on the horizontal plane anyway, matching
 * GROUND. {@link #tiltTowardCamera} rotates the normal toward the camera only while it is nearly
 * perpendicular to the view (|n·v| under sin of {@link #MIN_OPENING_DEGREES} -- the edge-on case):
 * the result keeps the normal's view-perpendicular component and gains exactly
 * {@link #MIN_OPENING_DEGREES} of opening, so the adjustment is continuous and never pops. The
 * quad is double-sided, so the sign of the dot is irrelevant.
 *
 * @author Alex
 * @version 1.0.0
 * @see RingParticle
 * @since 1.0.0
 */

final class RingGeometry {

    private RingGeometry() {}

    static final int LARGEST_CANVAS = 23;
    /** World span of the largest ring, in blocks (总表 TODO acceptance: max ~2.2 blocks across). */
    static final float LARGEST_RING_SPAN = 2.2F;
    private static final float MIN_DIRECTION_LENGTH = 1.0E-4F;
    static final float MIN_OPENING_DEGREES = 30.0F;
    private static final float SIN_MIN_OPENING = (float) Math.sin(Math.toRadians(MIN_OPENING_DEGREES));
    private static final float COS_MIN_OPENING = (float) Math.cos(Math.toRadians(MIN_OPENING_DEGREES));
    private static final Vector3f UP = new Vector3f(0.0F, 1.0F, 0.0F);
    private static final Vector3f FALLBACK_REFERENCE = new Vector3f(1.0F, 0.0F, 0.0F);

    static float scaleForWidth(int canvasWidth) {
        return LARGEST_RING_SPAN * canvasWidth / (2.0F * LARGEST_CANVAS);
    }

    static Vector3f[] ground(float size) {
        return corners(new Vector3f(size, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, size));
    }

    static Vector3f[] facing(Vec3 direction, Vec3 cameraForward, float size) {
        Vector3f normal = new Vector3f((float) direction.x(), (float) direction.y(), (float) direction.z());
        if (normal.length() < MIN_DIRECTION_LENGTH) return ground(size);
        normal.normalize();
        normal.set(tiltTowardCamera(normal, cameraForward));

        Vector3f reference = Math.abs(normal.y) > 0.99F ? FALLBACK_REFERENCE : UP;
        Vector3f u = new Vector3f(normal).cross(reference).normalize().mul(size);
        Vector3f v = new Vector3f(normal).cross(u).normalize().mul(size);
        return corners(u, v);
    }

    private static Vector3f tiltTowardCamera(Vector3f normal, Vec3 cameraForward) {
        Vector3f view = new Vector3f((float) cameraForward.x(), (float) cameraForward.y(), (float) cameraForward.z());
        if (view.length() < MIN_DIRECTION_LENGTH) return normal;
        view.normalize();

        float alignment = normal.dot(view);
        if (Math.abs(alignment) >= SIN_MIN_OPENING) return normal;

        Vector3f sideways = new Vector3f(normal).sub(new Vector3f(view).mul(alignment)).normalize();
        return new Vector3f(view).mul(SIN_MIN_OPENING).add(sideways.mul(COS_MIN_OPENING));
    }

    private static Vector3f[] corners(Vector3f u, Vector3f v) {
        return new Vector3f[] {
                new Vector3f(u).negate().sub(v), new Vector3f(u).negate().add(v),
                new Vector3f(u).add(v), new Vector3f(u).sub(v) };
    }
}
