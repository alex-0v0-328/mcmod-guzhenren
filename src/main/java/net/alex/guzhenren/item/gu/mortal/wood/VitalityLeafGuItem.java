package net.alex.guzhenren.item.gu.mortal.wood;

import net.alex.guzhenren.effect.timed.VitalityLeafEffect;
import net.alex.guzhenren.item.gu.GuSpec;
import net.alex.guzhenren.item.gu.OneShotGuItem;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A one-shot Gu that grants the vitality effect [生机叶], refusing while one it granted is still running.
 *
 * <p>Extends {@link net.alex.guzhenren.item.gu.OneShotGuItem}. The gate checks
 * {@code hasEffect(VITALITY_LEAF)} so a re-use while the effect runs is a refusal, not a refresh;
 * {@code apply} hands the effect holder and its duration to
 * {@link net.alex.guzhenren.registry.effect.ModEffects#instance}.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.OneShotGuItem
 * @since 1.0.0
 */

public class VitalityLeafGuItem extends OneShotGuItem {

    private static final String FAILED_VITALITY_ACTIVE = "guzhenren.item.failed.vitality_active";

    public VitalityLeafGuItem(Properties properties, GuSpec spec) {
        super(properties, spec);
    }

    @Override
    protected @Nullable Refusal useGate(Player player, ItemStack stack) {
        return player.hasEffect(ModEffects.VITALITY_LEAF) ? new Refusal(FAILED_VITALITY_ACTIVE) : null;
    }

    @Override
    protected int useApply(ServerPlayer player, ItemStack stack) {
        player.addEffect(ModEffects.instance(ModEffects.VITALITY_LEAF, VitalityLeafEffect.DURATION_TICKS));
        return 1;
    }
}
