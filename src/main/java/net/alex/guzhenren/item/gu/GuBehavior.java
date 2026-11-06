package net.alex.guzhenren.item.gu;

import net.alex.guzhenren.item.GuItem.Refusal;
import net.alex.guzhenren.item.gu.tending.ConsumedGuItem;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * One piece of what a Gu does [蛊虫行为]: a gate, an effect, and the limits it bends, each a default method, so a
 * behavior answers only what it changes. A Gu is a lifecycle host ({@link OneShotGuItem}, {@link TendedGuItem},
 * {@link ConsumedGuItem}) plus its behaviors, given in order at registration; the host owns refining, feeding,
 * the essence and hunger bill and cooldowns, and a behavior bends them only through the limits below.
 *
 * <p>The host asks every behavior in order: {@link #checkEffect} refuses with the first refusal, {@link #applyEffect}
 * runs them all, {@link #canBeAmplified} and {@link #canUseDuringEffectCooldown} hold when any one does, and the
 * hunger multiplier is the largest. {@link #defersForcedDeath} lets a behavior whose effect needs its Gu alive keep a
 * forced Gu to the end of its round, killing it there itself. A behavior carries its own numbers (a duration, a grade);
 * the Gu's shared numbers stay on {@link GuProperties}, read through the host it is given.
 *
 * <p>⚠ A behavior is stateless and may be shared between Gu: per-stack state lives on the stack, per-player state in
 * the services. Other code asks what a Gu does through {@link MortalGuItem#getBehavior}, never through its class.
 *
 * @author Alex
 * @version 1.0.0
 * @see MortalGuItem
 * @since 1.0.0
 */

public interface GuBehavior {

    default @Nullable Refusal checkEffect(MortalGuItem gu, Player player, ItemStack stack) { return null; }

    default void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {}

    default boolean canBeAmplified() { return false; }

    default boolean canUseDuringEffectCooldown() { return false; }

    default int getHungerCostMultiplier(MortalGuItem gu, Player player, ItemStack stack) { return 1; }

    default boolean defersForcedDeath(MortalGuItem gu, ServerPlayer player, ItemStack stack) { return false; }
}
