package net.alex.guzhenren.gameplay.immortalaperture.mappipeline;

import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLand;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 地图订阅门[Map Viewer Gate]：判一个玩家此刻能看哪块福地的地图，不能看即空。管线不认识面板，判定由组装层传进
 * {@link LandMaps#view} 与 {@link LandMaps#tick}。
 */

@FunctionalInterface
public interface MapViewerGate {

    @Nullable BlissfulLand viewing(@NotNull ServerPlayer player);
}
