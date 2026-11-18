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
 * 本模组流体标签数据生成器：将纯水的源流与正流加入 {@code #minecraft:water}，使海绵吸水、混凝土粉末凝固、玻璃瓶装水、钓
 * 鱼、水生植物存活及岩浆下流成石等原版方块侧水体判定将其视作水；元泉[Spirit Spring]则不加入该标签，仅保持与水相似的物理
 * 流动手感。
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
