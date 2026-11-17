package net.alex.guzhenren.datagen.block;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.registry.block.ModBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * 本模组方块的方块状态与模型。
 *
 * <ul>
 *   <li>流体方块只要粒子模型，同原版 {@code block/water}：一个变体指向唯一贴图为静止条带的模型；液面由流体渲染器按
 *       {@code FluidType} 贴图画（{@link net.alex.guzhenren.client.fluid.SpiritSpringClientExtensions}）。</li>
 *   <li>花用原版镂空 {@code block/cross}；九叶生机草[Nine Leaf Vitality Grass]用其物品 {@code _full} PNG，方块图集也拼
 *       接 {@code item/}，一图两用。</li>
 *   <li>双层植物按 {@code half} 每半一个原版 {@code block/cross} 模型，贴图自带颜色，不着色。</li>
 *   <li>作物按 {@code age} 每阶段一个原版井字形 {@code block/crop} 模型；知心草作物的 8 张阶段图沿用资产名
 *       {@code intimate_grass_stage0} 至 {@code 7}，模型名带方块 ID。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public class ModBlockStateProvider extends BlockStateProvider {

    public ModBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Guzhenren.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        registerFluidBlock(ModBlocks.SPIRIT_SPRING.get(), Guzhenren.id("block/spirit_spring_still"));
        registerFluidBlock(ModBlocks.PURE_WATER.get(), Guzhenren.id("block/pure_water_still"));
        registerFlowerBlock(ModBlocks.NINE_LEAF_VITALITY_GRASS_3.get(),
                Guzhenren.id("item/nine_leaf_vitality_grass_3_full"));
        registerDoublePlantBlock(ModBlocks.INTIMATE_GRASS.get());
        registerCropBlock(ModBlocks.INTIMATE_GRASS_CROP.get(), "intimate_grass");
        simpleBlock(ModBlocks.STEEL_BLOCK.get());
    }

    private void registerFluidBlock(Block block, ResourceLocation particle) {
        simpleBlock(block, models().getBuilder(BuiltInRegistries.BLOCK.getKey(block).getPath())
                .texture("particle", particle));
    }

    private void registerFlowerBlock(Block block, ResourceLocation cross) {
        simpleBlock(block, models().cross(BuiltInRegistries.BLOCK.getKey(block).getPath(), cross)
                .renderType("cutout"));
    }

    //region the double plant [双层植物]
    private void registerDoublePlantBlock(Block block) {
        ModelFile bottom = buildHalfModel(block, "_bottom");
        ModelFile top = buildHalfModel(block, "_top");
        getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER ? top : bottom)
                .build());
    }

    private ModelFile buildHalfModel(Block block, String suffix) {
        String name = BuiltInRegistries.BLOCK.getKey(block).getPath() + suffix;
        return models().cross(name, modLoc("block/" + name)).renderType("cutout");
    }
    //endregion

    //region the crop [作物]
    private void registerCropBlock(CropBlock block, String texture) {
        getVariantBuilder(block).forAllStates(state -> {
            int age = block.getAge(state);
            String name = BuiltInRegistries.BLOCK.getKey(block).getPath() + "_stage" + age;
            return ConfiguredModel.builder()
                    .modelFile(models().crop(name, modLoc("block/" + texture + "_stage" + age)).renderType("cutout"))
                    .build();
        });
    }
    //endregion
}
