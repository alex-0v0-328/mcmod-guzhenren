package net.alex.guzhenren.item.gu.mortal.water;

import net.alex.guzhenren.gameplay.body.MugwortShieldService;
import net.alex.guzhenren.item.gu.GuSpec;
import net.alex.guzhenren.item.gu.TendedGuItem;
import net.alex.guzhenren.item.gu.mortal.rule.AmplifyEffectGuItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Heavenly Mugwort Gu [天蓬蛊] item: raises the Heavenly Mugwort shield [天蓬护盾]; its points and length come from
 * registration.
 *
 * <p>Extends {@link TendedGuItem}. Every use fills the shield to its full points and restarts its timer through
 * {@link MugwortShieldService#raise}; a shield still up is replaced, never stacked. Like a buff Gu it may be driven
 * again inside its effect cooldown at double the hunger. An Amplify Effect Gu [功倍蛊] multiplies the length, never
 * the points.
 *
 * @author Alex
 * @version 1.0.0
 * @see MugwortShieldService
 * @since 1.0.0
 */

public final class HeavenlyMugwortGuItem extends TendedGuItem {

    private static final int COOLDOWN_HUNGER_MULTIPLIER = 2;
    private final float shieldPoints;
    private final int durationTicks;

    public HeavenlyMugwortGuItem(Properties properties, float shieldPoints, int durationTicks, GuSpec spec) {
        super(properties, spec);
        this.shieldPoints = shieldPoints;
        this.durationTicks = durationTicks;
    }

    public float getShieldPoints() { return shieldPoints; }

    public int getDurationTicks() { return durationTicks; }

    @Override
    protected @Nullable Refusal checkPayoutGate(Player player, ItemStack stack) { return null; }

    @Override
    protected void payOut(ServerPlayer player, ItemStack stack) {
        MugwortShieldService.raise(player, shieldPoints, durationTicks * AmplifyEffectGuItem.amplify(player, this));
    }

    @Override
    public boolean canBeAmplified() { return true; }

    @Override
    protected boolean canUseDuringEffectCooldown() { return true; }

    @Override
    protected int getHungerCostMultiplier(Player player, ItemStack stack) {
        return getEffectCooldownLeft(player, stack) > 0 ? COOLDOWN_HUNGER_MULTIPLIER : 1;
    }
}
