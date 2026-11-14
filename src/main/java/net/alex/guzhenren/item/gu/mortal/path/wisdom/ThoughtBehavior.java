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
 * 随意蛊[Casual Gu]、恶念蛊[Malicious Thought Gu]的念头行为：限时灌念头；不拒催动。
 * <ul>
 *   <li>流派效果、原版效果、时长取自注册；效果等级 = 级数（一转为 0）+ 注册偏移。</li>
 *   <li>效果交 {@link StateEffects#grantThought}：先铺流派效果，再投射原版效果。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see ThoughtBurstBehavior
 * @since 1.0.0
 */

public record ThoughtBehavior(PathEffectType<TimedBuff> type, Holder<MobEffect> effect, int amplifierOffset,
                            int durationTicks) implements GuBehavior {

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        StateEffects.grantThought(player, gu, type, effect, gu.getTier() + amplifierOffset, durationTicks);
    }
}
