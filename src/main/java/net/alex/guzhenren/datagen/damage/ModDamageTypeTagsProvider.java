package net.alex.guzhenren.datagen.damage;

import java.util.concurrent.CompletableFuture;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.registry.damage.ModDamageTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Writes the damage type tags.
 *
 * <p>Extends {@link net.minecraft.data.tags.TagsProvider} for {@link net.minecraft.world.damagesource.DamageType}.
 * All six mod damage types carry the same six bypass tags (armor, effects, enchantments, resistance,
 * shield, no knockback). Only the four lethal checks -- lifespan exhausted, soul collapse, mind ocean
 * shattered and the pressure explosion -- also carry {@code BYPASSES_INVULNERABILITY}, so a Totem of Undying
 * cannot cancel them; the heartbeat deals them only after skipping creative and spectator players. The
 * Ten Extreme disaster and the Vital Gu loss stay without it: the disaster hits every entity in the blast and
 * the loss is a fraction of health, so the tag would only reach creative players and invulnerable entities.
 * The aperture pressure explosion and the disaster also carry {@code IS_EXPLOSION}. Takes the datapack
 * provider's registry lookup so it sees the types generated this run.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.datagen.ModDatapackProvider
 * @since 1.0.0
 */

public class ModDamageTypeTagsProvider extends TagsProvider<DamageType> {

    public ModDamageTypeTagsProvider(PackOutput output,
                                     CompletableFuture<HolderLookup.Provider> lookupProvider,
                                     @Nullable ExistingFileHelper existingFileHelper) {
        super(output, Registries.DAMAGE_TYPE, lookupProvider, Guzhenren.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        tagUnstoppable(DamageTypeTags.BYPASSES_ARMOR);
        tagUnstoppable(DamageTypeTags.BYPASSES_EFFECTS);
        tagUnstoppable(DamageTypeTags.BYPASSES_ENCHANTMENTS);
        tagUnstoppable(DamageTypeTags.BYPASSES_RESISTANCE);
        tagUnstoppable(DamageTypeTags.BYPASSES_SHIELD);
        tagUnstoppable(DamageTypeTags.NO_KNOCKBACK);
        tag(DamageTypeTags.BYPASSES_INVULNERABILITY).add(ModDamageTypes.LIFESPAN_EXHAUSTED,
                ModDamageTypes.SOUL_COLLAPSE,
                ModDamageTypes.MIND_OCEAN_SHATTERED,
                ModDamageTypes.APERTURE_PRESSURE_EXPLOSION);
        tag(DamageTypeTags.IS_EXPLOSION).add(ModDamageTypes.APERTURE_PRESSURE_EXPLOSION,
                ModDamageTypes.TEN_EXTREME_DISASTER);
    }

    private void tagUnstoppable(TagKey<DamageType> tag) {
        tag(tag).add(ModDamageTypes.LIFESPAN_EXHAUSTED,
                ModDamageTypes.SOUL_COLLAPSE,
                ModDamageTypes.MIND_OCEAN_SHATTERED,
                ModDamageTypes.APERTURE_PRESSURE_EXPLOSION,
                ModDamageTypes.TEN_EXTREME_DISASTER,
                ModDamageTypes.VITAL_GU_LOST);
    }
}
