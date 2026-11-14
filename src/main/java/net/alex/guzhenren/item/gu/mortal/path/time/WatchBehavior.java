package net.alex.guzhenren.item.gu.mortal.path.time;

import net.alex.guzhenren.effect.timed.WatchGuEffect;
import net.alex.guzhenren.item.gu.GuBehavior;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.mortal.behavior.TimedEffects;
import net.alex.guzhenren.item.gu.mortal.path.rule.AmplifyEffectGuItem;
import net.alex.guzhenren.item.gu.mortal.path.rule.AmplifyEffectGuItem.Amplification;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;

/**
 * 更蛊[Watch Gu]的行为：限时让持有者的自身时间快于世界。
 * <ul>
 *   <li>两更蛊[Second Watch Gu]至多两层 ×2，三更蛊[Third Watch Gu]至多三层 ×3；速率与层数上限归效果所有。</li>
 *   <li>⚠ 不拒绝催动：同种再用加一层（有上限）并刷新时长；两种同在是设计，速率相加；功倍蛊[Amplify Effect Gu]乘刷新后的
 *       时长，不乘层数。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see TimedEffects#grantWatch
 * @since 1.0.0
 */

public record WatchBehavior(Holder<MobEffect> form, int effectTicks) implements GuBehavior {

    public WatchBehavior {
        if (!(form.value() instanceof WatchGuEffect)) {
            throw new IllegalArgumentException("watch gu needs a WatchGuEffect");
        }
    }

    @Override
    public boolean canBeAmplified() { return true; }

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        Amplification amplification = AmplifyEffectGuItem.bill(player, gu);
        TimedEffects.grantWatch(player, gu, form, (long) effectTicks * amplification.factor(), amplification);
    }
}
