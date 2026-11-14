package net.alex.guzhenren.item.gu.mortal.path.water;

import net.alex.guzhenren.gameplay.body.ShieldService;
import net.alex.guzhenren.gameplay.body.ShieldType;
import net.alex.guzhenren.item.gu.GuBehavior;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.mortal.behavior.TimedEffects;
import net.alex.guzhenren.item.gu.mortal.path.rule.AmplifyEffectGuItem;
import net.alex.guzhenren.item.gu.mortal.path.rule.AmplifyEffectGuItem.Amplification;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * 护盾行为：升一层护盾，类型、点数、时长取自注册；不拒催动，{@code TimedEffects} 凭它找某类护盾的蛊。
 * <ul>
 *   <li>天蓬蛊[Heavenly Mugwort Gu]携带，配 {@link net.alex.guzhenren.item.gu.mortal.behavior.RecastBehavior}。</li>
 *   <li>每次经 {@link ShieldService#raise} 补满并重计时，替换不叠加；功倍蛊[Amplify Effect Gu]只乘时长。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see ShieldService
 * @since 1.0.0
 */

public record ShieldBehavior(ShieldType type, float points, int durationTicks) implements GuBehavior {

    @Override
    public boolean canBeAmplified() { return true; }

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        Amplification amplification = AmplifyEffectGuItem.bill(player, gu);
        ShieldService.raise(player, type, points, durationTicks * amplification.factor());
        TimedEffects.markShieldAmplified(player, type, amplification);
    }
}
