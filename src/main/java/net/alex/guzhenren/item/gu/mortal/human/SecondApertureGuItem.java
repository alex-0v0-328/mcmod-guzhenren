package net.alex.guzhenren.item.gu.mortal.human;

import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.item.gu.GuSpec;
import net.alex.guzhenren.item.gu.OneShotGuItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A Second Aperture Gu [第二空窍蛊]: the only opener of the second aperture, human path. One-shot:
 * refining and using are free and instant, it carries no state and it stacks. Five rungs register
 * against this one class.
 *
 * <p>The gate refuses only a second aperture already at or above the rung -- an upgrade must be
 * strictly higher than the second aperture alone, and it overwrites it whole. (Back to the first stage,
 * Grade-A at 8/10, progress zeroed, bound paths kept.) Everything else answers: no first aperture,
 * no aperture at all, any rank gap -- a Gu below the holder needs no primary to compare against.
 * Usable while undead: an aperture opened by it reads a LIVING one under every later undeath.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.OneShotGuItem
 * @see ApertureService#openSecond
 * @since 1.0.0
 */

public class SecondApertureGuItem extends OneShotGuItem {

    private static final String FAILED_SECOND_RANK = "guzhenren.item.failed.second_aperture_rank";

    public SecondApertureGuItem(Properties properties, GuSpec spec) {
        super(properties, spec);
    }

    @Override
    protected @Nullable Refusal useGate(Player player, ItemStack stack) {
        ApertureData data = ApertureService.get(player);
        int index = data.secondIndex();
        return index >= 0 && data.get(index).rank().ordinal() >= rank().ordinal()
                ? new Refusal(FAILED_SECOND_RANK) : null;
    }

    @Override
    protected int useApply(ServerPlayer player, ItemStack stack) {
        ApertureService.openSecond(player, rank());
        return 1;
    }
}
