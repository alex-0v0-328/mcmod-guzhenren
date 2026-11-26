package net.alex.guzhenren.gameplay.immortalaperture.managementpanel;

import java.util.UUID;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLandData;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;

/**
 * 算一页 {@link PanelSection} 所需的服务端上下文：福地维度、该福地的经营存档、看面板的主人（按人区分的页面状态用，
 * 如库藏查询）。
 */

public record PanelContext(@NotNull ServerLevel level, @NotNull BlissfulLandData data, @NotNull UUID viewer) {}
