package net.alex.guzhenren.item.gu.mortal.path.strength;

import net.alex.guzhenren.gameplay.path.strength.HumanStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.gameplay.path.strength.StrengthPathBranch;
import net.alex.guzhenren.item.GuItem.Refusal;
import net.alex.guzhenren.item.gu.GuBehavior;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 人力行为：给一种人力加一层，种类与转数来自注册（斤 / 十斤 / 钧 / 十钧 / 百钧）。
 * <ul>
 *   <li>门槛先拒持兽力虚影流者，含运行中的兽力[Beast Strength]增益（{@link MarkInterference}），再拒该种已满层者。</li>
 *   <li>⚠ 授予挂在玩家而非物品堆上：满层者可把蛊交给别人，对方照样能用。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.tending.TendedGuItem
 * @since 1.0.0
 */

public record HumanStrengthBehavior(HumanStrength kind) implements GuBehavior {

    private static final String FAILED_LAYERS_FULL = "guzhenren.item.failed.human_strength_full";
    private static final int LAYERS_PER_GRANT = 1;

    @Override
    public @Nullable Refusal checkEffect(MortalGuItem gu, Player player, ItemStack stack) {
        Refusal interference = MarkInterference.check(player, StrengthPathBranch.HUMAN_JUN_STRENGTH);
        if (interference != null) return interference;

        return PathStrengthService.getHumanStrength(player, kind) >= kind.getMaxLayers()
                ? new Refusal(FAILED_LAYERS_FULL, Component.literal(String.valueOf(kind.getMaxLayers())))
                : null;
    }

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        PathStrengthService.addHumanStrength(player, kind, LAYERS_PER_GRANT);
    }
}
