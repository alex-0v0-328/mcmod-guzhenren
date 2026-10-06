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
 * Writes the blockstates and models for the mod's blocks. A fluid block only needs the particle
 * model -- the liquid surface itself is drawn by the fluid renderer from the FluidType textures
 * (see {@link net.alex.guzhenren.client.fluid.SpiritSpringClientExtensions}) -- so each entry
 * matches vanilla's {@code block/water} shape: a blockstate variant pointing at a model whose
 * single texture is the still strip.
 *
 * <p>A flower is vanilla's cutout {@code block/cross}. The Nine Leaf Vitality Grass [九叶生机草] crosses its
 * item's {@code _full} PNG -- the blocks atlas stitches {@code item/} too, so one file serves both.
 *
 * <p>A double plant is one model per half, picked by the {@code half} property, each on
 * {@link #TINTED_CROSS_OVERLAY}: vanilla's cross planes twice, the {@code #cross} gray PNG with tint index 0 (the
 * biome grass color, registered in {@code ClientEvents}) and the {@code #overlay} PNG coplanar on top without a
 * tint, so the parts drawn in color keep it. The later coplanar quad wins the depth test, the way the grass block's
 * side overlay does.
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
        fluidBlock(ModBlocks.SPIRIT_SPRING.get(), Guzhenren.id("block/spirit_spring_still"));
        flowerBlock(ModBlocks.NINE_LEAF_VITALITY_GRASS.get(), Guzhenren.id("item/nine_leaf_vitality_grass_full"));
        doublePlantBlock(ModBlocks.INTIMATE_GRASS.get(), tintedCrossOverlay());
    }

    private void fluidBlock(Block block, ResourceLocation particle) {
        simpleBlock(block, models().getBuilder(BuiltInRegistries.BLOCK.getKey(block).getPath())
                .texture("particle", particle));
    }

    private void flowerBlock(Block block, ResourceLocation cross) {
        simpleBlock(block, models().cross(BuiltInRegistries.BLOCK.getKey(block).getPath(), cross)
                .renderType("cutout"));
    }

    //region the double plant -- a tinted gray cross under an untinted overlay, per half
    private void doublePlantBlock(Block block, ModelFile parent) {
        ModelFile bottom = halfModel(block, "_bottom", parent);
        ModelFile top = halfModel(block, "_top", parent);
        getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER ? top : bottom)
                .build());
    }

    private ModelFile halfModel(Block block, String suffix, ModelFile parent) {
        String name = BuiltInRegistries.BLOCK.getKey(block).getPath() + suffix;
        return models().getBuilder(name).parent(parent)
                .texture("cross", modLoc("block/" + name))
                .texture("overlay", modLoc("block/" + name + "_overlay"))
                .renderType("cutout");
    }

    private ModelFile tintedCrossOverlay() {
        BlockModelBuilder model = models().getBuilder(TINTED_CROSS_OVERLAY).ao(false).texture("particle", "#cross");
        crossPlanes(model, "#cross", GRASS_TINT);
        crossPlanes(model, "#overlay", NO_TINT);
        return model;
    }

    private static void crossPlanes(BlockModelBuilder model, String texture, int tint) {
        crossPlane(model, texture, tint, new float[] { 0.8F, 0.0F, 8.0F }, new float[] { 15.2F, 16.0F, 8.0F },
                Direction.NORTH, Direction.SOUTH);
        crossPlane(model, texture, tint, new float[] { 8.0F, 0.0F, 0.8F }, new float[] { 8.0F, 16.0F, 15.2F },
                Direction.WEST, Direction.EAST);
    }

    private static void crossPlane(BlockModelBuilder model, String texture, int tint, float[] from, float[] to,
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
