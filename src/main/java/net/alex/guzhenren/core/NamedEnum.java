package net.alex.guzhenren.core;

import java.util.Locale;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * 模组领域枚举的两个名字：存档名与翻译键。
 * <ul>
 *   <li>封闭词表枚举（{@code GuPath}、{@code Rank}、{@code Stage}、{@code Talent} 等）只声明
 *       {@link #getTranslationPrefix}；存档名为常量名小写，翻译键为前缀加存档名。</li>
 *   <li>两个语言 provider 都收本接口而非 {@code String}，枚举带出的键与注册的键不会分成两个字面量。</li>
 *   <li>⚠ 各枚举的 {@code CODEC} 把存档名写进玩家存档：改常量名会丢失旧名下的值；小写用 {@link Locale#ROOT}，任何系统区
 *       域写出的名字都相同。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public interface NamedEnum extends StringRepresentable {

    String getTranslationPrefix();

    @Override
    default @NotNull String getSerializedName() { return ((Enum<?>) this).name().toLowerCase(Locale.ROOT); }

    default String getTranslationKey() { return getTranslationPrefix() + getSerializedName(); }
}
