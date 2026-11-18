package net.alex.guzhenren.gameplay.body;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.NamedEnum;
import net.minecraft.util.StringRepresentable;

/**
 * 解剖部位伤势五档枚举：按声明顺序由无伤（{@link #NONE}）至残废（{@link #CRIPPLED}）递增，供 {@link Anatomy} 记录各肢体
 * 与七窍伤势。
 * <ul>
 *   <li>受伤来源、各档惩罚与治疗手段尚未定型，当前仅由 {@code /gzr body injury} 指令写入并驱动 B 面板方块人部位染
 *       色。</li>
 *   <li>与苦力蛊[Bitter Strength Gu]按当前生命值比例划分的五档增幅无关：苦力蛊看实时血量比例，本枚举看部位解剖状
 *       态。</li>
 * </ul>
 */

public enum Injury implements NamedEnum {

    NONE,
    LIGHT,
    MODERATE,
    SEVERE,
    CRIPPLED;

    public static final Codec<Injury> CODEC = StringRepresentable.fromEnum(Injury::values);

    public boolean isInjured() { return this != NONE; }

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.body.injury."; }
}
