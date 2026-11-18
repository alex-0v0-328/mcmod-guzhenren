package net.alex.guzhenren.item.gu.mortal.path.heaven;

import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.Stage;
import net.alex.guzhenren.item.GuItem.Refusal;
import net.alex.guzhenren.item.gu.GuBehavior;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.registry.advancement.ModCriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 舍利蛊[Relics Gu]行为：供一次性消耗蛊（{@link net.alex.guzhenren.item.gu.OneShotGuItem}）将持有者主空窍提升一个小阶
 * 段，仅限与本蛊同转时使用；{@link #checkEffect} 委托 {@link MortalGuItem#checkStageUpGate} 拒绝转数不符或已达
 * {@link Stage#HIGHEST} 的情形，{@link #applyEffect} 调用 {@link ApertureService#shiftStage} 升阶并触发境界进度准则。
 */

public record StageUpBehavior() implements GuBehavior {

    @Override
    public @Nullable Refusal checkEffect(MortalGuItem gu, Player player, ItemStack stack) {
        return gu.checkStageUpGate(ApertureService.getAperture(player));
    }

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        ApertureService.shiftStage(player, 1);
        ModCriteriaTriggers.REACHED_RANK.get().trigger(player, ApertureService.getAperture(player));
    }
}
