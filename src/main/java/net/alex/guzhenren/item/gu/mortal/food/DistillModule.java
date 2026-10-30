package net.alex.guzhenren.item.gu.mortal.food;

import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.item.GuItem.Refusal;
import net.alex.guzhenren.item.gu.GuModule;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.mortal.StateEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The Liquor Worm's [酒虫] module: distills ordinary essence [真元] into the distilled reserve, in phases.
 *
 * <p>Four rungs register with it, each usable only at its own rank. The payout calls
 * {@link ApertureEssenceService#beginDistilling} and lays a day-long path effect [流派效果] on the body, the truth
 * of the distilling; the three phases (drain, redirect, 1:2 spend) live in the service and the effect, not here.
 *
 * <p>⚠ While it runs the ordinary pool is empty by design. Anything gating on essence must ask for the
 * spendable figure, or it will refuse everything for the whole of that stretch.
 *
 * @author Alex
 * @version 1.0.0
 * @see StateEffects#grantDistilling
 * @since 1.0.0
 */

public record DistillModule() implements GuModule {

    private static final String FAILED_RANK = "guzhenren.item.failed.liquor_rank";
    private static final String FAILED_DISTILLING = "guzhenren.item.failed.liquor_distilling";

    @Override
    public @Nullable Refusal checkEffect(MortalGuItem gu, Player player, ItemStack stack) {
        if (ApertureService.getRank(player) != gu.getRank()) {
            return new Refusal(FAILED_RANK, Component.translatable(gu.getRank().getTranslationKey()));
        }
        return ApertureEssenceService.canDistill(player) ? null : new Refusal(FAILED_DISTILLING);
    }

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        ApertureEssenceService.beginDistilling(player);
        StateEffects.grantDistilling(player, gu, gu.getTier(), Ticks.DAY);
    }
}
