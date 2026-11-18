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
 * 生机叶蛊[Vitality Leaf Gu]行为：供一次性消耗蛊（{@link net.alex.guzhenren.item.gu.OneShotGuItem}）赋予玩家生机叶持续
 * 回血效果（{@link TimedEffects#grantLeaf}）。
 * <ul>
 *   <li>{@link #checkEffect} 通过 {@link TimedEffects#hasLeaf} 拦截已有生机叶效果的玩家（持续期间再次催动直接拒绝而非刷
 *       新时长）。</li>
 *   <li>支持功倍蛊[Amplify Effect Gu]倍增持续时长，总回复生命量随持续时长同倍增加。</li>
 * </ul>
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
