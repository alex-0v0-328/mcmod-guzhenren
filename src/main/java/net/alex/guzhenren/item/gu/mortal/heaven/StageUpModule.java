package net.alex.guzhenren.item.gu.mortal.heaven;

import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.Stage;
import net.alex.guzhenren.item.GuItem.Refusal;
import net.alex.guzhenren.item.gu.GuModule;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.registry.advancement.ModCriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The Relics Gu's [舍利蛊] module: advances the holder's stage [阶段], only at the Gu's own rank.
 *
 * <p>The gate is {@link MortalGuItem#checkStageUpGate}: a rank mismatch or a holder already at {@link Stage#HIGHEST}
 * refuses. The effect delegates to {@link ApertureService#shiftStage}, then fires the realm criterion.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.OneShotGuItem
 * @since 1.0.0
 */

public record StageUpModule() implements GuModule {

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
