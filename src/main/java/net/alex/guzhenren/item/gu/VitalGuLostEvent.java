package net.alex.guzhenren.item.gu;

import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Posted on {@code NeoForge.EVENT_BUS} when a Gu bound as someone's Vital Gu [本命蛊] dies -- starved,
 * exhausted or ruined -- wherever it was held. {@link #getOwner} may be offline; the subscriber decides
 * whether to charge the owner now or park the stack until the next login. Dispatch is synchronous.
 *
 * <p>The item posts and does not know who listens: the penalty lives in the lifecycle, above the items,
 * and calling up into it would close a dependency cycle.
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
