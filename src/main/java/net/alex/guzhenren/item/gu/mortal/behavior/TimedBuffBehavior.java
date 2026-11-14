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
 * 即时增益行为：授予限时增益，效果、时长、强度由注册定。
 * <ul>
 *   <li>带此行为的蛊各配 {@link RecastBehavior}，名单见 wiki《蛊虫条目》。</li>
 *   <li>效果仅授予增益（{@link TimedEffects#grantBuff}）；兽力增益遇人力钧力流先拒（{@link MarkInterference}）；
 *       门槛仅在同效果更强副本运行时拒绝，免弱蛊白付；相等或更弱副本被刷新，强蛊连弱蛊效果与丝取代。</li>
 *   <li>效果冷却内同强度叠时长，冷却外取较长（{@link #getNextDuration}）；功倍蛊[Amplify Effect Gu]
 *       倍增所加时长并挂其律道[Rule Path]丝；兽力、冲撞蛊增益再加所持兽时长
 *       （{@code PathStrengthService.getDurationBonus}，青牛 10 秒），力气蛊不加。</li>
 *   <li>⚠ 各蛊仍各声明补食标签：共用行为不等于共用食谱。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.tending.TendedGuItem
 * @since 1.0.0
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
