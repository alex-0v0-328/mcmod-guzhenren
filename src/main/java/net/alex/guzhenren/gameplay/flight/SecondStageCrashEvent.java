package net.alex.guzhenren.gameplay.flight;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * 二段飞行[Second-Stage Flight]高速撞击墙面、地面或天花板导致撞毁时发布的事件。
 * <ul>
 *   <li>由执行玩家移动的一端在 {@link SecondStageService#travel} 中同步发布（服务端传入 {@code ServerPlayer}，客户端本
 *       机物理传入 {@code LocalPlayer}，订阅方自行区分）；发布时玩家仍贴在撞击面上，{@link #getHeading} 为含俯仰角的归
 *       一化飞行方向向量。</li>
 *   <li>{@code gameplay.flight} 包只发布事件：音爆粒子环与附近震屏位于 {@code net.alex.guzhenren.particle}，本机震屏位
 *       于 {@code net.alex.guzhenren.client.event.SecondStageCamera}。</li>
 * </ul>
 */

public class SecondStageCrashEvent extends Event {

    private final Player player;
    private final Vec3 heading;

    public SecondStageCrashEvent(@NotNull Player player, @NotNull Vec3 heading) {
        this.player = player;
        this.heading = heading;
    }

    public @NotNull Player getPlayer() { return player; }

    public @NotNull Vec3 getHeading() { return heading; }
}
