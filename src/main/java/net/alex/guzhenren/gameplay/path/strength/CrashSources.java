package net.alex.guzhenren.gameplay.path.strength;

import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * 横冲系冲撞此刻的四个来源：发挥点数、两猪之力、兽力冲刺加成（驰马）、全力以赴。
 * <ul>
 *   <li>{@link #of} 两端各读：客户端算位移与播放速度（Epic Fight 只在所属客户端移动玩家），服务端算清路与终点打击；
 *       点数取 {@link StrengthSnapshot#settledPoints}，不含解放，两端同值。</li>
 *   <li>两猪之力加距离、破坏硬度与打击伤害；驰马加距离与播放速度；全力以赴三者都加；人力钧力流只有点数与全力以赴。</li>
 *   <li>派生值只转交 {@link CrashDash}、{@link CrashBreach#getMaxHardness}、{@link CrashStrike#getDamageRatio}、
 *       {@link AreaImpact#getRange}，本类不存公式。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see CrashStrike
 * @since 1.0.0
 */

public record CrashSources(int points, boolean twoBoars, double beastBonus, double beastSpeedBonus, boolean allOut) {

    public static @NotNull CrashSources of(@NotNull Player player) {
        StrengthSnapshot snapshot = StrengthService.snapshot(player);
        PathStrengthData data = PathStrengthService.get(player);
        return new CrashSources(snapshot.settledPoints(), data.hasTwoBoars(), data.getBeastDashBonus(),
                data.getBeastDashSpeedBonus(), snapshot.allOut());
    }

    public double getScale() { return CrashDash.getScale(CrashDash.getMultiple(points, twoBoars, beastBonus, allOut)); }

    public double getTravel() { return CrashDash.getTravel(getScale()); }

    public double getPlaySpeed() { return CrashDash.getPlaySpeed(beastSpeedBonus, allOut); }

    public float getMaxHardness() { return CrashBreach.getMaxHardness(points, twoBoars, allOut); }

    public float getDamageRatio() { return CrashStrike.getDamageRatio(twoBoars, allOut); }

    public double getStrikeRange() { return AreaImpact.getRange(points); }
}
