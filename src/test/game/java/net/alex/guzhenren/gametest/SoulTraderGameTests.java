package net.alex.guzhenren.gametest;

import java.util.Objects;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.trade.SoulTradeMenu;
import net.alex.guzhenren.gameplay.trade.SoulTraderEntity;
import net.alex.guzhenren.registry.entity.ModEntityTypes;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 宝黄天[Treasure Yellow Heaven]外的魂魄交易 NPC 测试。
 * <ul>
 *   <li>GameTest 服务端无宝黄天维度；覆盖原地悬停、无敌、出现与消散计时、交易菜单、菜单按钮触发的结算。</li>
 *   <li>NPC 有意浮在地面上方 2 格，脚下无地也须定住，同宝黄天虚空；模拟玩家无重力放在旁边。</li>
 *   <li>交易直接调 {@code clickMenuButton}，即原版菜单按钮包的入口；模拟玩家读不到动作栏，改读背包。</li>
 * </ul>
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SoulTraderGameTests {

    private static final BlockPos TRADER_POS = new BlockPos(4, 2, 4);
    private static final Vec3 NEAR = new Vec3(4.5D, 2.0D, 2.0D);

    private SoulTraderGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void traderHoversInPlaceAndNeverDespawns(GameTestHelper helper) {
        SoulTraderEntity trader = spawnTrader(helper);
        Vec3 start = trader.position();
        trader.setDeltaMovement(new Vec3(0.5D, 0.5D, 0.5D));
        helper.runAtTickTime(40L, () -> {
            helper.assertTrue(trader.isAlive(), "trader vanished");
            helper.assertTrue(trader.position().distanceToSqr(start) < 1.0E-6,
                    "trader moved from " + start + " to " + trader.position());
            helper.assertTrue(trader.requiresCustomPersistence() && !trader.removeWhenFarAway(1.0E4D),
                    "trader may despawn");
            helper.assertTrue(!trader.isPushable() && !trader.canBeLeashed(),
                    "trader can be pushed or leashed");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void traderShrugsOffEverythingButKillThenFades(GameTestHelper helper) {
        SoulTraderEntity trader = spawnTrader(helper);
        ServerPlayer player = GameTestPlayers.survival(helper, "gzr-soul-attacker").pastSpawnInvulnerability().build();
        DamageSources sources = helper.getLevel().damageSources();
        helper.assertTrue(!trader.hurt(sources.playerAttack(player), 100.0F), "a player attack landed");
        helper.assertTrue(!trader.hurt(sources.generic(), 100.0F), "generic damage landed");
        helper.assertTrue(!trader.hurt(sources.inFire(), 100.0F), "fire landed");
        helper.assertTrue(!trader.hurt(sources.explosion(null, null), 100.0F), "an explosion landed");
        helper.assertTrue(trader.getHealth() == trader.getMaxHealth(), "trader lost health");

        trader.kill();
        helper.assertTrue(trader.isDeadOrDying(), "kill did not land");
        long killedAt = helper.getTick();
        helper.runAtTickTime(killedAt + SoulTraderEntity.DISAPPEAR_TICKS - 2,
                () -> helper.assertTrue(!trader.isRemoved(), "trader was removed before its disappear ended"));
        helper.runAtTickTime(killedAt + SoulTraderEntity.DISAPPEAR_TICKS + 1, () -> {
            helper.assertTrue(trader.isRemoved(), "dead trader was never removed");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void rightClickOpensTheTradeMenu(GameTestHelper helper) {
        SoulTraderEntity trader = spawnTrader(helper);
        ServerPlayer player = playerNear(helper, "gzr-soul-browser");
        helper.assertTrue(trader.interact(player, InteractionHand.MAIN_HAND).consumesAction(),
                "right click was not consumed");
        helper.assertTrue(player.containerMenu instanceof SoulTradeMenu, "no trade menu opened");
        SoulTradeMenu menu = (SoulTradeMenu) player.containerMenu;
        helper.assertValueEqual(menu.getOffers().size(), 2, "offer count");
        helper.assertValueEqual(menu.slots.size(), 36,
                "the menu should hold exactly the player's bag");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void clickingAnOfferPaysDeliversAndNods(GameTestHelper helper) {
        SoulTraderEntity trader = spawnTrader(helper);
        ServerPlayer player = playerNear(helper, "gzr-soul-buyer");
        Inventory bag = player.getInventory();
        bag.add(new ItemStack(ModItems.PRIMEVAL_STONE.get(), 10));
        for (int i = 0; i < 3; i++) bag.add(new ItemStack(Items.DIRT, 64));
        bag.add(new ItemStack(Items.DIRT, 8));
        SoulTradeMenu menu = open(helper, trader, player);

        helper.assertTrue(menu.clickMenuButton(player, 0), "pork offer click was not handled");
        helper.assertValueEqual(bag.countItem(Items.PORKCHOP), 64, "porkchops delivered");
        helper.assertValueEqual(bag.countItem(ModItems.PRIMEVAL_STONE.get()), 0,
                "primeval stones paid");
        helper.assertTrue(trader.isGesturing(), "a completed trade did not start the nod");

        helper.assertTrue(menu.clickMenuButton(player, 1), "Gu offer click was not handled");
        helper.assertValueEqual(bag.countItem(ModItems.WHITE_BOAR_GU_1.get()), 1,
                "White Boar Gu delivered");
        helper.assertValueEqual(bag.countItem(Items.DIRT), 8, "three stacks of dirt paid");

        helper.assertTrue(!menu.clickMenuButton(player, 2), "an offer index past the list was handled");
        helper.assertTrue(!menu.clickMenuButton(player, -1), "a negative offer index was handled");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void refusedTradesLeaveTheBagUntouched(GameTestHelper helper) {
        SoulTraderEntity trader = spawnTrader(helper);
        ServerPlayer player = playerNear(helper, "gzr-soul-short");
        Inventory bag = player.getInventory();
        bag.add(new ItemStack(ModItems.PRIMEVAL_STONE.get(), 9));
        SoulTradeMenu menu = open(helper, trader, player);

        helper.assertTrue(menu.clickMenuButton(player, 0), "short click was not handled");
        helper.assertValueEqual(bag.countItem(ModItems.PRIMEVAL_STONE.get()), 9,
                "a short trade took stones");
        helper.assertValueEqual(bag.countItem(Items.PORKCHOP), 0, "a short trade delivered");
        helper.assertTrue(trader.isGesturing(), "a refused trade did not start the head shake");

        bag.clearContent();
        bag.setItem(0, new ItemStack(ModItems.PRIMEVAL_STONE.get(), 10));
        for (int slot = 1; slot < Inventory.INVENTORY_SIZE; slot++) bag.setItem(slot, new ItemStack(Items.STONE, 64));
        menu.setCarried(new ItemStack(Items.DIAMOND));
        helper.assertTrue(menu.clickMenuButton(player, 0), "no-room click was not handled");
        helper.assertValueEqual(bag.countItem(ModItems.PRIMEVAL_STONE.get()), 10,
                "a trade that leaves no room for the cursor stack took stones");
        helper.assertValueEqual(bag.countItem(Items.PORKCHOP), 0, "a trade without room delivered");

        menu.setCarried(ItemStack.EMPTY);
        helper.assertTrue(menu.clickMenuButton(player, 0), "trade click was not handled");
        helper.assertValueEqual(bag.countItem(Items.PORKCHOP), 64,
                "the freed stone slot did not take the pork");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void menuClosesOutOfRangeAndAfterDeath(GameTestHelper helper) {
        SoulTraderEntity trader = spawnTrader(helper);
        ServerPlayer player = playerNear(helper, "gzr-soul-leaver");
        SoulTradeMenu menu = open(helper, trader, player);
        helper.assertTrue(menu.stillValid(player), "menu invalid next to the trader");

        GameTestPlayers.place(player, helper, NEAR.add(0.0D, 0.0D, -SoulTraderEntity.TRADE_RANGE));
        helper.assertTrue(!menu.stillValid(player), "menu stayed valid out of range");
        GameTestPlayers.place(player, helper, NEAR);
        helper.assertTrue(menu.stillValid(player), "menu invalid back in range");

        trader.kill();
        helper.assertTrue(!menu.stillValid(player), "menu stayed valid after the trader died");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void traderAppearsThenBeckonsAWaitingPlayer(GameTestHelper helper) {
        SoulTraderEntity trader = spawnTrader(helper);
        playerNear(helper, "gzr-soul-waiter");
        helper.assertTrue(trader.isAppearing(), "a new trader did not start appearing");
        helper.runAtTickTime(SoulTraderEntity.APPEAR_TICKS - 5L, () -> {
            helper.assertTrue(trader.isAppearing(), "the appear ended early");
            helper.assertTrue(!trader.isGesturing(), "the trader gestured while appearing");
        });
        helper.runAtTickTime(SoulTraderEntity.APPEAR_TICKS + 1L,
                () -> helper.assertTrue(!trader.isAppearing(), "the appear never ended"));
        helper.runAtTickTime(SoulTraderEntity.APPEAR_TICKS + 12L, () -> {
            helper.assertTrue(trader.isGesturing(), "the waiting player was never beckoned");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void savedTraderDoesNotAppearAgain(GameTestHelper helper) {
        SoulTraderEntity trader = spawnTrader(helper);
        CompoundTag saved = trader.saveWithoutId(new CompoundTag());
        helper.assertTrue(saved.getBoolean("Appeared"), "save did not record the appear");

        SoulTraderEntity reloaded = created(helper);
        reloaded.load(saved);
        helper.assertTrue(!reloaded.isAppearing(), "a reloaded trader appears again");

        CompoundTag summoned = saved.copy();
        summoned.remove("Appeared");
        SoulTraderEntity fresh = created(helper);
        fresh.load(summoned);
        helper.assertTrue(fresh.isAppearing(), "a trader summoned with NBT skips its appear");
        helper.succeed();
    }

    private static SoulTraderEntity spawnTrader(GameTestHelper helper) {
        return helper.spawn(ModEntityTypes.TEST_TRADE_GU_IMMORTAL.get(), TRADER_POS);
    }

    private static SoulTraderEntity created(GameTestHelper helper) {
        return Objects.requireNonNull(ModEntityTypes.TEST_TRADE_GU_IMMORTAL.get().create(helper.getLevel()),
                "trader type did not create an entity");
    }

    private static ServerPlayer playerNear(GameTestHelper helper, String name) {
        return GameTestPlayers.survival(helper, name).at(NEAR);
    }

    private static SoulTradeMenu open(GameTestHelper helper, SoulTraderEntity trader, ServerPlayer player) {
        trader.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.containerMenu instanceof SoulTradeMenu, "no trade menu opened");
        return (SoulTradeMenu) player.containerMenu;
    }
}
