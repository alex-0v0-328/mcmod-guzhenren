package net.alex.guzhenren.gameplay.immortalaperture.state;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * 地灵右键事件：玩家在服务端右键一只活着的 {@link EarthSpiritEntity} 时同步发布，由面板层接去开面板（地灵不认识面板）。
 */

public class EarthSpiritUseEvent extends Event {

    private final ServerPlayer player;
    private final EarthSpiritEntity spirit;

    public EarthSpiritUseEvent(@NotNull ServerPlayer player, @NotNull EarthSpiritEntity spirit) {
        this.player = player;
        this.spirit = spirit;
    }

    public @NotNull ServerPlayer getPlayer() { return player; }

    public @NotNull EarthSpiritEntity getSpirit() { return spirit; }
}
