package net.alex.guzhenren.item.gu.tending;

import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * 本命蛊死亡事件：绑定为本命的蛊无论在哪饿死、力竭或损毁，都在游戏事件总线同步发布。
 * <ul>
 *   <li>{@link #getOwner} 可能离线；订阅方决定立即惩罚，还是把物品暂存到下次登录。</li>
 *   <li>物品只发布、不知监听者：惩罚在物品之上的生命周期层，向上调用会成依赖环。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see TendedGuItem
 * @since 1.0.0
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
