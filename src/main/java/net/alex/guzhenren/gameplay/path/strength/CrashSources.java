package net.alex.guzhenren.gameplay.path.strength;

import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * 横冲系冲撞起手瞬间的输入状态快照：汇总总力量点[Strength Points]（{@link StrengthSnapshot#totalPoints}，不吃暴击、蓄
 * 力与苦力倍率，保证双端数值一致）、两猪之力与兽力冲刺距离、速度加成，并委派给 {@link CrashDash}、{@link CrashBreach}、
 * {@link CrashStrike} 与 {@link AreaDamage} 计算各项派生数值；全力以赴不影响冲撞。
 */

public record CrashSources(int points, boolean twoBoars, double beastBonus, double beastSpeedBonus) {

    public static @NotNull CrashSources of(@NotNull Player player) {
        StrengthSnapshot snapshot = StrengthService.snapshot(player);
        PathStrengthData data = PathStrengthService.get(player);
        return new CrashSources(snapshot.totalPoints(), data.hasTwoBoars(), data.getBeastDashBonus(),
                data.getBeastDashSpeedBonus());
    }

    public double getScale() { return CrashDash.getScale(CrashDash.getMultiple(points, twoBoars, beastBonus)); }

    public double getTravel() { return CrashDash.getTravel(getScale()); }

    public double getPlaySpeed() { return CrashDash.getPlaySpeed(beastSpeedBonus); }

    public float getMaxHardness() { return CrashBreach.getMaxHardness(points, twoBoars); }

    public float getDamageRatio() { return CrashStrike.getDamageRatio(twoBoars); }

    public double getStrikeRange() { return AreaDamage.getRange(points); }
}
