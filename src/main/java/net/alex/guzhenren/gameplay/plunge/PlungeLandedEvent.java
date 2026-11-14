package net.alex.guzhenren.gameplay.plunge;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * 坠击落地有表现时的同步事件。{@link PlungeImpact#getImpactPower} 大于 0 时在炸坑前发到游戏事件总线，
 * {@link #getPosition} 处地面仍完整；坠击不知谁听：地裂与落地动作在 {@code net.alex.guzhenren.compat}，震屏在
 * {@code net.alex.guzhenren.network}。
 *
 * @author Alex
 * @version 1.0.0
 * @see PlungeService
 * @since 1.0.0
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
