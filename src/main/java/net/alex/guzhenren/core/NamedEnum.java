package net.alex.guzhenren.core;

import java.util.Locale;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * 模组领域枚举的统一命名契约，同时派生存档序列化名与语言表翻译键。
 * <ul>
 *   <li>封闭词表枚举（如 {@code GuPath}、{@code Rank}、{@code Stage}、{@code Talent} 等）仅需实现
 *       {@link #getTranslationPrefix}：序列化名默认取枚举常量的小写名（{@link #getSerializedName}），翻译键自动拼接为前
 *       缀加序列化名（{@link #getTranslationKey}）。</li>
 *   <li>数据生成阶段的语言表 Provider 与运行时 UI 均直接接收本接口而非裸字符串，避免同一枚举的翻译键在多处硬编码失步。
 *       </li>
 *   <li>⚠ 各枚举的 {@code CODEC} 直接将 {@link #getSerializedName} 写入玩家存档，因此严禁随意重命名已发布的枚举常量；
 *       小写转换固定使用 {@link Locale#ROOT}，避免受操作系统语言环境影响。</li>
 * </ul>
 */

public interface NamedEnum extends StringRepresentable {

    String getTranslationPrefix();

    @Override
    default @NotNull String getSerializedName() { return ((Enum<?>) this).name().toLowerCase(Locale.ROOT); }

    default String getTranslationKey() { return getTranslationPrefix() + getSerializedName(); }
}
