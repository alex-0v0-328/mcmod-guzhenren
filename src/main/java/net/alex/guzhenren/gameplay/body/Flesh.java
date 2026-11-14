package net.alex.guzhenren.gameplay.body;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * 肉身的肉：暂只有原生、无加成，待蛊淬炼；{@code BodyData} 一槽。同族按声明顺序，新品阶只替换其前各级
 * （{@link #canReplace}）。
 *
 * @author Alex
 * @version 1.0.0
 * @see BodyData
 * @see BodyGrade
 * @since 1.0.0
 */

public enum Flesh implements BodyGrade<Flesh> {

    NATIVE;

    public static final Codec<Flesh> CODEC = StringRepresentable.fromEnum(Flesh::values);

    @Override
    public boolean isNative() { return this == NATIVE; }

    @Override
    public int getRank() { return 0; }

    @Override
    public boolean canReplace(@NotNull Flesh worn) { return worn.ordinal() < ordinal(); }

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.body.flesh."; }
}
