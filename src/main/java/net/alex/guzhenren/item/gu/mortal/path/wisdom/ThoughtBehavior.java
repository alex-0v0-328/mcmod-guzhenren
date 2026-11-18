package net.alex.guzhenren.item.gu.mortal.path.wisdom;

import net.alex.guzhenren.gameplay.path.PathEffectType;
import net.alex.guzhenren.gameplay.path.timed.TimedBuff;
import net.alex.guzhenren.item.gu.GuBehavior;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.mortal.behavior.StateEffects;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;

/**
 * 随意蛊[Casual Gu]与恶念蛊[Malicious Thought Gu]的持续灌念行为：在限时内持续向玩家脑海灌入对应念头（与
 * {@link ThoughtBurstBehavior} 配合使用，不设催动拦截门槛）。
 * <ul>
 *   <li>流派效果类型、原版药水效果与持续时长在注册时注入，实际效果等级为「蛊虫档位（一转为 0）+
 *       {@code amplifierOffset}」。</li>
 *   <li>{@link #applyEffect} 委托 {@link StateEffects#grantThought} 先写入流派效果真值，再由心跳投影为原版药水效
 *       果。</li>
 * </ul>
 */

public record ThoughtBehavior(PathEffectType<TimedBuff> type, Holder<MobEffect> effect, int amplifierOffset,
                            int durationTicks) implements GuBehavior {

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        StateEffects.grantThought(player, gu, type, effect, gu.getTier() + amplifierOffset, durationTicks);
    }
}
