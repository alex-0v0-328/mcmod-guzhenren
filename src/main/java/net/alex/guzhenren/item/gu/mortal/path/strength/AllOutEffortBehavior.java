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
 * The All-Out Effort Gu's [全力以赴蛊] behavior: no effect of its own, only what it lends a strength branch for a
 * while. A body holding the human jun [人力钧力流] or the beast phantom [兽力虚影流] branch gains 50/100/200% of its
 * normal carrying capacity [常态承受力] for the rank three/four/five Gu ({@code StrengthService.getAllOutCapacityBonus}),
 * added on the normal one beside every other bonus; a beast phantom round's roll rises from 5% to 80/90/100%
 * ({@code StrengthRound.getChance}) and fully releases [全面解放] what it draws. No load is paid.
 *
 * <p>Three rungs register it (三转至五转). A body with neither a permanent beast strength nor human strength is
 * refused before any charge ({@code all_out_needs_branch}). The effect stamps a marker effect at the Gu's tier, which
 * the strength service and the round read, and calls {@link AttackDamageService#refresh}, so attack still comes out of
 * one formula. An Amplify Effect Gu [功倍蛊] multiplies its length; the held beasts' duration bonus
 * ({@code PathStrengthService.getDurationBonus}, the green bull's ten seconds) is added after that.
 *
 * <p>⚠ Its effect is a marker carrying no {@code AttributeModifier}. A modifier would be a second
 * source for one fact, and re-using it while it runs is a refusal.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.tending.TendedGuItem
 * @since 1.0.0
 */

public record AllOutEffortBehavior(int effectSeconds) implements GuBehavior {

    private static final String FAILED_ALREADY_UNLEASHED = "guzhenren.item.failed.all_out_active";
    private static final String FAILED_NO_BRANCH = "guzhenren.item.failed.all_out_needs_branch";

    @Override
    public boolean canBeAmplified() { return true; }

    @Override
    public @Nullable Refusal checkEffect(MortalGuItem gu, Player player, ItemStack stack) {
        if (TimedEffects.hasBuff(player, ModEffects.ALL_OUT_EFFORT_GU)) return new Refusal(FAILED_ALREADY_UNLEASHED);

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
