package net.alex.guzhenren.datagen.fluid;

import java.util.concurrent.CompletableFuture;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.registry.fluid.ModFluids;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.FluidTagsProvider;
import net.minecraft.tags.FluidTags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Writes the fluid tags. Both Pure Water [纯水] entries join {@code #minecraft:water}, which is what makes vanilla's
 * block-side water checks -- sponges, concrete powder, bottles, fishing, coral, kelp, lava turning to stone --
 * treat it as water. The Spirit Spring [元泉] stays out: it only feels like water.
 */

public class ModFluidTagsProvider extends FluidTagsProvider {

    public ModFluidTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                                @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, Guzhenren.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        tag(FluidTags.WATER).add(ModFluids.PURE_WATER.get(), ModFluids.FLOWING_PURE_WATER.get());
    }
}
