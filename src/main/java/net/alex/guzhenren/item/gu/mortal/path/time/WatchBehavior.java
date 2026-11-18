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
 * 更蛊[Watch Gu]行为：在限时内加速持有者自身的时间流速（{@link TimedEffects#grantWatch}）。
 * <ul>
 *   <li>两更蛊[Second Watch Gu]至多叠加两层（达 ×2 速率），三更蛊[Third Watch Gu]至多叠加三层（达 ×3 速率）；具体速率公
 *       式与层数上限定义在 {@link WatchGuEffect} 中。</li>
 *   <li>⚠ 本行为不设重复催动拦截：持续期间再次催动同种更蛊会叠加一层（直至层数上限）并刷新持续时间；两更蛊与三更蛊可同
 *       时生效且加速倍率相加；功倍蛊[Amplify Effect Gu]仅倍增刷新后的持续时间，不倍增叠加层数。</li>
 * </ul>
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
