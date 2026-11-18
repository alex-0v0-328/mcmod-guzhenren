package net.alex.guzhenren.gameplay.path.strength;

import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * 横冲系冲撞起手瞬间的输入状态快照：汇总常态可发挥力量点[Strength Points]（{@link StrengthSnapshot#settledPoints}，不含
 * 全面解放，保证双端数值一致）、两猪之力、兽力冲刺距离与速度加成以及全力以赴状态，并委派给 {@link CrashDash}、
 * {@link CrashBreach}、{@link CrashStrike} 与 {@link AreaImpact} 计算各项派生数值。
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
