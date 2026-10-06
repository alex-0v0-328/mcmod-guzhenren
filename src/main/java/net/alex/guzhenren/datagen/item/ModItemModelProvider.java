package net.alex.guzhenren.datagen.item;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.block.SpiritSpringBlock;
import net.alex.guzhenren.item.gu.mortal.wood.LeafGrowth;
import net.alex.guzhenren.item.gu.mortal.wood.NineLeafVitalityGrassItem;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Writes an item model per registered item, dispatching on the item's class.
 *
 * <p>Extends {@link net.neoforged.neoforge.client.model.generators.ItemModelProvider}. Iterates every
 * registered item and calls {@code basicItem} on it, except the Spirit Spring [元泉] BlockItem,
 * whose icon is the fluid's still strip instead of an item PNG of its own, and the Nine Leaf Vitality
 * Grass [九叶生机草], whose model shows its {@code _full} PNG and overrides to {@code _half} and
 * {@code _empty} on the {@code picked} property.
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

    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Guzhenren.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        for (var entry : ModItems.ITEMS.getEntries()) {
            Item item = entry.get();
            if (item instanceof BlockItem blockItem && blockItem.getBlock() instanceof SpiritSpringBlock) {
                getBuilder(entry.getId().getPath())
                        .parent(GENERATED)
                        .texture("layer0", Guzhenren.id("block/spirit_spring_still"));
                continue;
            }
            if (item instanceof NineLeafVitalityGrassItem) {
                leafStagedItem(entry.getId().getPath());
                continue;
            }
            basicItem(item);
        }
    }

    private void leafStagedItem(String name) {
        ItemModelBuilder half = flatItem(name + "_half");
        ItemModelBuilder empty = flatItem(name + "_empty");
        getBuilder(name)
                .parent(GENERATED)
                .texture("layer0", modLoc("item/" + name + "_full"))
                .override().predicate(NineLeafVitalityGrassItem.PICKED, LeafGrowth.PICKED_HALF).model(half).end()
                .override().predicate(NineLeafVitalityGrassItem.PICKED, LeafGrowth.PICKED_BARE).model(empty).end();
    }

    private ItemModelBuilder flatItem(String name) {
        return getBuilder(name).parent(GENERATED).texture("layer0", modLoc("item/" + name));
    }
}
