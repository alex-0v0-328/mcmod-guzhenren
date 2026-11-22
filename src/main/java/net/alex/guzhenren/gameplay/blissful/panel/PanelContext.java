package net.alex.guzhenren.gameplay.blissful.panel;

import net.alex.guzhenren.gameplay.blissful.BlissfulLandData;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;

/**
 * 算一页 {@link PanelSection} 所需的服务端上下文：福地维度与该福地的经营存档。
 */

public record PanelContext(@NotNull ServerLevel level, @NotNull BlissfulLandData data) {}
