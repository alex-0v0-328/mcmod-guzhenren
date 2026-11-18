package net.alex.guzhenren.item.gu.mortal.path.strength;

import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthData;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.gameplay.path.strength.StoreStrength;
import net.alex.guzhenren.gameplay.path.strength.StrengthPathBranch;
import net.alex.guzhenren.item.GuItem.Refusal;
import net.alex.guzhenren.item.gu.GuBehavior;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.mortal.behavior.TimedEffects;
import net.alex.guzhenren.item.gu.mortal.path.rule.AmplifyEffectGuItem;
import net.alex.guzhenren.item.gu.mortal.path.rule.AmplifyEffectGuItem.Amplification;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 蓄力蛊[Store Strength Gu]与惯力蛊[Habitual Strength Gu]的行为：限时增益 {@code effect}，期间空手可蓄力放出蓄力一拳
 * （{@link StoreStrength}），惯力蛊的增益另让蓄力期间可移动；自己的增益未完再催动被拒，
 * 无永久兽力[Beast Strength]和人力者蓄力前即被拒（{@code all_out_needs_branch}，与全力以赴蛊[All-Out Effort Gu]共用）；
 * 功倍蛊[Amplify Effect Gu]乘时长。
 *
 * @author Alex
 * @version 1.0.0
 * @see StoreStrength
 * @since 1.0.0
 */

public record StoreStrengthBehavior(Holder<MobEffect> effect, int effectSeconds) implements GuBehavior {

    private static final String FAILED_NO_BRANCH = "guzhenren.item.failed.all_out_needs_branch";

    @Override
    public boolean canBeAmplified() { return true; }

    @Override
    public @Nullable Refusal checkEffect(MortalGuItem gu, Player player, ItemStack stack) {
        if (TimedEffects.hasBuff(player, effect)) return new Refusal(Refusal.EFFECT_ACTIVE);

        PathStrengthData data = PathStrengthService.get(player);
        return data.hasPathBranch(StrengthPathBranch.BEAST_STRENGTH_PHANTOM)
                || data.hasPathBranch(StrengthPathBranch.HUMAN_JUN_STRENGTH) ? null : new Refusal(FAILED_NO_BRANCH);
    }

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        Amplification amplification = AmplifyEffectGuItem.bill(player, gu);
        int ticks = effectSeconds * Ticks.SECOND * amplification.factor();
        TimedEffects.grantBuff(player, gu, effect, gu.getTier(), ticks, amplification);
    }
}
