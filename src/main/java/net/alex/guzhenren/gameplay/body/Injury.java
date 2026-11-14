package net.alex.guzhenren.gameplay.body;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.NamedEnum;
import net.minecraft.util.StringRepresentable;

/**
 * 伤势五档，肢体与七窍各记一档，由轻到重按声明顺序。
 * <ul>
 *   <li>受伤来源、各档惩罚与治疗未定，现在只有 {@code /gzr body injury} 写入，方块人按档染色。</li>
 *   <li>与苦力蛊按血量分的「伤势五档」无关：那是血量比例，这是部位状态。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see Anatomy
 * @since 1.0.0
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
