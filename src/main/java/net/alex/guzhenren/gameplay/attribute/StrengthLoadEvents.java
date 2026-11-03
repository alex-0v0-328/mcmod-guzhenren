package net.alex.guzhenren.gameplay.attribute;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.registry.damage.ModDamageTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * The load [负载] of All-Out Effort [全力以赴]: every hit a player lands while bringing more strength points to bear
 * than the body can carry costs the body {@link StrengthService#getLoadDamage}, dealt back as
 * {@code OVERLOAD_IMPACT}, which goes past armor, skin, shields, absorption and the hurt cooldown, and leaves the
 * player's own hurt frames untouched. The body is never killed by it: the load stops at {@link #HEALTH_FLOOR}.
 * A body that cannot be hurt (creative, {@code /gamemode}'s invulnerability, a spectator) pays no load at all:
 * the type carries no {@code BYPASSES_INVULNERABILITY}, like the Ten Extreme disaster, and the health clamp
 * below must not become a back door for what {@code hurt} refused.
 *
 * <p>Reads {@link LivingDamageEvent.Post} with the player as the DIRECT entity, so only a hit of the body itself
 * that did damage counts: a swing into the air or a hit a shield turned does not, and neither does an arrow, a
 * thrown potion or a Moon Blade, which carry none of the body's strength. The load's own hit has no entity and
 * cannot feed itself.
 *
 * @author Alex
 * @version 1.0.0
 * @see StrengthService
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class StrengthLoadEvents {

    private StrengthLoadEvents() {}

    public static final float HEALTH_FLOOR = 1.0F;

    @SubscribeEvent
    public static void onDamageDealt(LivingDamageEvent.Post event) {
        if (!(event.getSource().getDirectEntity() instanceof ServerPlayer player) || event.getEntity() == player) return;
        if (event.getNewDamage() <= 0.0F) return;

        float before = player.getHealth();
        float load = Math.min(StrengthService.getLoadDamage(player), before - HEALTH_FLOOR);
        if (load <= 0.0F) return;
        DamageSource source = ModDamageTypes.source(player, ModDamageTypes.OVERLOAD_IMPACT);
        if (player.getAbilities().invulnerable || player.isInvulnerableTo(source)) return;

        // The load tears the body itself: absorption hearts do not soak it, and the hit leaves the player's own
        // hurt frames and last-hurt bookkeeping as they were, so the next mob hit is settled as if it never happened.
        float absorption = player.getAbsorptionAmount();
        int frames = player.invulnerableTime;
        player.setAbsorptionAmount(0.0F);
        player.hurt(source, load);
        player.setAbsorptionAmount(absorption);
        player.invulnerableTime = frames;
        float target = Math.max(HEALTH_FLOOR, before - load);
        if (player.getHealth() > target) player.setHealth(target);
    }
}
