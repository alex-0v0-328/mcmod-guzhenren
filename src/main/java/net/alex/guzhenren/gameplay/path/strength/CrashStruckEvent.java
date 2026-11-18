package net.alex.guzhenren.gameplay.path.strength;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * 终点破坏打击[Crash Strike]触发事件：由 {@link CrashStrike#strike} 在服务端通过冷却检查后同步发布（挥空也发布；
 * {@link #getOrigin} 为玩家身体中心，{@link #getDirection} 为水平冲撞单位向量，{@link #getRange} 为打击半径），供上层
 * {@code net.alex.guzhenren.particle} 播放冲击环粒子。
 */

public class CrashStruckEvent extends Event {

    private final ServerPlayer player;
    private final Vec3 origin;
    private final Vec3 direction;
    private final double range;

    public CrashStruckEvent(@NotNull ServerPlayer player, @NotNull Vec3 origin, @NotNull Vec3 direction,
                            double range) {
        this.player = player;
        this.origin = origin;
        this.direction = direction;
        this.range = range;
    }

    public @NotNull ServerPlayer getPlayer() { return player; }

    public @NotNull Vec3 getOrigin() { return origin; }

    public @NotNull Vec3 getDirection() { return direction; }

    public double getRange() { return range; }
}
