package net.alex.guzhenren.gameplay.path.strength;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * 蓄力蛊[Store Strength Gu] / 惯力蛊[Habitual Strength Gu]的服务端蓄力状态变更事件：首次达到满蓄力
 * （{@link Change#FULL}）或被服务端判定中断（{@link Change#DROPPED}，如失去增益、切换主手或跨维度；客户端主动松手或取消
 * 不发）时同步发布，供上层 {@code network} 包通知客户端点亮蓄满提示或中止本地蓄力条。
 */

public class StoreStrengthChargeEvent extends Event {

    public enum Change { FULL, DROPPED }

    private final ServerPlayer player;
    private final Change change;

    public StoreStrengthChargeEvent(@NotNull ServerPlayer player, @NotNull Change change) {
        this.player = player;
        this.change = change;
    }

    public @NotNull ServerPlayer getPlayer() { return player; }

    public @NotNull Change getChange() { return change; }
}
