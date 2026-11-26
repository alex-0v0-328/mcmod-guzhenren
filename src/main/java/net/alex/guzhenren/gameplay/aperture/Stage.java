package net.alex.guzhenren.gameplay.aperture;

import com.mojang.serialization.Codec;
import java.util.Arrays;
import net.alex.guzhenren.core.NamedEnum;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;

/**
 * 一转内的小境界阶段枚举：定义各阶段的真元上限倍率（{@link #getEssenceMultiplier}）。
 * <ul>
 *   <li>封闭词汇枚举，兄弟模组不得新增；倍率定义在此，真元上限统一由 {@link Aperture#computeMaxEssence} 计算；
 *       {@link #NONE} 不可直接设置，{@link #shift} 夹取在 {@link #INITIAL} 至 {@link #PEAK} 之间。</li>
 *   <li>⚠ 调用方切勿在外部重复硬编码阶段倍率；{@link #NONE} 的倍率 {@code 0} 代表凡人空真元池，而非异常兜底值。</li>
 * </ul>
 */

public enum Stage implements NamedEnum {

    NONE(0),
    INITIAL(1),
    MIDDLE(2),
    UPPER(4),
    PEAK(8);

    public static final Codec<Stage> CODEC = StringRepresentable.fromEnum(Stage::values);
    public static final Stage LOWEST = INITIAL;
    public static final Stage HIGHEST = PEAK;
    private final int essenceMultiplier;

    Stage(int essenceMultiplier) {
        this.essenceMultiplier = essenceMultiplier;
    }

    public int getEssenceMultiplier() { return essenceMultiplier; }

    public Stage shift(int delta) {
        return values()[Mth.clamp(ordinal() + delta, LOWEST.ordinal(), HIGHEST.ordinal())];
    }

    public static Stage[] getSettable() {
        return Arrays.copyOfRange(values(), LOWEST.ordinal(), HIGHEST.ordinal() + 1);
    }

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.aperture.stage."; }
}
