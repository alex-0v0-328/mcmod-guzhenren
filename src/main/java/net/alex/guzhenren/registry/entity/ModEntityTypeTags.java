package net.alex.guzhenren.registry.entity;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

/**
 * The entity-type tags this mod declares.
 *
 * <p>{@link #PREDATOR_PREY} lists the vanilla farm and small animals that hunting beasts (the Asian
 * black bear, tigers) proactively attack. Mod creatures never proactively target each other.
 */

public final class ModEntityTypeTags {

    private ModEntityTypeTags() {}

    public static final TagKey<EntityType<?>> PREDATOR_PREY = TagKey.create(
            Registries.ENTITY_TYPE, Guzhenren.id("predator_prey"));
}
