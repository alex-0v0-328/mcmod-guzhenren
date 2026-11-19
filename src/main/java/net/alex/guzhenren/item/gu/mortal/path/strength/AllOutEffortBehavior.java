package net.alex.guzhenren.item.gu.mortal.path.strength;

import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.path.strength.AttackDamageService;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthData;
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
 * 全力以赴蛊[All-Out Effort Gu]行为：自身不提供独立基础力量，而是在限时内强化玩家已持有的力道流派分支。
 * <ul>
 *   <li>共三至五转三档：使人力钧力流派或兽力虚影流派的常态肉身承受力额外提升 50%/100%/200%（与其他承受力加成并列，见
 *       {@code StrengthService.getAllOutCapacityBonus}），且不计入肉身负载。</li>
 *   <li>对兽力虚影流派，将连招轮收尾的抽取概率由 5% 提升至 80%/90%/100%（{@code ComboRound.getChance}），并使抽中的兽
 *       种获得全面解放。</li>
 *   <li>未持有任何永久兽力或人力的玩家在蓄力起手前即被拒绝（{@code all_out_needs_branch}）；效果持续期间再次催动亦拒
 *       绝。</li>
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

        PathStrengthData data = PathStrengthService.get(player);
        return data.hasPathBranch(StrengthPathBranch.BEAST_STRENGTH_PHANTOM)
                || data.hasPathBranch(StrengthPathBranch.HUMAN_JUN_STRENGTH) ? null : new Refusal(FAILED_NO_BRANCH);
    }

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        Amplification amplification = AmplifyEffectGuItem.bill(player, gu);
        int ticks = effectSeconds * Ticks.SECOND * amplification.factor() + PathStrengthService.getDurationBonus(player);
        TimedEffects.grantBuff(player, gu, ModEffects.ALL_OUT_EFFORT_GU, gu.getTier(), ticks, amplification);
        AttackDamageService.refresh(player);
    }
}
