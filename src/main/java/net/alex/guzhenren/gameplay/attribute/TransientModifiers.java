package net.alex.guzhenren.gameplay.attribute;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.jetbrains.annotations.NotNull;

/**
 * The one way this mod writes an attribute: a transient {@code ADD_VALUE} modifier under a fixed id,
 * shared by {@link AttackDamageService} and {@link MaxHealthService}.
 *
 * <p>⚠ Transient on purpose: a permanent modifier is saved into attribute NBT and double-stacks on the
 * next login. ⚠ {@link #swap} is a no-op when the amount has not moved; refreshes run on every write
 * and every heartbeat, so re-issuing the modifier each time would churn the attribute sync.
 *
 * @author Alex
 * @version 1.0.0
 * @see AttackDamageService
 * @see MaxHealthService
 * @since 1.0.0
 */

final class TransientModifiers {

    private TransientModifiers() {}

    static void swap(@NotNull AttributeInstance instance, @NotNull ResourceLocation id, double amount) {
        AttributeModifier held = instance.getModifier(id);
        if (held == null ? amount == 0.0D : held.amount() == amount) return;

        instance.removeModifier(id);
        if (amount != 0.0D) {
            instance.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
        }
    }
}
