package net.alex.guzhenren.gameplay.aperture;

import com.mojang.serialization.Codec;
import java.util.Arrays;
import net.alex.guzhenren.core.NamedEnum;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;

/**
 * 阶段：一转内的分级及其真元[Essence]上限倍率。
 *
 * <ul>
 *   <li>封闭词汇枚举，兄弟模组不得新增；倍率在此、公式在空窍[Aperture] record 上，上限只有一个表达式；{@code NONE} 不可
 *       设，{@code shift(int)} 夹在 {@code INIT} 至 {@code PEAK}。</li>
 *   <li>⚠ 第二处相乘就是第二个答案，勿在调用处重复声明倍率；{@code NONE} 的倍率 {@code 0} 是凡人空池，不是兜底。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public enum Stage implements NamedEnum {

    NONE(0),
    INIT(1),
    MIDDLE(2),
    UPPER(4),
    PEAK(8);

    public static final Codec<Stage> CODEC = StringRepresentable.fromEnum(Stage::values);
    public static final Stage LOWEST = INIT;
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
