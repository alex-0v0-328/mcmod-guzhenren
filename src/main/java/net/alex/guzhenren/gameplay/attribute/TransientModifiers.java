package net.alex.guzhenren.gameplay.attribute;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.jetbrains.annotations.NotNull;

/**
 * The one way this mod writes an attribute: a transient modifier under a fixed id, {@code ADD_VALUE} for
 * {@link AttackDamageService} and {@link MaxHealthService}, {@code ADD_MULTIPLIED_BASE} for the Epic Fight
 * stamina cap ({@code compat.EpicFightIntegration}). The shared callers take the three-argument {@link #swap},
 * which adds; the operation names differ between game versions, so only this seam and the loader-side
 * integration spell one out.
 *
 * <p>⚠ Transient on purpose: a permanent modifier is saved into attribute NBT and double-stacks on the
 * next login. ⚠ {@link #swap} is a no-op when the attribute already {@link #hasModifier} that modifier, or holds none
 * and the amount is zero; refreshes run on every write and every heartbeat, so re-issuing the modifier each
 * time would churn the attribute sync.
 *
 * <p>⚠ A version seam: callers name a modifier by {@link ResourceLocation} on both sides. 1.20.1 keys
 * modifiers by {@link UUID}, so {@link #uuid} derives a stable one from the id; the same id always lands
 * on the same modifier, so a swap replaces instead of stacking.
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
        swap(instance, id, amount, AttributeModifier.Operation.ADDITION);
    }

    public static void swap(@NotNull AttributeInstance instance, @NotNull ResourceLocation id, double amount,
            @NotNull AttributeModifier.Operation operation) {
        UUID uuid = uuid(id);
        if (instance.getModifier(uuid) == null ? amount == 0.0D : hasModifier(instance, id, amount, operation)) return;

        instance.removeModifier(uuid);
        if (amount != 0.0D) {
            instance.addTransientModifier(new AttributeModifier(uuid, id.toString(), amount, operation));
        }
    }

    public static boolean hasModifier(@NotNull AttributeInstance instance, @NotNull ResourceLocation id, double amount,
                                      @NotNull AttributeModifier.Operation operation) {
        AttributeModifier held = instance.getModifier(uuid(id));
        return held != null && held.getAmount() == amount && held.getOperation() == operation;
    }

    static @NotNull UUID uuid(@NotNull ResourceLocation id) {
        return UUID.nameUUIDFromBytes(id.toString().getBytes(StandardCharsets.UTF_8));
    }
}
