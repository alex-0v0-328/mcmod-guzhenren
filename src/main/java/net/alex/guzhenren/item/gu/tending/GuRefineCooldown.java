package net.alex.guzhenren.item.gu.tending;

import net.alex.guzhenren.display.ModDisplayText;
import net.alex.guzhenren.gameplay.clock.TimeFlow;
import net.alex.guzhenren.item.GuItem.Refusal;
import net.alex.guzhenren.item.gu.GuProperties;
import net.alex.guzhenren.item.gu.stack.GuCooldowns;
import net.alex.guzhenren.registry.item.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A tended Gu's long cooldown [长冷却]: the one gate for the use, the tap use and {@code TendedGuItem.tryAutoUse}
 * ({@link #check}), and the cooldown put after a use ({@link #put}). Both read the larger of the effect cooldown
 * (waived by {@code canUseDuringEffectCooldown}) and the post-refine guard that {@code
 * TendedGuItem.POST_REFINE_COOLDOWN_TICKS} sets from the {@code REFINED_AT} stamp.
 *
 * <p>⚠ A refusal puts the ticks left back on the vanilla cooldown, so a lost sweep redraws. {@link #put} runs after
 * {@code super.spend} and puts the largest of the item cooldown, the post-refine guard and the effect cooldown:
 * {@code ItemCooldowns.addCooldown} overwrites unconditionally, so the other order would lose the long cooldown to
 * the base's short one.
 *
 * @author Alex
 * @version 1.0.0
 * @see TendedGuItem
 * @see GuCooldowns
 * @since 1.0.0
 */

final class GuRefineCooldown {

    private static final String FAILED_COOLDOWN = "guzhenren.item.failed.gu_cooldown";

    private final int itemCooldownTicks;

    GuRefineCooldown(GuProperties guProperties) { this(guProperties.getItemCooldownTicks()); }

    GuRefineCooldown(int itemCooldownTicks) { this.itemCooldownTicks = itemCooldownTicks; }

    static int postRefineCooldownLeft(Player player, ItemStack stack) {
        Long refinedAt = stack.get(ModDataComponents.REFINED_AT);
        return GuCooldowns.getCooldownLeft(player, refinedAt, TendedGuItem.POST_REFINE_COOLDOWN_TICKS);
    }

    static int longest(int itemWait, int postRefineLeft, boolean waivesEffect, int effectLeft) {
        return Math.max(Math.max(itemWait, postRefineLeft), waivesEffect ? 0 : effectLeft);
    }

    @Nullable Refusal check(TendedGuItem gu, Player player, ItemStack stack) {
        int left = longest(0, postRefineCooldownLeft(player, stack), gu.canUseDuringEffectCooldown(),
                gu.getEffectCooldownLeft(player, stack));
        if (left <= 0) return null;

        if (player instanceof ServerPlayer server) server.getCooldowns().addCooldown(gu, left);
        return new Refusal(FAILED_COOLDOWN,
                Component.literal(String.valueOf(ModDisplayText.getCountdownSeconds(left))));
    }

    void put(TendedGuItem gu, ServerPlayer player, ItemStack stack) {
        int left = longest(TimeFlow.shortenWait(player, itemCooldownTicks), postRefineCooldownLeft(player, stack),
                gu.canUseDuringEffectCooldown(), gu.getEffectCooldownLeft(player, stack));
        if (left > 0) player.getCooldowns().addCooldown(gu, left);
    }
}
