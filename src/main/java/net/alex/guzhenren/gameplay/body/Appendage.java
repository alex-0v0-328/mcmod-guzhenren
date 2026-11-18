package net.alex.guzhenren.gameplay.body;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.NamedEnum;
import net.minecraft.util.StringRepresentable;

/**
 * 肉身附加部位枚举：改造蛊在原生躯干外生长的翼、尾、角槽位（存于 {@link Anatomy}，当前仅由管理指令写入，尚未接入蛊虫与
 * 方块人渲染叠层）。
 */

public enum Appendage implements NamedEnum {

    WINGS,
    TAIL,
    HORNS;

    public static final Codec<Appendage> CODEC = StringRepresentable.fromEnum(Appendage::values);

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.body.appendage."; }
}
