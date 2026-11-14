package net.alex.guzhenren.datagen.block;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.registry.block.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
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
 *   <li>双层植物按 {@code half} 每半一个模型，父模型 {@link #TINTED_CROSS_OVERLAY}：交叉面画两遍，{@code #cross} 灰度
 *       PNG 带色调索引 0（群系草色，{@code ClientEvents} 注册），{@code #overlay} PNG 共面叠上不着色、保留原色；后画的
 *       共面四边形赢深度测试，同草方块侧面覆盖层。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public class ModBlockStateProvider extends BlockStateProvider {

    private static final String TINTED_CROSS_OVERLAY = "tinted_cross_overlay";
    private static final int GRASS_TINT = 0;
    private static final int NO_TINT = -1;

    public ModBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Guzhenren.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        registerFluidBlock(ModBlocks.SPIRIT_SPRING.get(), Guzhenren.id("block/spirit_spring_still"));
        registerFluidBlock(ModBlocks.PURE_WATER.get(), Guzhenren.id("block/pure_water_still"));
        registerFlowerBlock(ModBlocks.NINE_LEAF_VITALITY_GRASS_3.get(),
                Guzhenren.id("item/nine_leaf_vitality_grass_3_full"));
        registerDoublePlantBlock(ModBlocks.INTIMATE_GRASS.get(), buildTintedCrossOverlay());
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
    private void registerDoublePlantBlock(Block block, ModelFile parent) {
        ModelFile bottom = buildHalfModel(block, "_bottom", parent);
        ModelFile top = buildHalfModel(block, "_top", parent);
        getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER ? top : bottom)
                .build());
    }

    private ModelFile buildHalfModel(Block block, String suffix, ModelFile parent) {
        String name = BuiltInRegistries.BLOCK.getKey(block).getPath() + suffix;
        return models().getBuilder(name).parent(parent)
                .texture("cross", modLoc("block/" + name))
                .texture("overlay", modLoc("block/" + name + "_overlay"))
                .renderType("cutout");
    }

    private ModelFile buildTintedCrossOverlay() {
        BlockModelBuilder model = models().getBuilder(TINTED_CROSS_OVERLAY).ao(false).texture("particle", "#cross");
        addCrossPlanes(model, "#cross", GRASS_TINT);
        addCrossPlanes(model, "#overlay", NO_TINT);
        return model;
    }

    private static void addCrossPlanes(BlockModelBuilder model, String texture, int tint) {
        addCrossPlane(model, texture, tint, new float[] { 0.8F, 0.0F, 8.0F }, new float[] { 15.2F, 16.0F, 8.0F },
                Direction.NORTH, Direction.SOUTH);
        addCrossPlane(model, texture, tint, new float[] { 8.0F, 0.0F, 0.8F }, new float[] { 8.0F, 16.0F, 15.2F },
                Direction.WEST, Direction.EAST);
    }

    private static void addCrossPlane(BlockModelBuilder model, String texture, int tint, float[] from, float[] to,
                                      Direction front, Direction back) {
        var element = model.element()
                .from(from[0], from[1], from[2]).to(to[0], to[1], to[2])
                .shade(false)
                .rotation().origin(8.0F, 8.0F, 8.0F).axis(Direction.Axis.Y).angle(45.0F).rescale(true).end();
        for (Direction face : new Direction[] { front, back }) {
            element.face(face).uvs(0.0F, 0.0F, 16.0F, 16.0F).texture(texture).tintindex(tint).end();
        }
    }
    //endregion
}
