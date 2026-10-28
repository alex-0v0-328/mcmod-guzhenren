package net.alex.guzhenren.item.gu.mortal.strength;

import net.alex.guzhenren.item.gu.GuSpec;
import net.alex.guzhenren.item.gu.mortal.BuffGuItem;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Self Reliance Gu [自力更生蛊] item: a tended buff Gu that drives ITSELF at the brink.
 *
 * <p>{@code tryAutoUse} is the heartbeat's call -- when the holder is under 20% health and not
 * already running the effect, the first copy in the inventory fires on its own.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.effect.timed.SelfRelianceGuEffect
 * @since 1.0.0
 */

public final class SelfRelianceGuItem extends BuffGuItem {

    private static final float AUTO_USE_HEALTH_FRACTION = 0.2F;

    public SelfRelianceGuItem(Properties properties, int durationTicks, int amplifier, GuSpec spec) {
        super(properties, ModEffects.SELF_RELIANCE_GU, durationTicks, amplifier, spec);
    }

    public static void tryAutoUse(ServerPlayer player) {
        if (player.getHealth() >= player.getMaxHealth() * AUTO_USE_HEALTH_FRACTION
                || player.hasEffect(ModEffects.SELF_RELIANCE_GU)) return;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof SelfRelianceGuItem item && item.tryAutoUse(player, stack)) return;
        }
    }
}
