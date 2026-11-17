package net.alex.guzhenren.registry;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.block.SpiritSpringBlock;
import net.alex.guzhenren.item.AdvancementIconItem;
import net.alex.guzhenren.item.BookletItem;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.killingmove.KillingMoveBookItem;
import net.alex.guzhenren.item.material.GuMaterialItem;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * ModItems 注册的物品类别（测试运行时解析）：蛊 {@code MortalGuItem}、蛊材 {@code GuMaterialItem}、流体物品
 * （元泉[Spirit Spring]方块物品、纯水桶）、进度图标、杀招[Killing Move]书、书册、钢锭、钢块、知心草种子、黑土耕地；
 * 不钉总数：内容常增删，{@code WikiItemsGameTests} 已对到文档分组。
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RegistrySnapshotGameTests {

    private RegistrySnapshotGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void itemClassesArePinned(GameTestHelper helper) {
        for (Holder<Item> holder : ModItems.ITEMS.getEntries()) {
            Item item = holder.value();
            assertTrue(item instanceof MortalGuItem || item instanceof GuMaterialItem
                            || item instanceof BlockItem blockItem
                            && blockItem.getBlock() instanceof SpiritSpringBlock
                            || item instanceof BucketItem || item instanceof AdvancementIconItem
                            || item instanceof KillingMoveBookItem || item instanceof BookletItem
                            || item == ModItems.STEEL_INGOT.get() || item == ModItems.STEEL_BLOCK.get()
                            || item == ModItems.INTIMATE_GRASS_SEEDS.get()
                            || item == ModItems.BLACK_SOIL_FARMLAND.get(),
                    () -> "unexpected item class: " + item);
        }
        helper.succeed();
    }
}
