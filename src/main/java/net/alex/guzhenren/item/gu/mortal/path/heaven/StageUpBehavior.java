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
 * 舍利蛊[Relics Gu]行为：持有者升一阶段，限蛊本转。门槛 {@link MortalGuItem#checkStageUpGate}：转数不符或已在
 * {@link Stage#HIGHEST} 则拒绝；效果委托 {@link ApertureService#shiftStage}，再触发境界进度条件。
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.OneShotGuItem
 * @since 1.0.0
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
