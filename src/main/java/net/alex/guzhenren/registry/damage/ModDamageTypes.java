package net.alex.guzhenren.registry.damage;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;

/**
 * The keys of this mod's damage types.
 *
 * <p>Resource-key holder (not a DeferredRegister): damage types live in a datapack registry, so this
 * file owns only the {@link ResourceKey}s; the JSON is written at datagen time by a provider that does
 * not exist at runtime. {@code source(entity, type)} is the one factory.
 *
 * <p>⚠ Six types, all carrying the same six tags (five {@code BYPASSES_*} plus {@code NO_KNOCKBACK})
 * but deliberately NOT {@code BYPASSES_INVULNERABILITY} -- creative stays unkillable. The aperture
 * pressure explosion also carries {@code IS_EXPLOSION}.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class ModDamageTypes {

    private ModDamageTypes() {}

    public static final ResourceKey<DamageType> LIFESPAN_EXHAUSTED = key("lifespan_exhausted");
    public static final ResourceKey<DamageType> SOUL_COLLAPSE = key("soul_collapse");
    public static final ResourceKey<DamageType> MIND_OCEAN_SHATTERED = key("mind_ocean_shattered");
    public static final ResourceKey<DamageType> APERTURE_PRESSURE_EXPLOSION = key("aperture_pressure_explosion");
    public static final ResourceKey<DamageType> TEN_EXTREME_DISASTER = key("ten_extreme_disaster");
    public static final ResourceKey<DamageType> VITAL_GU_LOST = key("vital_gu_lost");

    public static DamageSource source(Entity entity, ResourceKey<DamageType> type) {
        return new DamageSource(entity.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(type));
    }

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE,
                Guzhenren.id(name));
    }
}
