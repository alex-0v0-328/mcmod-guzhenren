package net.alex.guzhenren.gameplay.body;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.NamedEnum;
import net.minecraft.util.StringRepresentable;

/**
 * 肉身六大肢体部位枚举：对应原版玩家模型的头、躯干与四肢，作为伤势落点与 B 面板方块人分区的键。
 * <ul>
 *   <li>左右方位以玩家自身视角为准（正面立绘中 {@link #RIGHT_ARM} 位于画面左侧）。</li>
 *   <li>⚠ 枚举序列化名直接作为 {@link Anatomy} 伤势映射表的持久化键，修改常量名会导致旧存档丢失对应部位伤势。</li>
 * </ul>
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
