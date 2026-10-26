package net.alex.guzhenren.gameplay.attribute;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.NotNull;

/**
 * The one way this mod writes an attribute: a transient modifier under a fixed id, {@code ADD_VALUE} for
 * {@link AttackDamageService}, {@link MaxHealthService} and {@link KnockbackResistanceService},
 * {@code ADD_MULTIPLIED_BASE} for the Epic Fight
 * stamina cap ({@code compat.EpicFightIntegration}). The shared callers take the three-argument {@link #swap},
 * which adds; the operation names differ between game versions, so only this seam and the loader-side
 * integration spell one out. {@link #setGravityFactor} multiplies an entity's gravity (the plunge [坠击]): the
 * attribute itself is a seam too, vanilla's {@code GRAVITY} here and Forge's entity gravity on 1.20.1, both synced
 * to the client.
 *
 * <p>⚠ Transient on purpose: a permanent modifier is saved into attribute NBT and double-stacks on the
 * next login. ⚠ {@link #swap} is a no-op when the attribute already {@link #hasModifier} that modifier, or holds none
 * and the amount is zero; refreshes run on every write and every heartbeat, so re-issuing the modifier each
 * time would churn the attribute sync.
 *
 * @author Alex
 * @version 1.0.0
 * @see AttackDamageService
 * @see MaxHealthService
 * @since 1.0.0
 */

public final class TransientModifiers {

    private TransientModifiers() {}

    public static void swap(@NotNull AttributeInstance instance, @NotNull ResourceLocation id, double amount) {
        swap(instance, id, amount, AttributeModifier.Operation.ADD_VALUE);
    }

    public static void swap(@NotNull AttributeInstance instance, @NotNull ResourceLocation id, double amount,
            @NotNull AttributeModifier.Operation operation) {
        if (instance.getModifier(id) == null ? amount == 0.0D : hasModifier(instance, id, amount, operation)) return;

        instance.removeModifier(id);
        if (amount != 0.0D) instance.addTransientModifier(new AttributeModifier(id, amount, operation));
    }

    /** Multiplies the entity's gravity by {@code factor} under {@code id}; a factor of 1 takes the modifier off. */
    public static void setGravityFactor(@NotNull LivingEntity entity, @NotNull ResourceLocation id, double factor) {
        AttributeInstance gravity = entity.getAttribute(Attributes.GRAVITY);
        if (gravity != null) swap(gravity, id, factor - 1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    public static boolean hasModifier(@NotNull AttributeInstance instance, @NotNull ResourceLocation id, double amount,
                                      @NotNull AttributeModifier.Operation operation) {
        AttributeModifier held = instance.getModifier(id);
        return held != null && held.amount() == amount && held.operation() == operation;
    }
}
