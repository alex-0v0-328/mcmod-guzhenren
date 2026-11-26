package net.alex.guzhenren.gameplay.immortalaperture.mappipeline;

import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLandData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

/**
 * 地图标记来源[Map Mark Source]：一个功能把自己管的活物交给 {@link MapMarks} 取样（养殖给栏内外动物，蛊虫给野生蛊）；
 * 管线不认识功能，来源由组装层传进 {@link LandMaps#tick}。只往 {@link Sink} 里放实体与 {@link MapMark} 种类，视口筛选、
 * 远近排序与数量上限都由 {@link MapMarks} 做；每秒每个订阅者调一次。
 */

@FunctionalInterface
public interface MapMarkSource {

    void collect(@NotNull ServerLevel level, @NotNull BlissfulLandData data, @NotNull Sink sink);

    @FunctionalInterface
    interface Sink {

        void add(@NotNull Entity entity, byte kind);
    }
}
