package net.alex.guzhenren.gameplay.body;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.NamedEnum;
import net.minecraft.util.StringRepresentable;

/**
 * 肢体：伤势落点与 B 面板方块人的分区，对应原版人模六块。
 * <ul>
 *   <li>左右按玩家自身：正面图里 {@code RIGHT_ARM} 在画面左侧。</li>
 *   <li>⚠ 存档名是 {@link Anatomy} 伤势表的键，改常量名丢旧值。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see Anatomy
 * @since 1.0.0
 */

public enum BodyPart implements NamedEnum {

    HEAD,
    TORSO,
    RIGHT_ARM,
    LEFT_ARM,
    RIGHT_LEG,
    LEFT_LEG;

    public static final Codec<BodyPart> CODEC = StringRepresentable.fromEnum(BodyPart::values);

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.body.part."; }
}
