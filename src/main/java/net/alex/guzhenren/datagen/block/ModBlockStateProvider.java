package net.alex.guzhenren.datagen.block;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.secretrealm.GutsStones;
import net.alex.guzhenren.registry.block.ModBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * 本模组方块的方块状态（BlockState）与方块模型数据生成器。
 * <ul>
 *   <li>流体方块（元泉[Spirit Spring]与纯水）对齐原版 {@code block/water}，仅生成声明静止条带粒子贴图的单变体模型，实际
 *       液面由流体渲染器根据 {@code FluidType} 客户端扩展绘制
 *       （{@link net.alex.guzhenren.client.fluid.SpiritSpringClientExtensions}）。</li>
 *   <li>单格植物（三转九叶生机草[Nine Leaf Vitality Grass]）使用原版镂空 {@code block/cross} 模板，并直接复用其物品的
 *       {@code _full} 贴图；双层植物（野生知心草）按上下半格分别生成独立的 {@code block/cross} 镂空模型。</li>
 *   <li>知心草作物按 {@code age}（0–7）生成 8 个原版井字形 {@code block/crop} 模型（共用
 *       {@code intimate_grass_stage0..7} 贴图）；黑土耕地对齐原版耕地生成干、湿两个继承自
 *       {@code block/template_farmland} 的模型，仅在 {@code moisture} 满值时切换为湿润顶面。</li>
 *   <li>荡魂山石生成 {@code soulquake_stone}、{@code _1}、{@code _2} 三个半透明 {@code cube_all} 模型并等权随机，大面积
 *       平铺不显重复；胆石按块数 1–3 引用手写模型 {@code guzhenren:block/guts_stone}、{@code _2}、{@code _3}。</li>
 * </ul>
 */

public class ModBlockStateProvider extends BlockStateProvider {

    private static final int SOULQUAKE_STONE_VARIANTS = 3;

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
        registerFarmlandBlock(ModBlocks.BLACK_SOIL_FARMLAND.get());
        simpleBlock(ModBlocks.STEEL_BLOCK.get());
        registerSoulquakeStone(ModBlocks.SOULQUAKE_STONE.get());
        registerGutsStone(ModBlocks.GUTS_STONE.get());
    }

    //region Guts stone [胆石]
    private void registerGutsStone(Block block) {
        getVariantBuilder(block).forAllStates(state -> {
            int stones = state.getValue(GutsStones.STONES);
            String model = stones == 1 ? "block/guts_stone" : "block/guts_stone_" + stones;
            return ConfiguredModel.builder().modelFile(models().getExistingFile(modLoc(model))).build();
        });
    }
    //endregion

    //region Soulquake stone [荡魂山石]
    private void registerSoulquakeStone(Block block) {
        String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
        ConfiguredModel.Builder<?> variants = ConfiguredModel.builder();
        for (int variant = 0; variant < SOULQUAKE_STONE_VARIANTS; variant++) {
            String model = variant == 0 ? name : name + "_" + variant;
            if (variant > 0) variants = variants.nextModel();
            variants.modelFile(models().cubeAll(model, modLoc("block/" + model)).renderType("translucent"));
        }
        getVariantBuilder(block).partialState().setModels(variants.build());
    }
    //endregion

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

    //region the farmland [耕地]
    private void registerFarmlandBlock(Block block) {
        String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
        ModelFile dry = buildFarmlandModel(name, name);
        ModelFile moist = buildFarmlandModel(name + "_moist", name);
        getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(FarmBlock.MOISTURE) == FarmBlock.MAX_MOISTURE ? moist : dry)
                .build());
    }

    private ModelFile buildFarmlandModel(String model, String block) {
        return models().withExistingParent(model, mcLoc("block/template_farmland"))
                .texture("dirt", modLoc("block/" + block + "_side"))
                .texture("top", modLoc("block/" + model));
    }
    //endregion
}
