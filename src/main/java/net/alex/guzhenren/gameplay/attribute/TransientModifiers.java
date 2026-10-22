package net.alex.guzhenren.gameplay.attribute;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
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

final class TransientModifiers {

    private TransientModifiers() {}

    static void swap(@NotNull AttributeInstance instance, @NotNull ResourceLocation id, double amount) {
        UUID uuid = uuid(id);
        AttributeModifier held = instance.getModifier(uuid);
        if (held == null ? amount == 0.0D : held.getAmount() == amount) return;

        instance.removeModifier(uuid);
        if (amount != 0.0D) {
            instance.addTransientModifier(new AttributeModifier(uuid, id.toString(), amount,
                    AttributeModifier.Operation.ADDITION));
        }
    }

    static @NotNull UUID uuid(@NotNull ResourceLocation id) {
        return UUID.nameUUIDFromBytes(id.toString().getBytes(StandardCharsets.UTF_8));
    }
}
