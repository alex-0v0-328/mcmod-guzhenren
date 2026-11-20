package net.alex.guzhenren.gameplay.path.strength;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 单项力道增幅记录：运行中状态向力道结算提供的力量点[Strength Points]来源或伤害倍率。
 * <ul>
 *   <li>两个数值各占结算公式的一层：{@code points} 登记为一个来源（不过准入、不带承受力、不出兽影），
 *       {@code damagePercent} 为伤害倍率（{@code 1 + damagePercent / 100}，多项相乘）；由 {@link StrengthService} 与永
 *       久点数并列汇总。</li>
 *   <li>限时兽力[Beast Strength]（{@link #beast}，花豕蛊[Flower Boar Gu]、蛮力天牛蛊[Brute Force Longhorn Beetle Gu]）
 *       指定 {@code family}；大力蛊[Big Strength Gu]只给点数（{@link #points}）；苦力蛊[Hardship Strength Gu]只给伤害
 *       倍率（{@link #damage}）。</li>
 * </ul>
 */

public record StrengthAmplifier(@NotNull ResourceLocation source, @Nullable BeastStrengthFamily family, int points,
                                int damagePercent) {

    public static StrengthAmplifier beast(@NotNull ResourceLocation source, @NotNull BeastStrengthFamily family,
                                          int points) {
        return new StrengthAmplifier(source, family, points, 0);
    }

    public static StrengthAmplifier points(@NotNull ResourceLocation source, int points) {
        return new StrengthAmplifier(source, null, points, 0);
    }

    public static StrengthAmplifier damage(@NotNull ResourceLocation source, int damagePercent) {
        return new StrengthAmplifier(source, null, 0, damagePercent);
    }

    public boolean isBeast() { return family != null; }
}
