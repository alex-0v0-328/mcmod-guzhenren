package net.alex.guzhenren.item.gu.mortal.path.strength;

import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.path.strength.AttackDamageService;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.gameplay.path.strength.StrengthPathBranch;
import net.alex.guzhenren.item.GuItem.Refusal;
import net.alex.guzhenren.item.gu.GuBehavior;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.mortal.behavior.TimedEffects;
import net.alex.guzhenren.item.gu.mortal.path.rule.AmplifyEffectGuItem.Amplification;
import net.alex.guzhenren.item.gu.mortal.path.rule.AmplifyEffectGuItem;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 全力以赴蛊[All-Out Effort Gu]行为：自身不提供力量点或承受力，只在限时内抬高已入力道流派分支玩家的暴击率[Crit
 * Chance]。
 * <ul>
 *   <li>共三至五转三档：人力钧力流派与兽力虚影流派共用的暴击率由平时 5%（另加兽力加成）提升至 80% / 90% / 100%，此时不
 *       再加兽力加成（{@code StrengthCrit.getChance}，经 {@code StrengthService.getAllOutTier} 读档位）；连招轮收尾与蓄
 *       力一拳都读它，蓄力一拳蓄满本就必暴击。每次出几种兽影由苦力蛊决定。</li>
 *   <li>未入任何力道分支（既无永久兽力或人力，也无生效中的限时兽力）的玩家在蓄力起手前即被拒绝
 *       （{@code all_out_needs_branch}）；效果持续期间再次催动亦拒绝。</li>
 *   <li>生效时按蛊虫档位挂载标记效果供力量服务与连招轮读取，并调用 {@link AttackDamageService#refresh} 重算攻击力，保
 *       证攻击力始终由单一公式派生；功倍蛊[Amplify Effect Gu]先倍增基础时长，之后再加上玩家所持永久兽力的时长加成
 *       （{@link PathStrengthService#getDurationBonus}，如青牛加 10 秒）。</li>
 *   <li>⚠ 该药水效果仅作状态标记，绝不直接挂载原版 {@code AttributeModifier}，以免与 {@link AttackDamageService} 产生双
 *       重属性来源。</li>
 * </ul>
 */

public record AllOutEffortBehavior(int effectSeconds) implements GuBehavior {

    private static final String FAILED_NO_BRANCH = "guzhenren.item.failed.all_out_needs_branch";

    @Override
    public boolean canBeAmplified() { return true; }

    @Override
    public @Nullable Refusal checkEffect(MortalGuItem gu, Player player, ItemStack stack) {
        if (TimedEffects.hasBuff(player, ModEffects.ALL_OUT_EFFORT_GU)) return new Refusal(Refusal.EFFECT_ACTIVE);

        return PathStrengthService.getHeldBranch(player) != StrengthPathBranch.NORMAL
                ? null : new Refusal(FAILED_NO_BRANCH);
    }

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        Amplification amplification = AmplifyEffectGuItem.bill(player, gu);
        int ticks = effectSeconds * Ticks.SECOND * amplification.factor() + PathStrengthService.getDurationBonus(player);
        TimedEffects.grantBuff(player, gu, ModEffects.ALL_OUT_EFFORT_GU, gu.getTier(), ticks, amplification);
        AttackDamageService.refresh(player);
    }
}
