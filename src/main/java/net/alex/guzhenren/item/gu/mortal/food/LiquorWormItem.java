package net.alex.guzhenren.item.gu.mortal.food;

import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.item.gu.GuSpec;
import net.alex.guzhenren.item.gu.TendedGuItem;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Liquor Worm [酒虫]: a tended Gu that distills ordinary essence [真元] into the distilled reserve, in phases.
 *
 * <p>Extends {@link net.alex.guzhenren.item.gu.TendedGuItem}. Four rungs register against this one class,
 * each usable only at its own rank. The payout calls
 * {@link ApertureEssenceService#beginDistilling} and stamps a
 * day-long effect; the three phases (drain, redirect, 1:2 spend) live in the service and the effect, not here.
 *
 * <p>⚠ While it runs the ordinary pool is empty by design. Anything gating on essence must ask for the
 * spendable figure, or it will refuse everything for the whole of that stretch.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.TendedGuItem
 * @since 1.0.0
 */

public class LiquorWormItem extends TendedGuItem {

    private static final String FAILED_RANK = "guzhenren.item.failed.liquor_rank";
    private static final String FAILED_DISTILLING = "guzhenren.item.failed.liquor_distilling";

    public LiquorWormItem(Properties properties, GuSpec spec) {
        super(properties, spec);
    }

    @Override
    protected @Nullable Refusal payoutGate(Player player, ItemStack stack) {
        if (ApertureService.getRank(player) != rank()) {
            return new Refusal(FAILED_RANK, Component.translatable(rank().getTranslationKey()));
        }
        return ApertureEssenceService.canDistill(player) ? null : new Refusal(FAILED_DISTILLING);
    }

    @Override
    protected void payout(ServerPlayer player, ItemStack stack) {
        ApertureEssenceService.beginDistilling(player);
        player.addEffect(ModEffects.instance(ModEffects.LIQUOR_WORM, Ticks.DAY, tier()));
    }
}
