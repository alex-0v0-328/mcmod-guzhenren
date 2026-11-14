package net.alex.guzhenren.gameplay.path.strength;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * 终点破坏打击[Crash Strike]落下时在游戏事件总线发布，只在服务端、冷却放行后发一次，掷空也发。
 * {@link #getOrigin} 为玩家身体中心，{@link #getDirection} 为水平冲撞单位向量，{@link #getRange} 为打击半径（格）。
 * 力道包只发布：冲击环在 {@code net.alex.guzhenren.particle}。
 *
 * @author Alex
 * @version 1.0.0
 * @see CrashStrike
 * @since 1.0.0
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
