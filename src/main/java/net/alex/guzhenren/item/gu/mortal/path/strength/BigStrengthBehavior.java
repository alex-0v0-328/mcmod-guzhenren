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
 * 大力蛊[Big Strength Gu]行为：为玩家赋予不限力道分支的限时大力状态（{@link BigStrength}）；状态未结束时拒绝重复催
 * 动，支持功倍蛊[Amplify Effect Gu]倍增持续时长，持有兽种的时长加成（{@code PathStrengthService.getDurationBonus}，
 * 青牛 10 秒）乘后再加，生效后立即调用 {@link AttackDamageService#refresh} 刷新攻击力。
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
