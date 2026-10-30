package net.alex.guzhenren.item.gu.mortal.human;

import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.item.GuItem.Refusal;
import net.alex.guzhenren.item.gu.GuModule;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The Second Aperture Gu's [第二空窍蛊] module: the only opener of the second aperture, human path. Its Gu is
 * one-shot: refining and using are free and instant, it carries no state and it stacks. Five rungs register it.
 *
 * <p>The gate refuses only a second aperture already at or above the Gu's rung -- an upgrade must be
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

public record SecondApertureModule() implements GuModule {

    private static final String FAILED_SECOND_RANK = "guzhenren.item.failed.second_aperture_rank";

    @Override
    public @Nullable Refusal checkEffect(MortalGuItem gu, Player player, ItemStack stack) {
        ApertureData data = ApertureService.get(player);
        int index = data.getSecondIndex();
        return index >= 0 && data.get(index).rank().ordinal() >= gu.getRank().ordinal()
                ? new Refusal(FAILED_SECOND_RANK) : null;
    }

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        ApertureService.openSecond(player, gu.getRank());
    }
}
