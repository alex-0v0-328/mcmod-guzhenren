package net.alex.guzhenren.gameplay.body;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * {@link ShieldService#raise} 建层前发布的事件：上层特性可改点数，如石龟兽力[Beast Strength] ×1.5
 * （{@code BeastTraitEvents}）；返回后按 {@link #getPoints} 铺层，不低于 0，时长归调用方。
 *
 * @author Alex
 * @version 1.0.0
 * @see ShieldRaisedEvent
 * @since 1.0.0
 */

public class ShieldRaisingEvent extends Event {

    private final ServerPlayer player;
    private final ShieldType type;
    private float points;

    public ShieldRaisingEvent(@NotNull ServerPlayer player, @NotNull ShieldType type, float points) {
        this.player = player;
        this.type = type;
        this.points = points;
    }

    public @NotNull ServerPlayer getPlayer() { return player; }

    public @NotNull ShieldType getType() { return type; }

    public float getPoints() { return points; }

    public void setPoints(float points) { this.points = points; }
}
