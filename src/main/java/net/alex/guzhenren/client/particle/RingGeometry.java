package net.alex.guzhenren.client.particle;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * {@link RingParticle} 的纯角点几何。
 *
 * <ul>
 *   <li>不引客户端类，纯 JVM 可测；四边形双面，点积符号无关；角点顺序均为 {@code -u-v, -u+v, +u+v, +u-v}。</li>
 *   <li>{@link #getGround}：世界 XZ 平面，{@code u=(1,0,0)}、{@code v=(0,0,1)}，不看镜头，无方向时回退用。</li>
 *   <li>{@link #getFacing}：环面朝 {@code direction}（如冲刺激波），正交基横跨该方向；（近）竖直时换参考轴防退化，竖直
 *       法线落在水平面，同 {@link #getGround}；短于 {@link #MIN_DIRECTION_LENGTH} 为无方向。</li>
 *   <li>{@link #scaleForWidth}：由 {@link #LARGEST_CANVAS}（最大环画布宽）与 {@link #LARGEST_RING_SPAN}（其世界跨度，
 *       格；总表 TODO 验收直径最大约 2.2 格）得帧 {@code quadSize} 半跨度倍数；跨度不同者另传
 *       {@code largestSpan}。</li>
 *   <li>退场自完全绽放至消失：{@link #getExitProgress} 在 {@code lastFrameAge}（含 partial tick）为 0，
 *       {@code lingerTicks + 1} tick 后为 1；扩散环按进度长出额外尺寸，横扫环改为每 tick 再长一个帧步长
 *       （{@link #getSweepSize}）；{@link #getFadeAlpha} 到 {@code fadeStart} 前全亮，之后淡出。</li>
 *   <li>横穿视野的环侧对镜头、不可见，故法线朝镜头倾斜到张角 {@link #MIN_OPENING_DEGREES}（度）：5° 时横向冲刺的大号早
 *       期帧只剩 0.19 格细条，拖尾读成由小到大；30° 时最大帧侧对也约 1.1 格宽。</li>
 *   <li>仅侧对时向 {@code cameraForward} 倾斜：{@link #tiltTowardCamera} 只在 |n·v| 小于 sin
 *       {@link #MIN_OPENING_DEGREES} 时旋转，保留法线垂直视线分量，张角恰为阈值、连续；正对的环（向前冲刺）不调。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see RingParticle
 * @since 1.0.0
 */

final class RingGeometry {

    private RingGeometry() {}

    static final int LARGEST_CANVAS = 23;
    static final float LARGEST_RING_SPAN = 2.2F;
    private static final float MIN_DIRECTION_LENGTH = 1.0E-4F;
    static final float MIN_OPENING_DEGREES = 30.0F;
    private static final float SIN_MIN_OPENING = (float) Math.sin(Math.toRadians(MIN_OPENING_DEGREES));
    private static final float COS_MIN_OPENING = (float) Math.cos(Math.toRadians(MIN_OPENING_DEGREES));
    private static final Vector3f UP = new Vector3f(0.0F, 1.0F, 0.0F);
    private static final Vector3f FALLBACK_REFERENCE = new Vector3f(1.0F, 0.0F, 0.0F);

    static float scaleForWidth(int canvasWidth) {
        return scaleForWidth(canvasWidth, LARGEST_RING_SPAN);
    }

    static float scaleForWidth(int canvasWidth, float largestSpan) {
        return largestSpan * canvasWidth / (2.0F * LARGEST_CANVAS);
    }

    static float getExitProgress(float age, int lastFrameAge, int lingerTicks) {
        return Math.clamp((age - lastFrameAge) / (lingerTicks + 1), 0.0F, 1.0F);
    }

    static float getSweepSize(float age, int lastFrameAge, float bloomSize, float step) {
        return bloomSize + step * Math.max(0.0F, age - lastFrameAge);
    }

    static float getFadeAlpha(float progress, float fadeStart) {
        if (progress <= fadeStart) return 1.0F;
        return Math.max(0.0F, 1.0F - (progress - fadeStart) / (1.0F - fadeStart));
    }

    static Vector3f[] getGround(float size) {
        return getCorners(new Vector3f(size, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, size));
    }

    static Vector3f[] getFacing(Vec3 direction, Vec3 cameraForward, float size) {
        Vector3f normal = new Vector3f((float) direction.x(), (float) direction.y(), (float) direction.z());
        if (normal.length() < MIN_DIRECTION_LENGTH) return getGround(size);
        normal.normalize();
        normal.set(tiltTowardCamera(normal, cameraForward));

        Vector3f reference = Math.abs(normal.y) > 0.99F ? FALLBACK_REFERENCE : UP;
        Vector3f u = new Vector3f(normal).cross(reference).normalize().mul(size);
        Vector3f v = new Vector3f(normal).cross(u).normalize().mul(size);
        return getCorners(u, v);
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

    private static Vector3f[] getCorners(Vector3f u, Vector3f v) {
        return new Vector3f[] {
                new Vector3f(u).negate().sub(v), new Vector3f(u).negate().add(v),
                new Vector3f(u).add(v), new Vector3f(u).sub(v) };
    }
}
