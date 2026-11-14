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
 * 持续照顾型蛊[Tended Gu]的长冷却：{@link #check} 是催动、点按、{@code TendedGuItem.tryAutoUse} 的唯一门槛，
 * {@link #put} 加催动后冷却。
 * <ul>
 *   <li>两者取效果冷却（{@code canUseDuringEffectCooldown} 可免）与炼化后保护冷却（{@code REFINED_AT} 起
 *       {@code TendedGuItem.POST_REFINE_COOLDOWN_TICKS}）的较大者。</li>
 *   <li>⚠ 被拒时剩余 tick 写回原版冷却以重画转圈；{@link #put} 另含物品冷却，须在 {@code super.spend} 后写入：
 *       {@code ItemCooldowns.addCooldown} 无条件覆盖，反序则基类短冷却盖掉长冷却。</li>
 * </ul>
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
