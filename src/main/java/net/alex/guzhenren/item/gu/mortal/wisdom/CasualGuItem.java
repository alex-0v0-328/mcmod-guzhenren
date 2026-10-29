package net.alex.guzhenren.item.gu.mortal.wisdom;

import net.alex.guzhenren.effect.timed.CasualThoughtEffect;
import net.alex.guzhenren.item.gu.ConsumedGuItem;
import net.alex.guzhenren.item.gu.GuSpec;
import net.alex.guzhenren.item.gu.mortal.StateEffects;
import net.alex.guzhenren.registry.effect.ModPathEffectTypes;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The Casual Gu [随意蛊]: a one-use wisdom Gu that floods the mind with random thoughts for ten seconds.
 *
 * <p>Extends {@link net.alex.guzhenren.item.gu.ConsumedGuItem}, making it tended AND taken by its
 * own use. The effect holder comes from registration; the amplifier is derived from the rank [转数] so a
 * higher-rung Gu floods harder. The payout delegates to
 * {@link StateEffects#grantThought}, which lays the path effect [流派效果] of the thoughts and projects the vanilla
 * effect from it.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.ConsumedGuItem
 * @since 1.0.0
 */

public class CasualGuItem extends ConsumedGuItem {

    private final Holder<MobEffect> effect;

    public CasualGuItem(Properties properties, Holder<MobEffect> effect, GuSpec spec) {
        super(properties, spec);
        this.effect = effect;
    }

    @Override
    protected @Nullable Refusal checkPayoutGate(Player player, ItemStack stack) { return null; }

    @Override
    protected void payOut(ServerPlayer player, ItemStack stack) {
        int amplifier = getTier();
        StateEffects.grantThought(player, this, ModPathEffectTypes.CASUAL_THOUGHT, effect, amplifier,
                CasualThoughtEffect.DURATION_TICKS);
    }
}
