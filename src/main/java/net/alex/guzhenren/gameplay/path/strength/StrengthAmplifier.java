package net.alex.guzhenren.gameplay.path.strength;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 一项临时力量增幅：运行中状态加的力量点[Strength Points]与承受力。以 {@code source} 与永久记录区分；
 * 限时兽力[Beast Strength]（花豕蛊[Flower Boar Gu]、蛮力天牛蛊[Brute Force Longhorn Beetle Gu]）设兽种、无承受力，
 * 超出即肉体过载（{@code BodyOverload}）；苦力蛊[Hardship Strength Gu]只加承受力；不进全面解放基数，不计永久兽力准入；
 * {@code StrengthService} 与永久点数并列求和。
 *
 * @author Alex
 * @version 1.0.0
 * @see StrengthAmplifierProvider
 * @since 1.0.0
 */

public record StrengthAmplifier(@NotNull ResourceLocation source, @Nullable BeastStrengthFamily family, int points,
                                int capacityBonus) {

    /** 限时兽力[Beast Strength]：只有点数，无自身承受力。 */
    public static StrengthAmplifier beast(@NotNull ResourceLocation source, @NotNull BeastStrengthFamily family,
                                          int points) {
        return new StrengthAmplifier(source, family, points, 0);
    }

    public boolean isBeast() { return family != null; }
}
