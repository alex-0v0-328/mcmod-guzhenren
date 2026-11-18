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
 * 持续照顾型蛊[Tended Gu]的长冷却门控：{@link #check} 是催动、点按及 {@code TendedGuItem.tryAutoUse} 共用的唯一冷却检
 * 查入口，{@link #put} 负责在催动后写入冷却。
 * <ul>
 *   <li>剩余长冷却取效果冷却（声明 {@code canUseDuringEffectCooldown} 时豁免）与炼化后保护冷却（自 {@code REFINED_AT}
 *       起计 {@code TendedGuItem.POST_REFINE_COOLDOWN_TICKS}）两者的较大值。</li>
 *   <li>⚠ 校验被拒时会将剩余 tick 写回原版物品冷却以重绘扇形转圈；{@link #put} 另行写入常规物品冷却，因此必须在
 *       {@code super.spend} 之后调用：原版 {@code ItemCooldowns.addCooldown} 会无条件覆盖既有冷却，若顺序相反则基类的短
 *       冷却会覆盖掉长冷却。</li>
 * </ul>
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
