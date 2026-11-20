package net.alex.guzhenren.item.gu.mortal.path.strength;

import net.alex.guzhenren.gameplay.path.strength.HumanStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.gameplay.path.strength.StrengthPathBranch;
import net.alex.guzhenren.gameplay.path.strength.StrengthService;
import net.alex.guzhenren.item.GuItem.Refusal;
import net.alex.guzhenren.item.gu.GuBehavior;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 人力授予行为：灌注[Channel]完成后为玩家叠加一层注册指定的 {@link HumanStrength}（一至五转分别对应一斤、十斤、一钧、十
 * 钧、百钧之力）。
 * <ul>
 *   <li>{@link #checkEffect} 先拦截已持有兽力虚影流派或正处于限时兽力增益中的玩家（{@link MarkInterference}），再拦截该
 *       档人力已达层数上限的玩家，最后过准入（{@link StrengthService#canAdmit}：已有永久力量点加这一层不得超过常态承
 *       受力）；拒绝均不扣费。</li>
 *   <li>百钧之力一层 3000 点，凡人的承受力装不下，一层也灌不进：留给蛊仙阶段。</li>
 *   <li>⚠ 已叠加的人力层数记录在玩家身上而非物品栈上：已满层的玩家可将同种蛊虫转交他人继续炼化使用。</li>
 * </ul>
 */

public record HumanStrengthBehavior(HumanStrength kind) implements GuBehavior {

    private static final String FAILED_LAYERS_FULL = "guzhenren.item.failed.human_strength_full";
    private static final String FAILED_CAPACITY = "guzhenren.item.failed.human_strength_capacity";
    private static final int LAYERS_PER_GRANT = 1;

    @Override
    public @Nullable Refusal checkEffect(MortalGuItem gu, Player player, ItemStack stack) {
        Refusal interference = MarkInterference.check(player, StrengthPathBranch.HUMAN_JUN_STRENGTH);
        if (interference != null) return interference;

        if (PathStrengthService.getHumanStrength(player, kind) >= kind.getMaxLayers()) {
            return new Refusal(FAILED_LAYERS_FULL, Component.literal(String.valueOf(kind.getMaxLayers())));
        }
        return StrengthService.canAdmit(player, kind) ? null : new Refusal(FAILED_CAPACITY);
    }

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        PathStrengthService.addHumanStrength(player, kind, LAYERS_PER_GRANT);
    }
}
