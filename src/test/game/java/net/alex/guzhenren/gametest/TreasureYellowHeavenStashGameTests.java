package net.alex.guzhenren.gametest;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.gameplay.dimension.TossStashData;
import net.alex.guzhenren.gameplay.dimension.TreasureYellowHeavenGuard;
import net.alex.guzhenren.lifecycle.PlayerDataService;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 宝黄天[Treasure Yellow Heaven]丢弃暂存回归测试。放不回背包的丢弃不留物品实体，暂存于 {@link TossStashData}，
 * 出宝黄天后掉在脚下。GameTest 世界无真实宝黄天：直接调 {@link TreasureYellowHeavenGuard#stowOrStash}，传入
 * {@code CommonHooks.onPlayerTossEvent} 会交给事件的实体，即捕获掉落时丢出、从未加入维度的实体；GameTest
 * 维度在宝黄天外，在此调 {@link TreasureYellowHeavenGuard#tick} 等同经任意途径离开后到达。
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TreasureYellowHeavenStashGameTests {

    private TreasureYellowHeavenStashGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void fullBagStashesTheCursorStackWithoutAnEntity(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survival(helper, "gzr-tyh-stash").onGround();
        Inventory bag = fillBag(player);

        TreasureYellowHeavenGuard.stowOrStash(player, captureToss(player, new ItemStack(Items.EMERALD, 5)));
        TreasureYellowHeavenGuard.stowOrStash(player, captureToss(player, new ItemStack(Items.DIAMOND, 2)));
        TreasureYellowHeavenGuard.stowOrStash(player, captureToss(player, new ItemStack(Items.EMERALD, 4)));

        helper.assertValueEqual(bag.countItem(Items.EMERALD), 64, "the fitting part was not stowed");
        List<ItemStack> stash = PlayerData.get(player).getTossStash().stacks();
        helper.assertValueEqual(stash.size(), 2, "stashed stacks");
        helper.assertTrue(stash.get(0).is(Items.EMERALD), "the first stashed stack");
        helper.assertValueEqual(stash.get(0).getCount(), 7, "the emeralds were not merged into one stack");
        helper.assertTrue(stash.get(1).is(Items.DIAMOND), "the second stashed stack");
        helper.assertValueEqual(stash.get(1).getCount(), 2, "the stashed diamonds");
        helper.assertValueEqual(nearby(helper, player, Items.EMERALD) + nearby(helper, player, Items.DIAMOND), 0,
                "a stashed toss left an item entity");

        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void leavingDropsTheStashAtTheFeet(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survival(helper, "gzr-tyh-stash").onGround();
        fillBag(player);
        PlayerData.get(player).setTossStash(TossStashData.DEFAULT
                .with(new ItemStack(Items.EMERALD, 7)).with(new ItemStack(Items.DIAMOND, 2)));

        TreasureYellowHeavenGuard.tick(player);
        TreasureYellowHeavenGuard.tick(player);

        helper.assertTrue(PlayerData.get(player).getTossStash().isEmpty(), "the stash was not emptied");
        helper.assertValueEqual(nearby(helper, player, Items.EMERALD), 7, "emeralds dropped at the feet");
        helper.assertValueEqual(nearby(helper, player, Items.DIAMOND), 2, "diamonds dropped at the feet");
        for (ItemEntity drop : drops(helper, player)) {
            helper.assertTrue(!drop.isNoGravity(), "a carried-out stack hovers");
            drop.discard();
        }

        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void deathCloneCarriesTheStashOut(GameTestHelper helper) {
        ServerPlayer from = GameTestPlayers.survival(helper, "gzr-tyh-stash").build();
        PlayerData.get(from).setTossStash(TossStashData.DEFAULT.with(new ItemStack(Items.EMERALD, 3)));
        ServerPlayer to = GameTestPlayers.survival(helper, "gzr-tyh-stash").onGround();

        to.copyAttachmentsFrom(from, true);
        PlayerDataService.onClone(from, to, true, false);

        List<ItemStack> stash = PlayerData.get(to).getTossStash().stacks();
        helper.assertValueEqual(stash.size(), 1, "a wiping death lost the stash");
        helper.assertValueEqual(stash.get(0).getCount(), 3, "the stash the clone carried");

        TreasureYellowHeavenGuard.tick(to);

        helper.assertValueEqual(nearby(helper, to, Items.EMERALD), 3, "the respawned player got no drop");
        drops(helper, to).forEach(ItemEntity::discard);
        from.discard();
        to.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void stashSurvivesSaveAndLoad(GameTestHelper helper) {
        ServerPlayer saved = GameTestPlayers.survival(helper, "gzr-tyh-stash").build();
        ItemStack named = new ItemStack(Items.DIAMOND, 2);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("stash"));
        PlayerData.get(saved).setTossStash(TossStashData.DEFAULT.with(named));

        CompoundTag tag = saved.saveWithoutId(new CompoundTag());
        ServerPlayer loaded = GameTestPlayers.survival(helper, "gzr-tyh-stash").build();
        loaded.load(tag);

        List<ItemStack> stash = PlayerData.get(loaded).getTossStash().stacks();
        helper.assertValueEqual(stash.size(), 1, "the stash was not saved");
        helper.assertTrue(ItemStack.matches(stash.get(0), named), "the saved stack changed");
        saved.discard();
        loaded.discard();
        helper.succeed();
    }

    private static Inventory fillBag(ServerPlayer player) {
        Inventory bag = player.getInventory();
        bag.setItem(0, new ItemStack(Items.EMERALD, 62));
        for (int slot = 1; slot < Inventory.INVENTORY_SIZE; slot++) bag.setItem(slot, new ItemStack(Items.STONE, 64));
        return bag;
    }

    private static ItemEntity captureToss(ServerPlayer player, ItemStack stack) {
        player.captureDrops(new ArrayList<>());
        ItemEntity tossed = player.drop(stack, false, false);
        player.captureDrops(null);
        return tossed;
    }

    private static List<ItemEntity> drops(GameTestHelper helper, ServerPlayer player) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(1.0D));
    }

    private static int nearby(GameTestHelper helper, ServerPlayer player, Item item) {
        return drops(helper, player).stream().filter(drop -> drop.getItem().is(item))
                .mapToInt(drop -> drop.getItem().getCount()).sum();
    }
}
