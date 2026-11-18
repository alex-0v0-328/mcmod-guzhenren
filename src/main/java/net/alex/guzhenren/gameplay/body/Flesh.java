package net.alex.guzhenren.gameplay.body;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * 肉身血肉品阶枚举：存于 {@link BodyData}，当前仅含无加成的原生血肉（{@link #NATIVE}），预留给后续炼体蛊按声明顺序向上
 * 替换（{@link #canReplace}）。
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
