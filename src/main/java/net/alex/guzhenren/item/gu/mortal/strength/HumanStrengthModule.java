package net.alex.guzhenren.item.gu.mortal.strength;

import net.alex.guzhenren.gameplay.path.strength.HumanStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.item.GuItem.Refusal;
import net.alex.guzhenren.item.gu.GuModule;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The human-strength module [人力]: adds one layer of one kind; the rank [转数] and the kind come from registration.
 *
 * <p>Four rungs register it (斤 / 十斤 / 钧 / 十钧). The gate refuses a holder who has maxed out that kind; the
 * effect delegates to {@link PathStrengthService#addHumanStrength}. One layer per grant is fixed by
 * {@code LAYERS_PER_GRANT}.
 *
 * <p>⚠ What it grants rides the player rather than the stack, so a holder who has already maxed out
 * can hand the Gu to someone else, and it goes on working for them.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.TendedGuItem
 * @since 1.0.0
 */

public record HumanStrengthModule(HumanStrength kind) implements GuModule {

    private static final String FAILED_LAYERS_FULL = "guzhenren.item.failed.human_strength_full";
    private static final int LAYERS_PER_GRANT = 1;

    @Override
    public @Nullable Refusal checkEffect(MortalGuItem gu, Player player, ItemStack stack) {
        return PathStrengthService.getHumanStrength(player, kind) >= kind.getMaxLayers()
                ? new Refusal(FAILED_LAYERS_FULL, Component.literal(String.valueOf(kind.getMaxLayers())))
                : null;
    }

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        PathStrengthService.addHumanStrength(player, kind, LAYERS_PER_GRANT);
    }
}
