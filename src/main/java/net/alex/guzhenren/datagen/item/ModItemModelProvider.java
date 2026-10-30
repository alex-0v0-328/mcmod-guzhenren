package net.alex.guzhenren.datagen.item;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.aperture.Stage;
import net.alex.guzhenren.item.AdvancementIconItem;
import net.alex.guzhenren.item.gu.mortal.wood.LeafGrowth;
import net.alex.guzhenren.item.gu.mortal.wood.NineLeafVitalityGrassItem;
import net.alex.guzhenren.registry.builder.ItemModel;
import net.alex.guzhenren.registry.builder.ItemRegistrar;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Writes an item model per registered item, in the {@link ItemModel} shape it asks for.
 *
 * <p>Extends {@link net.neoforged.neoforge.client.model.generators.ItemModelProvider}. Every item names its shape on
 * its {@link ItemRegistrar} registration chain: the Spirit Spring [元泉] BlockItem the fluid's still strip instead
 * of an item PNG of its own, the Steel Block [钢块] its block model, the Nine Leaf Vitality Grass [九叶生机草] its
 * {@code _full} PNG overriding to {@code _half} and {@code _empty} on the {@code picked} property, the Advancement
 * Icon [进度图标] its {@code _0} PNG overriding on vanilla's {@code custom_model_data} property, one model per
 * settable rank and stage, and the rest a flat {@code basicItem}.
 *
 * <p>⚠ An item registered on {@code ModItems.ITEMS} past the chain would ship with no names, model or tags, so it
 * fails datagen here, naming the id.
 *
 * <p>⚠ The texture existence check means a missing PNG fails datagen instead of shipping
 * as a missing-texture item nobody notices until they open the tab.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public class ModItemModelProvider extends ItemModelProvider {

    private static final ModelFile GENERATED = new ModelFile.UncheckedModelFile("minecraft:item/generated");
    private static final ResourceLocation CUSTOM_MODEL_DATA =
            ResourceLocation.withDefaultNamespace("custom_model_data");

    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Guzhenren.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        ItemRegistrar registrar = ItemRegistrar.of(ModItems.ITEMS);
        for (var entry : ModItems.ITEMS.getEntries()) {
            if (registrar.getEntry(entry.getId()) == null) {
                throw new IllegalStateException("Item " + entry.getId() + " registers outside ItemRegistrar");
            }
        }
        for (ItemRegistrar.Entry entry : registrar.getEntries()) {
            registerModel(entry.item().get(), entry.item().getId().getPath(), entry.model());
        }
    }

    private ItemModelBuilder registerModel(Item item, String name, ItemModel model) {
        return switch (model) {
            case FLAT -> basicItem(item);
            case BLOCK -> getBuilder(name).parent(new ModelFile.UncheckedModelFile(modLoc("block/" + name)));
            case FLUID -> getBuilder(name).parent(GENERATED).texture("layer0", modLoc("block/" + name + "_still"));
            case LEAF_STAGES -> registerLeafStagedItem(name);
            case NUMERALS -> registerNumeralItem(name);
        };
    }

    private ItemModelBuilder registerLeafStagedItem(String name) {
        ItemModelBuilder half = buildFlatItem(name + "_half");
        ItemModelBuilder empty = buildFlatItem(name + "_empty");
        return getBuilder(name)
                .parent(GENERATED)
                .texture("layer0", modLoc("item/" + name + "_full"))
                .override().predicate(NineLeafVitalityGrassItem.PICKED, LeafGrowth.PICKED_HALF).model(half).end()
                .override().predicate(NineLeafVitalityGrassItem.PICKED, LeafGrowth.PICKED_BARE).model(empty).end();
    }

    private ItemModelBuilder registerNumeralItem(String name) {
        ItemModelBuilder model = getBuilder(name).parent(GENERATED).texture("layer0", modLoc("item/" + name + "_0"));
        // Ascending order: the last override whose threshold the value reaches wins.
        for (Rank rank : Rank.getSettable()) {
            for (Stage stage : Stage.values()) {
                model.override().predicate(CUSTOM_MODEL_DATA, AdvancementIconItem.getModelData(rank, stage))
                        .model(buildFlatItem(name + "_" + AdvancementIconItem.getSuffix(rank, stage))).end();
            }
        }
        return model;
    }

    private ItemModelBuilder buildFlatItem(String name) {
        return getBuilder(name).parent(GENERATED).texture("layer0", modLoc("item/" + name));
    }
}
