package net.alex.guzhenren.gameplay.path;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * 流派效果结束事件：{@link PathEffectService} 从玩家身上移除流派效果后在游戏总线上同步发布。
 * <ul>
 *   <li>携带被移除的 {@link PathEffect}（含其道痕[Path Marks]与载荷）以及结束原因（{@link Reason}：自然到期、被同槽顶
 *       替、主动或指令移除），供需要在效果结束时做收尾清理的机制订阅。</li>
 *   <li>⚠ 未开启死亡不掉落的死亡重置直接清空附件而不逐条发布此事件，各订阅方随死亡克隆流程自行重置状态。</li>
 * </ul>
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
