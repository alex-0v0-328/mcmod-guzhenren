package net.alex.guzhenren.item.gu;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * 凡蛊（{@link MortalGuItem}）催动成功并完成资源结算后，在 NeoForge 游戏事件总线同步发布的服务端事件。
 * <ul>
 *   <li>由 {@link MortalGuItem} 的 {@code spend} 统一触发，覆盖所有生效催动入口：手持右键、轻点、原地催动（轮盘、
 *       杀招[Killing Move]施放、功倍蛊[Amplify Effect Gu]驱动、元老蛊[Primeval Elder Gu]存取）以及自动催动；被门槛拒
 *       绝、因饱食[Hunger]不足中止、野生蛊炼化及灌注[Channel]中间轮次均不触发，蛊材亦不触发。</li>
 *   <li>Epic Fight 桥接层（{@code EpicFightServerEvents}）监听本事件以播放通用催动动作；事件不携带物品栈引用，因为单次
 *       消耗型蛊虫在结算后可能已耗尽。</li>
 * </ul>
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
