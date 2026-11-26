package net.alex.guzhenren.gameplay.blissful.panel;

import net.alex.guzhenren.gameplay.blissful.land.BlissfulLandData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * 执行一个 {@link PanelAction} 的服务端上下文：已复判过的主人、他开着的面板菜单、福地维度与经营存档。
 */

public record ActionContext(@NotNull ServerPlayer player, @NotNull EarthSpiritMenu menu, @NotNull ServerLevel level,
                            @NotNull BlissfulLandData data) {}
