package net.alex.guzhenren.item.gu.mortal.strength;

import net.alex.guzhenren.item.gu.GuModule;
import net.alex.guzhenren.item.gu.TendedGuItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * The brink module [濒危自动催动]: its Gu drives ITSELF when the holder falls under a share of their max health.
 * The Self Reliance Gu [自力更生蛊] carries it with its {@link SelfRelianceModule}.
 *
 * <p>{@link #tryAutoUse} is the heartbeat's call: the first refined copy in the inventory whose holder is under its
 * share fires on its own, through {@code TendedGuItem.tryAutoUse}, so it pays and is gated as a hand use would be.
 *
 * <p>⚠ The brink relies on that gate to stay quiet while its effect runs: the Self Reliance gate refuses while a
 * heal of the same or a higher grade runs, and the effect cooldown holds between starts, or it would fire every
 * heartbeat until the holder healed.
 *
 * @author Alex
 * @version 1.0.0
 * @see SelfRelianceModule
 * @since 1.0.0
 */

public record AutoUseModule(float healthFraction) implements GuModule {

    public static void tryAutoUse(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().items) {
            if (!(stack.getItem() instanceof TendedGuItem gu)) continue;

            AutoUseModule brink = gu.getModule(AutoUseModule.class);
            if (brink == null || player.getHealth() >= player.getMaxHealth() * brink.healthFraction()) continue;

            if (gu.tryAutoUse(player, stack)) return;
        }
    }
}
