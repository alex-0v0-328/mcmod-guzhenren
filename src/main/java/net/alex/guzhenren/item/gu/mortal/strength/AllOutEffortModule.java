package net.alex.guzhenren.item.gu.mortal.strength;

import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.attribute.AttackDamageService;
import net.alex.guzhenren.item.GuItem.Refusal;
import net.alex.guzhenren.item.gu.GuModule;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.mortal.TimedEffects;
import net.alex.guzhenren.item.gu.mortal.rule.AmplifyEffectGuItem;
import net.alex.guzhenren.item.gu.mortal.rule.AmplifyEffectGuItem.Amplification;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The All-Out Effort Gu's [全力以赴蛊] module: for a while, the body's carrying limit [承受上限] stops applying.
 *
 * <p>Three rungs register it (三转至五转). The effect stamps a marker effect at the Gu's tier and calls
 * {@link AttackDamageService#refresh}; the lift itself is read back by the strength service, so attack still comes
 * out of one formula. An Amplify Effect Gu [功倍蛊] multiplies its length.
 *
 * <p>⚠ Its effect is a marker carrying no {@code AttributeModifier}. A modifier would be a second
 * source for one fact, and re-using it while it runs is a refusal.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.TendedGuItem
 * @since 1.0.0
 */

public record AllOutEffortModule(int effectSeconds) implements GuModule {

    private static final String FAILED_ALREADY_UNLEASHED = "guzhenren.item.failed.all_out_active";

    @Override
    public boolean canBeAmplified() { return true; }

    @Override
    public @Nullable Refusal checkEffect(MortalGuItem gu, Player player, ItemStack stack) {
        return TimedEffects.hasBuff(player, ModEffects.ALL_OUT_EFFORT_GU)
                ? new Refusal(FAILED_ALREADY_UNLEASHED)
                : null;
    }

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        Amplification amplification = AmplifyEffectGuItem.bill(player, gu);
        int ticks = effectSeconds * Ticks.SECOND * amplification.factor();
        TimedEffects.grantBuff(player, gu, ModEffects.ALL_OUT_EFFORT_GU, gu.getTier(), ticks, amplification);
        AttackDamageService.refresh(player);
    }
}
