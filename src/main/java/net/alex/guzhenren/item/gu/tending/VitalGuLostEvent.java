package net.alex.guzhenren.item.gu.tending;

import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * 本命蛊[Vital Gu]死亡事件：当绑定为本命的蛊虫在任意容器中饿死、力竭或损毁时，在游戏事件总线同步发布本事件。
 * <ul>
 *   <li>{@link #getOwner} 对应的玩家可能已离线；由监听方自行决定立即执行惩罚还是暂存待其下次登录时结算。</li>
 *   <li>物品层仅负责发布事件而不感知上层监听者：本命反噬惩罚位于物品层之上的生命周期层，若由物品层向上直接调用会形成循
 *       环依赖。</li>
 * </ul>
 */

public class VitalGuLostEvent extends Event {

    private final MinecraftServer server;
    private final UUID owner;
    private final ItemStack stack;

    public VitalGuLostEvent(@NotNull MinecraftServer server, @NotNull UUID owner, @NotNull ItemStack stack) {
        this.server = server;
        this.owner = owner;
        this.stack = stack;
    }

    public @NotNull MinecraftServer getServer() { return server; }

    public @NotNull UUID getOwner() { return owner; }

    public @NotNull ItemStack getStack() { return stack; }
}
