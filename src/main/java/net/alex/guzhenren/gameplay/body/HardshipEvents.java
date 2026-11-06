package net.alex.guzhenren.gameplay.body;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

/**
 * The Hardship Strength Gu's [苦力蛊] price: while its effect runs, {@code onHeal} cuts every heal -- natural regen,
 * Self Reliance [自力更生], the Vitality Leaf [生机叶] and any other {@link LivingHealEvent} -- to the room under
 * {@link #HEAL_CEILING} ({@link #clampHeal}), and cancels it at or above the ceiling. Health already above the
 * ceiling is left alone. Self Reliance reads the same ceiling itself ({@code SelfRelianceBehavior.getCeiling}) so
 * its round ends there instead of pulsing into cancelled heals. The Grievous Injury [重伤] marker follows the
 * Hardship effect: added with its remaining duration when it appears, removed when it is removed or runs out.
 *
 * <p>⚠ NeoForge posts the effect events BEFORE the effect map changes, and {@code Expired} fires inside the
 * map's iteration, so the marker is queued as a {@link TickTask} rather than written in the event. ⚠ The
 * ceiling reads the Hardship effect, never the marker: {@code /effect clear} takes both icons until the heartbeat
 * re-projects the buff, which brings the marker back through {@code Added}.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.effect.timed.HardshipStrengthGuEffect
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class HardshipEvents {

    private HardshipEvents() {}

    public static final float HEAL_CEILING = 0.5F;

    /** The heal that may still land: {@code amount} cut to the room under the ceiling, zero at or above it. */
    public static float clampHeal(float health, float maxHealth, float amount) {
        float room = maxHealth * HEAL_CEILING - health;
        return Math.max(0.0F, Math.min(amount, room));
    }

    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof Player player) || !player.hasEffect(ModEffects.HARDSHIP_STRENGTH_GU)) return;

        float allowed = clampHeal(player.getHealth(), player.getMaxHealth(), event.getAmount());
        if (allowed <= 0.0F) {
            event.setCanceled(true);
        } else {
            event.setAmount(allowed);
        }
    }

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        if (event.getEffectInstance().is(ModEffects.HARDSHIP_STRENGTH_GU)) syncInjury(event.getEntity());
    }

    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        MobEffectInstance instance = event.getEffectInstance();
        if (instance != null && instance.is(ModEffects.HARDSHIP_STRENGTH_GU)) syncInjury(event.getEntity());
    }

    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        MobEffectInstance instance = event.getEffectInstance();
        if (instance != null && instance.is(ModEffects.HARDSHIP_STRENGTH_GU)) syncInjury(event.getEntity());
    }

    private static void syncInjury(LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) return;
        player.server.tell(new TickTask(player.server.getTickCount(), () -> projectInjury(player)));
    }

    private static void projectInjury(ServerPlayer player) {
        MobEffectInstance hardship = player.getEffect(ModEffects.HARDSHIP_STRENGTH_GU);
        if (hardship == null) {
            player.removeEffect(ModEffects.GRIEVOUS_INJURY);
            return;
        }
        MobEffectInstance injury = player.getEffect(ModEffects.GRIEVOUS_INJURY);
        if (injury == null || injury.getDuration() != hardship.getDuration()) {
            player.addEffect(ModEffects.instance(ModEffects.GRIEVOUS_INJURY, hardship.getDuration()));
        }
    }
}
