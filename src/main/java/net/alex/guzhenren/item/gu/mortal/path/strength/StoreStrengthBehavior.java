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
 * 蓄力蛊[Store Strength Gu]与惯力蛊[Habitual Strength Gu]行为：赋予玩家限时增益 {@code effect}，持续期间空手可按键蓄力
 * 并释放蓄力一拳（{@link StoreStrength}，其中惯力蛊增益允许在蓄力期间移动）；自身同种增益尚未结束时拒绝重复催动，未持有
 * 任何永久兽力或人力的玩家在蓄力起手前即被拒绝（与全力以赴蛊[All-Out Effort Gu]共用 {@code all_out_needs_branch} 提示
 * 键），支持功倍蛊[Amplify Effect Gu]倍增持续时长。
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
