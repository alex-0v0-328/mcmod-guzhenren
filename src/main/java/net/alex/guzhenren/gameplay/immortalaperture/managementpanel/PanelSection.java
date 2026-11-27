package net.alex.guzhenren.gameplay.immortalaperture.managementpanel;

import org.jetbrains.annotations.NotNull;

/**
 * 地灵经营面板一页里的一段显示数据：由所属面板段在服务端算好，随 {@link PanelSnapshot} 下发；客户端按面板段缓存，只显示。
 * <ul>
 *   <li>{@link #source} 指回出这一段的面板段（取其 {@code INSTANCE}），编码按它在 {@link PanelSectionSources#ALL} 里的
 *       下标；段落在哪页由面板段的 {@code page()} 决定。</li>
 * </ul>
 */

public interface PanelSection {

    @NotNull PanelSectionSource<?> source();
}
