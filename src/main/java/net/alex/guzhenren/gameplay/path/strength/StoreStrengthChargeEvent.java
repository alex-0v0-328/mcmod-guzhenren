package net.alex.guzhenren.gameplay.path.strength;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * 蓄力蛊[Store Strength Gu]蓄力的服务端变化事件，游戏事件总线。{@link Change#FULL}
 * 在首次判满（{@link StoreStrength#isFull}）那 tick，{@link Change#DROPPED} 在服务端自行取消（丢增益、分支或空手，
 * 换维度）；客户端自己松手、取消不发；同步分发；通知客户端的监听在 {@code net.alex.guzhenren.network}，
 * 客户端仅在服务端确认后显示蓄满。
 *
 * @author Alex
 * @version 1.0.0
 * @see StoreStrength
 * @since 1.0.0
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
