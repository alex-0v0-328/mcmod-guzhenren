package net.alex.guzhenren.gametest;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static net.alex.guzhenren.gametest.GameTestPlayers.CENTER_POS;

/**
 * 锄头专属草药掉落：原版兰花用锄破坏经全局战利品修改器另掉一份月兰花瓣，否则只掉花。破坏均为真实生存
 * {@code gameMode.destroyBlock}，工具像玩家挥动般进入战利品上下文；{@code Level.destroyBlock} 掉落不带工具。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HoeHarvestGameTests {

    private HoeHarvestGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void blueOrchidGivesPetalsToAHoeOnTopOfTheFlower(GameTestHelper helper) {
        helper.setBlock(CENTER_POS.below(), Blocks.DIRT);
        helper.setBlock(CENTER_POS, Blocks.BLUE_ORCHID);
        breakWith(helper, new ItemStack(Items.STONE_HOE), CENTER_POS);

        helper.assertValueEqual(count(helper, CENTER_POS, Items.BLUE_ORCHID), 1, "the flower still drops");
        helper.assertValueEqual(count(helper, CENTER_POS, ModItems.MOON_ORCHID_PETALS.get()), 1,
                "one petal");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void blueOrchidGivesNoPetalsWithoutAHoe(GameTestHelper helper) {
        helper.setBlock(CENTER_POS.below(), Blocks.DIRT);
        helper.setBlock(CENTER_POS, Blocks.BLUE_ORCHID);
        breakWith(helper, new ItemStack(Items.SHEARS), CENTER_POS);

        helper.assertValueEqual(count(helper, CENTER_POS, Items.BLUE_ORCHID), 1, "the flower drops");
        helper.assertValueEqual(count(helper, CENTER_POS, ModItems.MOON_ORCHID_PETALS.get()), 0,
                "no petal without a hoe");
        helper.succeed();
    }

    private static void breakWith(GameTestHelper helper, ItemStack tool, BlockPos pos) {
        ServerPlayer player = GameTestPlayers.survival(helper, "gzr-hoe-mock").build();
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        helper.assertTrue(player.gameMode.destroyBlock(helper.absolutePos(pos)), "the survival break lands");
    }

    private static int count(GameTestHelper helper, BlockPos pos, Item item) {
        int total = 0;
        for (ItemEntity drop : helper.getEntities(EntityType.ITEM, pos, 1.0)) {
            if (drop.getItem().is(item)) total += drop.getItem().getCount();
        }
        return total;
    }
}
