package net.alex.guzhenren.item.gu.mortal.path.strength;

import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.path.PathEffectService;
import net.alex.guzhenren.gameplay.path.strength.AttackDamageService;
import net.alex.guzhenren.gameplay.path.strength.BigStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.item.GuItem.Refusal;
import net.alex.guzhenren.item.gu.GuBehavior;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.mortal.behavior.TimedEffects;
import net.alex.guzhenren.item.gu.mortal.path.rule.AmplifyEffectGuItem;
import net.alex.guzhenren.item.gu.mortal.path.rule.AmplifyEffectGuItem.Amplification;
import net.alex.guzhenren.registry.effect.ModPathEffectTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 大力蛊[Big Strength Gu]的行为：限时大力状态（{@link BigStrength}），不限流派；状态未完再催动被拒；
 * 功倍蛊[Amplify Effect Gu]乘时长，持有兽种的时长加成（{@code PathStrengthService.getDurationBonus}，青牛 10 秒）乘后
 * 再加；施后调 {@link AttackDamageService#refresh}，攻击仍出自一个公式。
 *
 * @author Alex
 * @version 1.0.0
 * @see BigStrength
 * @since 1.0.0
 */

public record BigStrengthBehavior(int effectSeconds) implements GuBehavior {

    @Override
    public boolean canBeAmplified() { return true; }

    @Override
    public @Nullable Refusal checkEffect(MortalGuItem gu, Player player, ItemStack stack) {
        return BigStrength.isActive(player) ? new Refusal(Refusal.EFFECT_ACTIVE) : null;
    }

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        Amplification amplification = AmplifyEffectGuItem.bill(player, gu);
        long endTick = player.level().getGameTime() + (long) effectSeconds * Ticks.SECOND * amplification.factor()
                + PathStrengthService.getDurationBonus(player);
        PathEffectService.apply(player, gu.createEffect(ModPathEffectTypes.BIG_STRENGTH, endTick,
                BigStrength.State.INSTANCE));
        TimedEffects.markAmplified(player, ModPathEffectTypes.BIG_STRENGTH, gu.getEffectSource(), amplification);
        AttackDamageService.refresh(player);
    }
}
