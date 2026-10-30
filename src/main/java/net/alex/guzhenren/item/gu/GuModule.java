package net.alex.guzhenren.item.gu;

import net.alex.guzhenren.item.GuItem.Refusal;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * One piece of what a Gu does [蛊虫模块]: a gate, an effect, and the limits it bends, each a default method, so a
 * module answers only what it changes. A Gu is a lifecycle host ({@link OneShotGuItem}, {@link TendedGuItem},
 * {@link ConsumedGuItem}) plus its modules, given in order at registration; the host owns refining, feeding,
 * the essence and hunger bill and cooldowns, and a module bends them only through the limits below.
 *
 * <p>The host asks every module in order: {@link #checkEffect} refuses with the first refusal, {@link #applyEffect}
 * runs them all, {@link #canBeAmplified} and {@link #canUseDuringEffectCooldown} hold when any one does, and the
 * hunger multiplier is the largest. A module carries its own numbers (a duration, a grade); the Gu's shared numbers
 * stay on {@link GuSpec}, read through the host it is given.
 *
 * <p>⚠ A module is stateless and may be shared between Gu: per-stack state lives on the stack, per-player state in
 * the services. Other code asks what a Gu does through {@link MortalGuItem#getModule}, never through its class.
 *
 * @author Alex
 * @version 1.0.0
 * @see MortalGuItem
 * @since 1.0.0
 */

public interface GuModule {

    default @Nullable Refusal checkEffect(MortalGuItem gu, Player player, ItemStack stack) { return null; }

    default void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {}

    default boolean canBeAmplified() { return false; }

    default boolean canUseDuringEffectCooldown() { return false; }

    default int getHungerCostMultiplier(MortalGuItem gu, Player player, ItemStack stack) { return 1; }
}
