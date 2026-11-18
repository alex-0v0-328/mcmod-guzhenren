package net.alex.guzhenren.gameplay.path.strength;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 单项临时力道增幅记录：承载运行中状态提供的临时力量点[Strength Points]与临时承受力加成。
 * <ul>
 *   <li>通过 {@code source} 与永久力道记录区分，由 {@link StrengthService} 与永久点数并列汇总（不计入全面解放基数，也不
 *       计入永久兽力准入）。</li>
 *   <li>限时兽力[Beast Strength]（{@link #beast}，如花豕蛊[Flower Boar Gu]、
 *       蛮力天牛蛊[Brute Force Longhorn Beetle Gu]）指定 {@code family} 且 {@code capacityBonus == 0}，超出当前承受力的
 *       部分全额发挥并触发 {@link BodyOverload} 肉体过载；苦力蛊等状态则仅提供 {@code capacityBonus}。</li>
 * </ul>
 */

public record StrengthAmplifier(@NotNull ResourceLocation source, @Nullable BeastStrengthFamily family, int points,
                                int capacityBonus) {

    public static StrengthAmplifier beast(@NotNull ResourceLocation source, @NotNull BeastStrengthFamily family,
                                          int points) {
        return new StrengthAmplifier(source, family, points, 0);
    }

    public boolean isBeast() { return family != null; }
}
