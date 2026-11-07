package net.alex.guzhenren.gameplay.path.strength;

import net.alex.guzhenren.gameplay.attribute.StrengthService;
import net.alex.guzhenren.gameplay.attribute.StrengthSnapshot;
import net.alex.guzhenren.registry.damage.ModDamageTypes;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * The body overload [肉体过载]: the timed beast strength (the Flower Boar Gu's boar, the Brute Force Longhorn Beetle
 * Gu's ox) carries no capacity of its own, so the part of it past the current capacity
 * ({@code StrengthSnapshot.overloadPoints}) is brought to bear in full and paid for in health. Every fist hit that
 * opens or shares an attack round ({@link BeastPhantomEvents}'s gate, not once a round; a miss costs nothing) deals
 * the attacker {@link #getRecoil}: {@code overload / capacity * max health * }{@link #RECOIL_RATIO}, as
 * {@code guzhenren:body_overload}, which goes past armor and has no attacker.
 *
 * <p>It never kills: {@link #getDamage} stops the health at {@link #HEALTH_FLOOR}, and a body already at or under it
 * pays nothing. The recoil neither consumes nor grants the attacker's hurt frames -- they are cleared for the hit and
 * put back after it -- so a steady fist pays every hit and a mob's blow is not swallowed by the recoil's frames.
 *
 * @author Alex
 * @version 1.0.0
 * @see StrengthService#getOverloadPoints
 * @since 1.0.0
 */

public final class BodyOverload {

    private BodyOverload() {}

    public static final float RECOIL_RATIO = 0.5F;
    public static final float HEALTH_FLOOR = 2.0F;

    public static float recoil(@NotNull ServerPlayer player) {
        StrengthSnapshot snapshot = StrengthService.snapshot(player);
        float damage = getDamage(getRecoil(snapshot.overloadPoints(), snapshot.capacity(), player.getMaxHealth()),
                player.getHealth());
        if (damage <= 0.0F) return 0.0F;

        int hurtFrames = player.invulnerableTime;
        player.invulnerableTime = 0;
        boolean hurt = player.hurt(ModDamageTypes.source(player, ModDamageTypes.BODY_OVERLOAD), damage);
        player.invulnerableTime = hurtFrames;
        return hurt ? damage : 0.0F;
    }

    public static float getRecoil(int overloadPoints, int capacity, float maxHealth) {
        if (overloadPoints <= 0 || capacity <= 0 || maxHealth <= 0.0F) return 0.0F;
        return (float) overloadPoints / capacity * maxHealth * RECOIL_RATIO;
    }

    public static float getDamage(float recoil, float health) {
        if (recoil <= 0.0F || health <= HEALTH_FLOOR) return 0.0F;
        return Math.min(recoil, health - HEALTH_FLOOR);
    }
}
