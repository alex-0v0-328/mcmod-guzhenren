package net.alex.guzhenren.gameplay.body;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.NamedEnum;
import net.minecraft.util.StringRepresentable;

/**
 * 肉身七窍分类枚举：按眼、耳、鼻、口四类记录于 {@link Anatomy}（成对器官共用一档伤势）。
 * <ul>
 *   <li>七窍在解剖上均位于头部（{@link BodyPart#HEAD}），但伤势与头部独立记录（例如头部无伤时双目仍可单独受损）。</li>
 *   <li>当前仅承载伤势档位；七窍改造档位留待引入首只七窍改造蛊时扩展。</li>
 * </ul>
 */

public enum Organ implements NamedEnum {

    EYES,
    EARS,
    NOSE,
    MOUTH;

    public static final Codec<Organ> CODEC = StringRepresentable.fromEnum(Organ::values);

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.body.organ."; }
}
