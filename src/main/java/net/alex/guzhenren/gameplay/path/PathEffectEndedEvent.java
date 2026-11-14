package net.alex.guzhenren.gameplay.path;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * {@link PathEffectService} 摘掉玩家流派效果后发到游戏事件总线的事件。
 *
 * <ul>
 *   <li>带被摘效果及其道痕[Path Marks]与原因（到期、被顶替、调用方或命令摘除）；结束后要留影响的效果族在此订阅。</li>
 *   <li>⚠ 清空玩家的死亡绕过服务整体重置存储，不发事件；订阅方随这次死亡自行重置状态，与其他玩家数值一致。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see PathEffectService
 * @since 1.0.0
 */

public class PathEffectEndedEvent extends Event {

    private final ServerPlayer player;
    private final PathEffect<?> effect;
    private final Reason reason;

    public PathEffectEndedEvent(@NotNull ServerPlayer player, @NotNull PathEffect<?> effect, @NotNull Reason reason) {
        this.player = player;
        this.effect = effect;
        this.reason = reason;
    }

    public @NotNull ServerPlayer getPlayer() { return player; }

    public @NotNull PathEffect<?> getEffect() { return effect; }

    public @NotNull Reason getReason() { return reason; }

    public enum Reason { EXPIRED, REPLACED, REMOVED }
}
