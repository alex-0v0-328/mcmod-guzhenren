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
 * 本模组物品模型数据生成器，按 {@link ItemRegistrar} 注册链声明的 {@link ItemModel} 策略分发生成。
 * <ul>
 *   <li>元泉[Spirit Spring]方块物品直接使用流体静止条带贴图（{@link ItemModel#FLUID}），钢块直接继承同名方块模型
 *       （{@link ItemModel#BLOCK}），常规物品生成单层平面模型（{@link ItemModel#FLAT}）。</li>
 *   <li>九叶生机草[Nine Leaf Vitality Grass]（{@link ItemModel#LEAF_STAGES}）默认使用 {@code _full} 贴图，并按采摘属性
 *       {@link NineLeafVitalityGrassItem#PICKED} 覆盖切换为 {@code _half} 与 {@code _empty}；进度数字图标
 *       （{@link ItemModel#NUMERALS}，{@link #registerNumeralItem}）以 {@code _0} 为底，按转数与阶段升序写入
 *       {@code custom_model_data} 覆盖项（原版按最后一个达到阈值的覆盖项生效）。</li>
 *   <li>⚠ {@link #registerModels} 会校验 {@link ModItems#ITEMS} 中的每个注册项是否均经过 {@link ItemRegistrar} 登记，且
 *       模型构造器通过 {@link ExistingFileHelper} 强制校验贴图文件存在，确保漏登记或缺失 PNG 在 {@code runData} 阶段直
 *       接报错。</li>
 * </ul>
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
