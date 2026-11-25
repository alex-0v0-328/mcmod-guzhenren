package net.alex.guzhenren.client.particle;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * {@link RingParticle} 的纯角点几何计算工具。
 * <ul>
 *   <li>不引用任何客户端渲染类，可直接通过纯 JVM 单元测试验证；四边形采用双面渲染且与法线点积正负无关，四角点固定按
 *       {@code -u-v, -u+v, +u+v, +u-v} 顺序输出。</li>
 *   <li>{@link #getGround} 生成位于世界 XZ 平面的水平基（{@code u=(1,0,0)}、{@code v=(0,0,1)}），无视镜头朝向，作为无
 *       方向时的回退方案。</li>
 *   <li>{@link #getFacing} 生成垂直于 {@code direction} 的正交基（用于冲刺激波等定向环）：当方向接近竖直时切换参考轴防
 *       止叉乘退化（纯竖直法线退化回水平面，与 {@link #getGround} 一致），长度短于 {@link #MIN_DIRECTION_LENGTH} 视为
 *       无方向。</li>
 *   <li>{@link #scaleForWidth} 依据 {@link #LARGEST_CANVAS}（最大环画布宽）与 {@link #LARGEST_RING_SPAN}（对应世界跨
 *       度，单位：格，默认直径上限约 2.2 格）换算单帧 {@code quadSize} 半跨度倍数；不同跨度的环可另行传入
 *       {@code largestSpan}。</li>
 *   <li>退场计算从末帧完全绽放到彻底消失：{@link #getExitProgress} 在 {@code lastFrameAge}（含 partial tick）处为 0，
 *       经过 {@code lingerTicks + 1} tick 后达到 1；扩散环按进度追加额外缩放，横扫环每 tick 再增长一个帧步长（
 *       {@link #getSweepSize}）；{@link #getFadeAlpha} 在 {@code fadeStart} 之前保持全亮，之后线性淡出。</li>
 *   <li>几乎侧对镜头的环在画面上会缩成不可见的细线，因此将法线朝镜头方向倾斜至最小张角
 *       {@link #MIN_OPENING_DEGREES}（度）：5° 时横向冲刺的大号早期帧仅剩 0.19 格细条导致拖尾像由小变大，30° 时最大帧
 *       侧对仍有约 1.1 格可视宽度。</li>
 *   <li>{@link #tiltTowardCamera} 仅在侧对（|n·v| 小于 sin {@link #MIN_OPENING_DEGREES}）时朝 {@code cameraForward}
 *       旋转法线，保留法线垂直于视线的分量使边界处连续过渡；正对镜头的环（例如向前冲刺）不做偏转。</li>
 * </ul>
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
