package net.alex.guzhenren.item.gu.mortal.behavior;

import net.alex.guzhenren.effect.timed.CrashGuEffect;
import net.alex.guzhenren.gameplay.path.PathEffect;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthBuff;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.gameplay.path.strength.StrengthPathBranch;
import net.alex.guzhenren.gameplay.path.timed.TimedBuff;
import net.alex.guzhenren.item.GuItem.Refusal;
import net.alex.guzhenren.item.gu.GuBehavior;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.mortal.path.rule.AmplifyEffectGuItem;
import net.alex.guzhenren.item.gu.mortal.path.rule.AmplifyEffectGuItem.Amplification;
import net.alex.guzhenren.item.gu.mortal.path.strength.MarkInterference;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 即时增益行为：为玩家赋予指定类型、时长与等级的限时流派增益（{@link TimedEffects#grantBuff}）。
 * <ul>
 *   <li>挂载本行为的持续照顾型蛊[Tended Gu]均同时挂载 {@link RecastBehavior}（具体蛊虫名单见 wiki《蛊虫条目》）。</li>
 *   <li>兽力类增益（{@link BeastStrengthBuff}）在催动前先经 {@link MarkInterference#check} 检查是否受人力钧力流派互斥拦
 *       截；当玩家身上已有同种药水效果且等级高于本蛊时拒绝催动（避免低转蛊白白扣费），等级相同或更低时则刷新或覆盖旧效
 *       果及其道痕[Path Marks]点数。</li>
 *   <li>在效果冷却期间再次催动同等级增益时累加剩余时长，冷却外催动则取较长时长（{@link #getNextDuration}）；
 *       功倍蛊[Amplify Effect Gu]倍增本次新增的基础时长并附加律道点数；兽力与横冲系增益还会额外加上玩家所持永久兽力的时
 *       长加成（{@link PathStrengthService#getDurationBonus}，如青牛加 10 秒），而力气蛊[Strength Qi Gu]不吃该加
 *       成。</li>
 *   <li>⚠ 复用本行为的多只蛊虫各自在 {@code GuProperties} 中独立声明补食标签，共用行为不代表共用食谱。</li>
 * </ul>
 */

public record TimedBuffBehavior(Holder<MobEffect> buff, int durationTicks, int amplifier) implements GuBehavior {


    public TimedBuffBehavior(Holder<MobEffect> buff, int durationTicks) { this(buff, durationTicks, 0); }

    @Override
    public boolean canBeAmplified() { return true; }

    @Override
    public @Nullable Refusal checkEffect(MortalGuItem gu, Player player, ItemStack stack) {
        if (buff.value() instanceof BeastStrengthBuff) {
            Refusal interference = MarkInterference.check(player, StrengthPathBranch.BEAST_STRENGTH_PHANTOM);
            if (interference != null) return interference;
        }

        PathEffect<TimedBuff> current = TimedEffects.findBuff(player, buff);
        return current != null && current.payload().amplifier() > amplifier
                ? new Refusal(Refusal.EFFECT_ACTIVE) : null;
    }

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        PathEffect<TimedBuff> current = TimedEffects.findBuff(player, buff);
        if (current != null && amplifier < current.payload().amplifier()) return;

        Amplification amplification = AmplifyEffectGuItem.bill(player, gu);
        int added = durationTicks * amplification.factor()
                + (takesDurationBonus(buff.value()) ? PathStrengthService.getDurationBonus(player) : 0);
        int duration = current == null ? added : getNextDuration(TimedEffects.getBuffTicksLeft(player, buff),
                current.payload().amplifier(), added, amplifier, gu.getEffectCooldownLeft(player, stack) > 0);
        TimedEffects.grantBuff(player, gu, buff, amplifier, duration, amplification);
    }

    static boolean takesDurationBonus(MobEffect effect) {
        return effect instanceof BeastStrengthBuff || effect instanceof CrashGuEffect;
    }

    static int getNextDuration(int currentDuration, int currentAmplifier, int addedDuration, int addedAmplifier,
                               boolean accumulates) {
        if (addedAmplifier < currentAmplifier) return currentDuration;
        if (addedAmplifier > currentAmplifier) return addedDuration;
        if (!accumulates) return Math.max(currentDuration, addedDuration);
        return (int) Math.min(Integer.MAX_VALUE, (long) currentDuration + addedDuration);
    }
}
