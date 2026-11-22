package net.alex.guzhenren.gameplay.blissful.panel;

import org.jetbrains.annotations.NotNull;

/**
 * 地灵经营面板某一页的显示数据：由该页模块在服务端算好，随 {@link PanelSnapshot} 下发；客户端按页缓存，只显示。
 */

public interface PanelSection {

    @NotNull Page page();
}
