package net.alex.guzhenren.item.gu.mortal.wood;

import net.alex.guzhenren.item.gu.mortal.rule.AmplifyEffectGuItem.Amplification;
import net.alex.guzhenren.item.gu.mortal.TimedEffects;
import net.alex.guzhenren.effect.timed.VitalityLeafEffect;
import net.alex.guzhenren.item.gu.GuSpec;
import net.alex.guzhenren.item.gu.OneShotGuItem;
import net.alex.guzhenren.item.gu.mortal.rule.AmplifyEffectGuItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A one-shot Gu that grants the vitality effect [生机叶], refusing while one it granted is still running.
 *
 * <p>Extends {@link net.alex.guzhenren.item.gu.OneShotGuItem}. The gate checks {@link TimedEffects#hasLeaf} so a
 * re-use while the leaf runs is a refusal, not a refresh; {@code useApply} hands its duration to
 * {@link TimedEffects#grantLeaf}. An Amplify Effect Gu [功倍蛊] multiplies that duration, and with it the health
 * the leaf returns.
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
    public boolean canBeAmplified() { return true; }

    @Override
    protected @Nullable Refusal checkUseGate(Player player, ItemStack stack) {
        return TimedEffects.hasLeaf(player) ? new Refusal(FAILED_VITALITY_ACTIVE) : null;
    }

    @Override
    protected int useApply(ServerPlayer player, ItemStack stack) {
        Amplification amplification = AmplifyEffectGuItem.bill(player, this);
        TimedEffects.grantLeaf(player, this, VitalityLeafEffect.DURATION_TICKS * amplification.factor(),
                amplification);
        return 1;
    }
}
