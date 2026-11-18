package net.alex.guzhenren.client.cinematic;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * 温养[Nourish]运镜的机位几何与时间曲线计算器：全为纯函数，供 {@link CultivationCamera} 每帧取用。
 * <ul>
 *   <li>{@link Shot} 按声明顺序循环（环绕、远眺、侧推、仰望、天顶），完整一轮耗时 56 秒；每个机位输出镜头位置与注视点（
 *       {@link Pose}），朝向角由两点算出（{@link #getYaw} 与 {@link #getPitch} 沿用原版约定：偏航 0 朝 +Z、90 朝 -X，俯
 *       仰正值朝下）。</li>
 *   <li>机位锚点取盘坐玩家的脚底坐标，基准朝向取开始温养时的偏航角；盘坐姿态仅改变动画高度而非碰撞箱，因此头、胸高度固
 *       定取 {@link #HEAD_HEIGHT} 与 {@link #CHEST_HEIGHT}，不读取碰撞箱的站立视线高度。</li>
 *   <li>远眺朝 {@link #pickVista} 选定的方向展开：在 {@link #VISTA_DIRECTIONS} 个候选中挑选可视距离最远且不短于
 *       {@link #VISTA_MIN_REACH} 格的方向，将镜头置于玩家身后偏侧使人像落在画面下三分之一、远景居于中央。</li>
 *   <li>切镜前后各用 {@link #FADE_TICKS} tick 淡入黑屏（{@link #getFade}）；首个机位不做切镜淡入，而是由进出过渡（
 *       {@link #getBlend}，时长 {@link #BLEND_TICKS}）接管，上下黑边滑入滑出进度由 {@link #getBars}（时长
 *       {@link #BAR_TICKS}）控制。</li>
 * </ul>
 */

final class CultivationShots {

    private CultivationShots() {}

    static final double HEAD_HEIGHT = 1.1;
    static final double CHEST_HEIGHT = 0.75;
    static final int BLEND_TICKS = 20;
    static final int BAR_TICKS = 10;
    static final int FADE_TICKS = 4;
    static final int VISTA_DIRECTIONS = 8;
    static final double VISTA_MIN_REACH = 16.0;
    private static final double ORBIT_RADIUS = 3.5;
    private static final double ORBIT_HEIGHT = HEAD_HEIGHT + 1.0;
    private static final float ORBIT_START = 30.0F;
    private static final float ORBIT_DEGREES_PER_TICK = 0.45F;
    private static final double VISTA_NEAR = 6.0;
    private static final double VISTA_FAR = 8.0;
    private static final double VISTA_LOW = 3.0;
    private static final double VISTA_HIGH = 5.0;
    private static final double VISTA_SIDE = 1.5;
    private static final double VISTA_AHEAD = 12.0;
    private static final double DOLLY_DISTANCE = 2.5;
    private static final double DOLLY_TRAVEL = 0.75;
    private static final double DOLLY_DROP = 0.2;
    private static final double RISE_NEAR = 1.8;
    private static final double RISE_FAR = 2.2;
    private static final double RISE_SIDE = 0.6;
    private static final double RISE_HEIGHT = 0.3;
    private static final double RISE_LIFT = 0.1;
    private static final double ZENITH_HEIGHT = 6.0;
    private static final double ZENITH_BACK = 0.6;
    private static final float ZENITH_TURN = 15.0F;

    enum Shot {
        ORBIT(280), VISTA(240), DOLLY(200), RISE(200), ZENITH(200);

        final int ticks;

        Shot(int ticks) { this.ticks = ticks; }

        Shot getNext() { return values()[(ordinal() + 1) % values().length]; }
    }

    record Pose(Vec3 eye, Vec3 target) {}

    static Pose getPose(Shot shot, Vec3 anchor, float facing, float vistaYaw, int side, float progress) {
        float clamped = Mth.clamp(progress, 0.0F, 1.0F);
        return switch (shot) {
            case ORBIT -> getOrbit(anchor, facing, clamped * shot.ticks);
            case VISTA -> getVista(anchor, vistaYaw, clamped);
            case DOLLY -> getDolly(anchor, facing, side, clamped);
            case RISE -> getRise(anchor, facing, clamped);
            case ZENITH -> getZenith(anchor, facing, clamped);
        };
    }

    //region shots [机位]
    private static Pose getOrbit(Vec3 anchor, float facing, float ticks) {
        Vec3 eye = anchor.add(getHorizontal(facing + ORBIT_START + ticks * ORBIT_DEGREES_PER_TICK).scale(ORBIT_RADIUS))
                .add(0.0, ORBIT_HEIGHT, 0.0);
        return new Pose(eye, anchor.add(0.0, CHEST_HEIGHT, 0.0));
    }

    private static Pose getVista(Vec3 anchor, float vistaYaw, float progress) {
        Vec3 ahead = getHorizontal(vistaYaw);
        Vec3 eye = anchor.subtract(ahead.scale(Mth.lerp(progress, VISTA_NEAR, VISTA_FAR)))
                .add(getHorizontal(vistaYaw + 90.0F).scale(VISTA_SIDE))
                .add(0.0, Mth.lerp(progress, VISTA_LOW, VISTA_HIGH), 0.0);
        return new Pose(eye, anchor.add(ahead.scale(VISTA_AHEAD)).add(0.0, HEAD_HEIGHT, 0.0));
    }

    private static Pose getDolly(Vec3 anchor, float facing, int side, float progress) {
        Vec3 eye = anchor.add(getHorizontal(facing + 90.0F * side).scale(DOLLY_DISTANCE))
                .add(getHorizontal(facing).scale(Mth.lerp(progress, -DOLLY_TRAVEL, DOLLY_TRAVEL)))
                .add(0.0, HEAD_HEIGHT, 0.0);
        return new Pose(eye, anchor.add(0.0, HEAD_HEIGHT - DOLLY_DROP, 0.0));
    }

    private static Pose getRise(Vec3 anchor, float facing, float progress) {
        Vec3 eye = anchor.add(getHorizontal(facing).scale(Mth.lerp(progress, RISE_FAR, RISE_NEAR)))
                .add(getHorizontal(facing + 90.0F).scale(RISE_SIDE))
                .add(0.0, RISE_HEIGHT, 0.0);
        return new Pose(eye, anchor.add(0.0, HEAD_HEIGHT + RISE_LIFT, 0.0));
    }

    private static Pose getZenith(Vec3 anchor, float facing, float progress) {
        Vec3 eye = anchor.subtract(getHorizontal(facing + ZENITH_TURN * progress).scale(ZENITH_BACK))
                .add(0.0, ZENITH_HEIGHT, 0.0);
        return new Pose(eye, anchor.add(0.0, CHEST_HEIGHT, 0.0));
    }
    //endregion

    //region aiming [朝向]
    static Vec3 getHorizontal(float yaw) {
        float radians = yaw * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(radians), 0.0, Mth.cos(radians));
    }

    static float getYaw(Vec3 from, Vec3 to) {
        return (float) (Mth.atan2(from.x - to.x, to.z - from.z) * Mth.RAD_TO_DEG);
    }

    static float getPitch(Vec3 from, Vec3 to) {
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        return (float) (Mth.atan2(from.y - to.y, Math.sqrt(dx * dx + dz * dz)) * Mth.RAD_TO_DEG);
    }

    static float getContinuousTurn(float turn, float previous) {
        if (Float.isNaN(previous)) return Mth.wrapDegrees(turn);
        return previous + Mth.wrapDegrees(turn - previous);
    }

    static int pickVista(double[] reaches) {
        int best = -1;
        for (int i = 0; i < reaches.length; i++) {
            if (reaches[i] >= VISTA_MIN_REACH && (best < 0 || reaches[i] > reaches[best])) best = i;
        }
        return best;
    }
    //endregion

    //region timing [时间曲线]
    static float getBlend(float ticks) { return getSmooth(ticks / BLEND_TICKS); }

    static float getBars(float ticks) { return getSmooth(ticks / BAR_TICKS); }

    static float getFade(float ticks, int length, boolean first) {
        if (!first && ticks < FADE_TICKS) return 1.0F - Math.max(ticks, 0.0F) / FADE_TICKS;
        return Mth.clamp((ticks - (length - FADE_TICKS)) / FADE_TICKS, 0.0F, 1.0F);
    }

    private static float getSmooth(float value) {
        float clamped = Mth.clamp(value, 0.0F, 1.0F);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }
    //endregion
}
