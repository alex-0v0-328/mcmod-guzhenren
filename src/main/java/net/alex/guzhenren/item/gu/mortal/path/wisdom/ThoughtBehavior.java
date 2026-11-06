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
 * The thought behavior of the Casual [随意蛊] and Malicious Thought [恶念蛊] Gu: floods the mind for a while. The path
 * effect type, the vanilla effect and the length come from registration; the amplifier is the Gu's tier plus the
 * registered offset, so a higher-rung Gu floods harder.
 *
 * <p>The payout delegates to {@link StateEffects#grantThought}, which lays the path effect [流派效果] of the thoughts
 * and projects the vanilla effect from it. It refuses nothing.
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
