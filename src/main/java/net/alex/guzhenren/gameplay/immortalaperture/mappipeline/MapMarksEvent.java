package net.alex.guzhenren.gameplay.immortalaperture.mappipeline;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * 活物标记下发事件：{@link LandMaps} 每秒给订阅者算好一份标记（至多 {@value MapMarks#LIMIT} 个）时同步发布，由网络层
 * 转成下发通知。
 */

public class MapMarksEvent extends Event {

    private final ServerPlayer player;
    private final List<MapMark> marks;

    public MapMarksEvent(@NotNull ServerPlayer player, @NotNull List<MapMark> marks) {
        this.player = player;
        this.marks = List.copyOf(marks);
    }

    public @NotNull ServerPlayer getPlayer() { return player; }

    public @NotNull List<MapMark> getMarks() { return marks; }
}
