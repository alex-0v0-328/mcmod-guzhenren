package net.alex.guzhenren.gameplay.path.strength;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 单项力道增幅记录：运行中状态向力道结算提供的力量点[Strength Points]来源、承受力加成或整拳倍率。
 * <ul>
 *   <li>三个数值各占结算公式的一层：{@code points} 登记为一个来源（来源倍率恒为 1，不过准入、不带承受力、不出兽影），
 *       {@code capacityBonus} 加进当前承受力，{@code punchPercent} 为整拳倍率（{@code 1 + punchPercent / 100}，多项相
 *       乘）；由 {@link StrengthService} 与永久点数并列汇总。</li>
 *   <li>限时兽力[Beast Strength]（{@link #beast}，花豕蛊[Flower Boar Gu]、蛮力天牛蛊[Brute Force Longhorn Beetle Gu]）
 *       指定 {@code family}；大力蛊[Big Strength Gu]只给点数（{@link #points}）；苦力蛊[Hardship Strength Gu]只给整拳
 *       倍率（{@link #punch}）。</li>
 * </ul>
 */

public record StrengthAmplifier(@NotNull ResourceLocation source, @Nullable BeastStrengthFamily family, int points,
                                int capacityBonus, int punchPercent) {

    public static StrengthAmplifier beast(@NotNull ResourceLocation source, @NotNull BeastStrengthFamily family,
                                          int points) {
        return new StrengthAmplifier(source, family, points, 0, 0);
    }

    public static StrengthAmplifier points(@NotNull ResourceLocation source, int points) {
        return new StrengthAmplifier(source, null, points, 0, 0);
    }

    public static StrengthAmplifier punch(@NotNull ResourceLocation source, int punchPercent) {
        return new StrengthAmplifier(source, null, 0, 0, punchPercent);
    }

    public boolean isBeast() { return family != null; }
}
