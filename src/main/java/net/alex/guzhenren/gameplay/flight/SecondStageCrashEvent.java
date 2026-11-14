package net.alex.guzhenren.gameplay.flight;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * 二段飞行[Second-Stage Flight]撞上墙、地面或天花板时在游戏事件总线发布。由移动玩家的一端发
 * （{@link SecondStageService#travel}）：服务端那步带 {@code ServerPlayer}，本机物理带 {@code LocalPlayer}，监听方自
 * 判；同步派发，玩家仍贴着撞击面；{@link #getHeading} 为含俯仰的飞行单位向量，俯冲朝下。飞行包只发布：音爆环在
 * {@code net.alex.guzhenren.particle}，震屏在 {@code net.alex.guzhenren.client.event.SecondStageCamera}。
 *
 * @author Alex
 * @version 1.0.0
 * @see SecondStageService
 * @since 1.0.0
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
