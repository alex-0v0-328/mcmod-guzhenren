package net.alex.guzhenren.item.gu.mortal.path.wood;

import net.alex.guzhenren.effect.timed.VitalityLeafGuEffect;
import net.alex.guzhenren.item.GuItem.Refusal;
import net.alex.guzhenren.item.gu.GuBehavior;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.mortal.behavior.TimedEffects;
import net.alex.guzhenren.item.gu.mortal.path.rule.AmplifyEffectGuItem;
import net.alex.guzhenren.item.gu.mortal.path.rule.AmplifyEffectGuItem.Amplification;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 生机叶蛊[Vitality Leaf Gu]行为：给生机叶效果，仍在则拒。
 * <ul>
 *   <li>门槛查 {@link TimedEffects#hasLeaf}：持续中再催动是拒而非刷新；时长交 {@link TimedEffects#grantLeaf}。</li>
 *   <li>功倍蛊[Amplify Effect Gu]乘时长，返还生命同倍增加。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.OneShotGuItem
 * @since 1.0.0
 */

public record VitalityLeafBehavior() implements GuBehavior {


    @Override
    public boolean canBeAmplified() { return true; }

    @Override
    public @Nullable Refusal checkEffect(MortalGuItem gu, Player player, ItemStack stack) {
        return TimedEffects.hasLeaf(player) ? new Refusal(Refusal.EFFECT_ACTIVE) : null;
    }

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        Amplification amplification = AmplifyEffectGuItem.bill(player, gu);
        TimedEffects.grantLeaf(player, gu, (long) VitalityLeafGuEffect.DURATION_TICKS * amplification.factor(),
                amplification);
    }
}
