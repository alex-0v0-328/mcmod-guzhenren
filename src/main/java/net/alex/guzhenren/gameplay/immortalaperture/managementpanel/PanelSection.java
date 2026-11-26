package net.alex.guzhenren.gameplay.immortalaperture.managementpanel;

import net.alex.guzhenren.gameplay.immortalaperture.LandModule;
import org.jetbrains.annotations.NotNull;

/**
 * 地灵经营面板一页里的一段显示数据：由所属模块在服务端算好，随 {@link PanelSnapshot} 下发；客户端按模块缓存，只显示。
 * <ul>
 *   <li>{@link #module} 指回出这一段的模块（取其 {@code INSTANCE}），编码按它在
 *       {@link net.alex.guzhenren.gameplay.immortalaperture.LandModules#ALL} 里的下标；段落在哪页由模块的 {@code page()} 决定。</li>
 * </ul>
 */

public interface PanelSection {

    @NotNull LandModule<?> module();
}
