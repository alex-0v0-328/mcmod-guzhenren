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
 * 流体标签：纯水两条目加入 {@code #minecraft:water}，原版方块侧水判定（海绵、混凝土粉末、玻璃瓶、钓鱼、珊瑚、海带、熔岩
 * 变石）才当它是水；元泉[Spirit Spring]不加入，只是手感像水。
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
