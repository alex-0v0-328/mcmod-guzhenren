package net.alex.guzhenren.datagen.item;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.aperture.Stage;
import net.alex.guzhenren.item.AdvancementIconItem;
import net.alex.guzhenren.item.gu.mortal.path.wood.LeafGrowth;
import net.alex.guzhenren.item.gu.mortal.path.wood.NineLeafVitalityGrassItem;
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
 * 物品模型，形状取 {@link ItemRegistrar} 注册链声明的 {@link ItemModel}。
 *
 * <ul>
 *   <li>元泉[Spirit Spring] BlockItem 用流体静止条带而非自身 PNG；钢块用方块模型；其余为平面 {@code basicItem}。</li>
 *   <li>九叶生机草[Nine Leaf Vitality Grass]用 {@code _full} PNG，按 {@code picked} 覆盖为 {@code _half}、
 *       {@code _empty}；进度图标 {@code _0} PNG，按 {@code custom_model_data} 覆盖，每个可设转与阶段一个模型。</li>
 *   <li>⚠ 绕过注册链注册到 {@code ModItems.ITEMS} 的物品缺名字、模型、标签，故在此让数据生成失败并报 id。</li>
 *   <li>⚠ 查贴图存在：缺 PNG 让数据生成失败，而非发布成缺贴图物品、打开物品栏页才发现。</li>
 * </ul>
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
