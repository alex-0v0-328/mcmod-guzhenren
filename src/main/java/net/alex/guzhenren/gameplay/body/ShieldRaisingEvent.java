package net.alex.guzhenren.gameplay.body;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * 肉身护盾建层前事件：{@link ShieldService#raise} 创建护盾层前同步发布，允许上层特性改写护盾初始点数（如石龟
 * 兽力[Beast Strength]将护盾值乘以 1.5，见 {@code BeastTraitEvents}）；事件返回后按不低于 {@code 0} 的
 * {@link #getPoints} 铺设护盾层，持续时间由调用方决定。
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
