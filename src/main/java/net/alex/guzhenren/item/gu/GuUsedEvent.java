package net.alex.guzhenren.item.gu;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * 凡蛊催动生效并结账后，发布于游戏事件总线的事件。发自 {@link MortalGuItem} 的 {@code spend}，即所有生效催动的终点：点
 * 击、轻点、原地（轮盘、杀招[Killing Move] 施放、功倍蛊[Amplify Effect Gu] 驱动、元老蛊[Primeval Elder Gu] 存取）及自动
 * 催动；被拒、饱食[Hunger] 中止、野生蛊炼化、灌注[Channel] 各轮不触发；蛊材非凡蛊。Epic Fight 桥接据此播通用催动动作
 * （{@code EpicFightServerEvents}）。不带物品堆：催动可能用掉最后一只。同步派发。
 *
 * @author Alex
 * @version 1.0.0
 * @see MortalGuItem
 * @since 1.0.0
 */

public class GuUsedEvent extends Event {

    private final ServerPlayer player;
    private final MortalGuItem gu;

    public GuUsedEvent(@NotNull ServerPlayer player, @NotNull MortalGuItem gu) {
        this.player = player;
        this.gu = gu;
    }

    public @NotNull ServerPlayer getPlayer() { return player; }

    public @NotNull MortalGuItem getGu() { return gu; }
}
