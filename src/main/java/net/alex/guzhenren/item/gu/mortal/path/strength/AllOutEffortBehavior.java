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
 * 全力以赴蛊[All-Out Effort Gu]的行为：自身无效果，限时借力给力道分支。
 * <ul>
 *   <li>三至五转：人力钧力流或兽力虚影流的常态承受力加 50%/100%/200%，与其他加成并列
 *       （{@code StrengthService.getAllOutCapacityBonus}）；不付负载。</li>
 *   <li>兽力虚影流开轮概率 5% 升至 80%/90%/100%（{@code StrengthRound.getChance}），并全面解放抽中兽种。</li>
 *   <li>无永久兽力[Beast Strength]和人力者蓄力前即被拒（{@code all_out_needs_branch}）；效果期间再催动也拒。</li>
 *   <li>按蛊的 tier 打标记效果供力量服务与轮次读，再调 {@link AttackDamageService#refresh}，攻击仍出自一个公式。</li>
 *   <li>功倍蛊[Amplify Effect Gu]乘时长，持有兽种的时长加成（{@code PathStrengthService.getDurationBonus}，青牛 10 秒）
 *       乘后再加。</li>
 *   <li>⚠ 标记不带 {@code AttributeModifier}：修饰符会让同一事实有两个来源。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.tending.TendedGuItem
 * @since 1.0.0
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
