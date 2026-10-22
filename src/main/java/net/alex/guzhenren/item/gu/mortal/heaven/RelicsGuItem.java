package net.alex.guzhenren.item.gu.mortal.heaven;

import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.Stage;
import net.alex.guzhenren.item.gu.GuSpec;
import net.alex.guzhenren.item.gu.OneShotGuItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A one-shot Relics Gu [舍利蛊] that advances the holder's stage [阶段], only at its own rank.
 *
 * <p>Extends {@link net.alex.guzhenren.item.gu.OneShotGuItem}. The gate refuses a rank mismatch and
 * a holder already at {@link Stage#HIGHEST}; the apply
 * delegates to {@link ApertureService#shiftStage}.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.OneShotGuItem
 * @since 1.0.0
 */

public class RelicsGuItem extends OneShotGuItem {

    public RelicsGuItem(Properties properties, GuSpec spec) {
        super(properties, spec);
    }

    @Override
    protected @Nullable Refusal useGate(Player player, ItemStack stack) {
        return stageUpGate(ApertureService.getAperture(player));
    }

    @Override
    protected int useApply(ServerPlayer player, ItemStack stack) {
        ApertureService.shiftStage(player, 1);
        return 1;
    }
}
