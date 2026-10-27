package net.alex.guzhenren.item.gu.mortal.strength;

import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.attribute.AttackDamageService;
import net.alex.guzhenren.item.gu.GuSpec;
import net.alex.guzhenren.item.gu.TendedGuItem;
import net.alex.guzhenren.item.gu.mortal.rule.AmplifyEffectGuItem;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * All-Out Effort Gu [全力以赴蛊]: for a while, the body's carrying limit [承受上限] stops applying.
 *
 * <p>Extends {@link net.alex.guzhenren.item.gu.TendedGuItem}. Three rungs register against this one
 * class (三转至五转). The payout stamps a marker effect and calls
 * {@link AttackDamageService#refresh}; the lift itself is read
 * back by the strength service, so attack still comes out of one formula. An Amplify Effect Gu [功倍蛊] in the
 * offhand multiplies its length.
 *
 * <p>⚠ Its effect is a marker carrying no {@code AttributeModifier}. A modifier would be a second
 * source for one fact, and re-using it while it runs is a refusal.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.TendedGuItem
 * @since 1.0.0
 */

public class AllOutEffortGuItem extends TendedGuItem {

    private static final String FAILED_ALREADY_UNLEASHED = "guzhenren.item.failed.all_out_active";
    private final int effectSeconds;

    public AllOutEffortGuItem(Properties properties, int effectSeconds, GuSpec spec) {
        super(properties, spec);
        this.effectSeconds = effectSeconds;
    }

    @Override
    protected @Nullable Refusal checkPayoutGate(Player player, ItemStack stack) {
        return ModEffects.has(player, ModEffects.ALL_OUT_EFFORT) ? new Refusal(FAILED_ALREADY_UNLEASHED) : null;
    }

    @Override
    protected void payOut(ServerPlayer player, ItemStack stack) {
        int ticks = effectSeconds * Ticks.SECOND * AmplifyEffectGuItem.amplify(player, this);
        player.addEffect(ModEffects.instance(ModEffects.ALL_OUT_EFFORT, ticks, getTier()));
        AttackDamageService.refresh(player);
    }
}
