package net.alex.guzhenren.gameplay.plunge;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * 坠击有效落地事件：当 {@link PlungeImpact#getImpactPower} 大于 {@code 0}（下落满 3 格）时，由 {@link PlungeService} 在
 * 炸坑破块前同步发布（此时 {@link #getPosition} 脚下地面仍完整），供上层 {@code compat} 播放地裂与跪地硬直动作、
 * {@code network} 广播震屏。
 */

public class PlungeLandedEvent extends Event {

    private final ServerPlayer player;
    private final Vec3 position;
    private final float impactPower;

    public PlungeLandedEvent(@NotNull ServerPlayer player, @NotNull Vec3 position, float impactPower) {
        this.player = player;
        this.position = position;
        this.impactPower = impactPower;
    }

    public @NotNull ServerPlayer getPlayer() { return player; }

    public @NotNull Vec3 getPosition() { return position; }

    public float getImpactPower() { return impactPower; }
}
