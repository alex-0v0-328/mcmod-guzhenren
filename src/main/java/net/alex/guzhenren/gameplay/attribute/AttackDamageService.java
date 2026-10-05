package net.alex.guzhenren.gameplay.attribute;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.body.BodyData;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.BodyStrengthService;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.HumanStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthData;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * The only thing in the mod that touches the {@code ATTACK_DAMAGE} attribute -- {@code getBonus()} is
 * the whole sum: {@link AttackContributor} timed effects [力道], beast strengths [兽力], the usable-jin
 * ramp, and the zombie [僵] tier bonus. {@link AttributeEvents} refreshes it when a physique, a strength
 * or an effect changes; the heartbeat refreshes it every second.
 *
 * <p>⚠ The modifier MUST stay transient ({@link TransientModifiers}). ⚠ No effect may declare its own
 * {@code addAttributeModifier} -- {@code getBonus()} already counts it via
 * {@link AttackContributor#attackBonus}. ⚠ The zombie bonus rides {@code BodyData.zombieTier}, NOT a
 * MobEffect (permanent 僵 has none; command tier -1 gets NO attack).
 *
 * <p>{@link #showsImpactRing(double)} tests the attack-panel value against
 * {@link #IMPACT_RING_ATTACK_THRESHOLD}, at and above which a landed Epic Fight bare-hand/fist punch
 * spawns the shockwave ring; read as the panel number, so only fist-category
 * weapon damage ever rides along with the strength bonus.
 *
 * @author Alex
 * @version 1.0.0
 * @see MaxHealthService
 * @see BodyStrengthService
 * @since 1.0.0
 */

public final class AttackDamageService {

    private AttackDamageService() {}

    private static final ResourceLocation MODIFIER_ID =
            Guzhenren.id("strength_attack_damage");
    public static final double ZOMBIE_ATTACK_BASE = 5.0D;
    public static final double IMPACT_RING_ATTACK_THRESHOLD = 16.0D;

    public static boolean showsImpactRing(double attackDamage) {
        return attackDamage >= IMPACT_RING_ATTACK_THRESHOLD;
    }

    public static double getBonus(@NotNull Player player) {
        PathStrengthData data = PathStrengthService.get(player);
        double total = 0.0D;

        for (BeastStrength beast : BeastStrength.values()) {
            if (data.has(beast)) total += beast.getAttackBonus();
        }
        return total + BodyStrengthService.getUsableJin(player) * HumanStrength.ATTACK_PER_JIN
                + getZombieBonus(player) + getEffectBonus(player);
    }

    public static double getEffectBonus(@NotNull Player player) {
        double total = 0.0D;
        for (MobEffectInstance instance : player.getActiveEffects()) {
            if (instance.getEffect().value() instanceof AttackContributor contributor) {
                total += contributor.attackBonus(instance.getAmplifier());
            }
        }
        return total;
    }

    public static double getZombieBonus(@NotNull Player player) {
        BodyData body = BodyService.get(player);
        if (!body.isZombieOrHalfZombie() || body.zombieTier() < 0) return 0.0D;

        return ZOMBIE_ATTACK_BASE * (1 << body.zombieTier());
    }

    public static void refresh(@NotNull ServerPlayer player) {
        AttributeInstance instance = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (instance == null) return;
        TransientModifiers.swap(instance, MODIFIER_ID, getBonus(player));
    }
}
