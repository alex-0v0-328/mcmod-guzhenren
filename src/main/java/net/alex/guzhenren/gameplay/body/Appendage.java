package net.alex.guzhenren.gameplay.body;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.NamedEnum;
import net.minecraft.util.StringRepresentable;

/**
 * 附加部位：改造蛊在原生肢体之外长出的翼、尾、角，预留槽位，暂无蛊写入，方块人也还没有对应叠层。
 *
 * @author Alex
 * @version 1.0.0
 * @see Anatomy
 * @since 1.0.0
 */

public enum Appendage implements NamedEnum {

    WINGS,
    TAIL,
    HORNS;

    public static final Codec<Appendage> CODEC = StringRepresentable.fromEnum(Appendage::values);

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.body.appendage."; }
}
