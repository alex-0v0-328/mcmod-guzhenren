package net.alex.guzhenren.gametest;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.TossedItems;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.gameplay.dimension.DimensionReturnData;
import net.alex.guzhenren.gameplay.dimension.DimensionTravelService;
import net.alex.guzhenren.gameplay.dimension.TreasureYellowHeavenGuard;
import net.alex.guzhenren.gameplay.flight.FlightAbility;
import net.alex.guzhenren.gameplay.trade.SoulTraderEntity;
import net.alex.guzhenren.lifecycle.PlayerDataService;
import net.alex.guzhenren.registry.entity.ModEntityTypes;
import net.alex.guzhenren.registry.world.ModDimensions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static net.alex.guzhenren.gametest.GameTestPlayers.CENTER_POS;

/**
 * 宝黄天[Treasure Yellow Heaven]锚定维度测试。
 * <ul>
 *   <li>⚠ {@code net.minecraft.gametest.framework.GameTestServer} 第 97～109 行用 {@code minecraft:flat} 预设，
 *       只建主世界、下界、末地；数据包维度只登记进注册表，从不实例化为 {@code ServerLevel}，故需真实宝黄天的进入、
 *       退出、虚空救援、守卫测试跑不了 {@code runGameTestServer}；本类只留可行的：数据包登记、服务分支、记录生命周期、
 *       守卫处理器不拦宝黄天外玩家的健全性检查。</li>
 *   <li>维度类型与生物群系来自兄弟模组 Gu World，本源集看不到；{@link #dimensionDatapackIsRegistered}
 *       用维度键位置构造其键，与该模组 LevelStem 守同一契约，也证明两模组名称一致。</li>
 *   <li>{@link #exitWithRecordRevokesFlight} 覆盖存在维度里的返回点：收回飞行授权，生存玩家不能复飞；回归检查
 *       {@link #exitFallbackRevokesFlight}：无返回记录的退出不留飞行授权；{@link #tickOutsideTyhRevokesFlight}：
 *       经其他途径离开（原版 {@code /tp}、其他模组）下一 tick 收回。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TreasureYellowHeavenGameTests {


    private TreasureYellowHeavenGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void dimensionDatapackIsRegistered(GameTestHelper helper) {
        var registryAccess = helper.getLevel().registryAccess();
        var dimensionTypes = registryAccess.registryOrThrow(Registries.DIMENSION_TYPE);
        var biomes = registryAccess.registryOrThrow(Registries.BIOME);
        var typeKey = ResourceKey.create(Registries.DIMENSION_TYPE, ModDimensions.TREASURE_YELLOW_HEAVEN.location());
        var biomeKey = ResourceKey.create(Registries.BIOME, ModDimensions.TREASURE_YELLOW_HEAVEN.location());

        helper.assertTrue(dimensionTypes.containsKey(typeKey),
                "dimension type missing");
        helper.assertTrue(biomes.containsKey(biomeKey),
                "biome missing");

        var type = dimensionTypes.get(typeKey);
        helper.assertTrue(type != null, "dimension type value missing");
        helper.assertValueEqual(type.minY(), 0, "TYH min_y");
        helper.assertValueEqual(type.height(), 256, "TYH height");
        helper.assertValueEqual(type.logicalHeight(), 256, "TYH logical_height");
        helper.assertTrue(type.hasSkyLight(), "TYH must have sky light");
        helper.assertTrue(type.natural(), "TYH must be natural, so a bed sets the spawn");
        helper.assertTrue(type.bedWorks() && type.respawnAnchorWorks(), "TYH beds and respawn anchors must work");

        var biome = biomes.get(biomeKey);
        helper.assertTrue(biome != null, "biome value missing");
        helper.assertTrue(!biome.hasPrecipitation(), "TYH biome must not have precipitation");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void isInsideFalseForOverworldPlayer(GameTestHelper helper) {
        ServerPlayer player = survivalMock(helper);
        helper.assertTrue(!DimensionTravelService.isInside(player, ModDimensions.TREASURE_YELLOW_HEAVEN),
                "overworld player reported as inside TYH");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void enterReturnsFalseWhenTargetLevelMissing(GameTestHelper helper) {
        ServerPlayer player = survivalMock(helper);
        player.moveTo(helper.absoluteVec(new Vec3(4.5D, 2.5D, 4.5D)));

        ResourceKey<Level> missing = ResourceKey.create(Registries.DIMENSION, Guzhenren.id("gametest_missing"));
        boolean entered = DimensionTravelService.enter(player, new ModDimensions.AnchoredDimension(missing,
                ModDimensions.TREASURE_YELLOW_HEAVEN_SPAWN, ModDimensions.Arrival.FLIGHT, 8, 1.0));

        helper.assertTrue(!entered, "enter must return false when the target ServerLevel is absent");
        helper.assertTrue(!PlayerData.get(player).getDimensionReturn().isPresent(),
                "return record must stay empty when enter fails");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void exitReturnsFalseAndClearsEmptyRecord(GameTestHelper helper) {
        ServerPlayer player = survivalMock(helper);
        player.moveTo(helper.absoluteVec(new Vec3(4.5D, 2.5D, 4.5D)));
        helper.assertTrue(!PlayerData.get(player).getDimensionReturn().isPresent(),
                "fresh player already had a return record");

        boolean used = DimensionTravelService.exit(player);

        helper.assertTrue(!used, "exit must report fallback when no record exists");
        helper.assertTrue(!PlayerData.get(player).getDimensionReturn().isPresent(),
                "empty record must stay empty");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    @SuppressWarnings("deprecation")
    public static void ensureFlightGrantsCreativeFlight(GameTestHelper helper) {
        ServerPlayer player = survivalMock(helper);
        player.getAbilities().mayfly = false;
        player.getAbilities().flying = false;

        DimensionTravelService.ensureFlight(player);

        helper.assertTrue(FlightAbility.canFly(player), "ensureFlight did not grant flight");
        helper.assertTrue(!player.getAbilities().mayfly,
                "ensureFlight must not write the raw mayfly field");
        helper.assertTrue(!player.getAbilities().flying, "ensureFlight must not force flying");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void exitWithRecordRevokesFlight(GameTestHelper helper) {
        ServerPlayer player = survivalMock(helper);
        Vec3 back = helper.absoluteVec(new Vec3(4.5D, 2.5D, 4.5D));
        PlayerData.get(player).setDimensionReturn(DimensionReturnData.of(
                helper.getLevel().dimension(), back.x, back.y, back.z, 0.0F, 0.0F, true));
        DimensionTravelService.ensureFlight(player);
        player.getAbilities().flying = true;

        boolean used = DimensionTravelService.exit(player);

        helper.assertTrue(used, "exit must use a return point whose level exists");
        helper.assertValueEqual(player.getX(), back.x, "return x");
        helper.assertTrue(!FlightAbility.canFly(player), "record exit left flight granted");
        helper.assertTrue(!player.getAbilities().flying, "a survival player resumed flying without a grant");
        helper.assertTrue(!PlayerData.get(player).getDimensionReturn().isPresent(), "record not cleared");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void exitFallbackRevokesFlight(GameTestHelper helper) {
        ServerPlayer player = survivalMock(helper);
        player.moveTo(helper.absoluteVec(new Vec3(4.5D, 2.5D, 4.5D)));
        DimensionTravelService.ensureFlight(player);
        player.getAbilities().flying = true;
        helper.assertTrue(FlightAbility.canFly(player), "ensureFlight did not grant flight");

        boolean used = DimensionTravelService.exit(player);

        helper.assertTrue(!used, "exit must report fallback when no record exists");
        helper.assertTrue(!FlightAbility.canFly(player), "fallback exit left flight granted");
        helper.assertTrue(!player.getAbilities().flying, "fallback exit left the player flying");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void tickOutsideTyhRevokesFlight(GameTestHelper helper) {
        ServerPlayer player = survivalMock(helper);
        DimensionTravelService.ensureFlight(player);
        player.getAbilities().flying = true;

        TreasureYellowHeavenGuard.tick(player);

        helper.assertTrue(!FlightAbility.canFly(player), "a player outside TYH kept the flight grant");
        helper.assertTrue(!player.getAbilities().flying, "a player outside TYH was left flying");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void rescueIfBelowVoidTeleportsToSpawn(GameTestHelper helper) {
        ServerPlayer player = survivalMock(helper);
        Vec3 spawn = helper.absoluteVec(new Vec3(4.5D, 2.5D, 4.5D));
        player.moveTo(spawn.x, spawn.y, spawn.z, 0.0F, 0.0F);
        player.moveTo(spawn.x, helper.getLevel().getMinBuildHeight() - 5.0, spawn.z);
        helper.assertTrue(player.getY() < helper.getLevel().getMinBuildHeight(), "player not below void");

        boolean rescued = DimensionTravelService.rescueIfBelowVoid(player, spawn);

        helper.assertTrue(rescued, "rescue must return true");
        helper.assertValueEqual(player.getX(), spawn.x(), "rescue x");
        helper.assertValueEqual(player.getY(), spawn.y(), "rescue y");
        helper.assertValueEqual(player.getZ(), spawn.z(), "rescue z");
        helper.assertValueEqual(player.fallDistance, 0.0F, "rescue did not reset fall distance");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void clearRemovesReturnRecord(GameTestHelper helper) {
        ServerPlayer player = survivalMock(helper);
        PlayerData.get(player).setDimensionReturn(DimensionReturnData.of(
                helper.getLevel().dimension(), 1.0, 2.0, 3.0, 0.0F, 0.0F, false));

        DimensionTravelService.clear(player);

        helper.assertTrue(!PlayerData.get(player).getDimensionReturn().isPresent(),
                "clear did not remove record");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void resetAllClearsReturnRecord(GameTestHelper helper) {
        ServerPlayer player = survivalMock(helper);
        PlayerData.get(player).setDimensionReturn(DimensionReturnData.of(
                helper.getLevel().dimension(), 1.0, 2.0, 3.0, 0.0F, 0.0F, false));

        PlayerDataService.resetAll(player);

        helper.assertTrue(!PlayerData.get(player).getDimensionReturn().isPresent(),
                "resetAll did not clear record");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void deathCloneClearsReturnRecord(GameTestHelper helper) {
        ServerPlayer from = survivalMock(helper);
        PlayerData.get(from).setDimensionReturn(DimensionReturnData.of(
                helper.getLevel().dimension(), 1.0, 2.0, 3.0, 0.0F, 0.0F, false));
        ServerPlayer to = survivalMock(helper);

        // The inherited player data is copied before PlayerDataEvents.onClone runs.
        to.copyAttachmentsFrom(from, true);
        PlayerDataService.onClone(from, to, true, true);

        helper.assertTrue(!PlayerData.get(to).getDimensionReturn().isPresent(),
                "death clone did not clear record");
        from.discard();
        to.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void guardHandlersIgnoreOverworldPlayer(GameTestHelper helper) {
        ServerPlayer player = survivalMock(helper);
        player.moveTo(helper.absoluteVec(new Vec3(4.5D, 2.5D, 4.5D)));
        var pig = helper.spawn(EntityType.PIG, CENTER_POS);

        helper.assertTrue(!TreasureYellowHeavenGuard.isBreakRefused(player), "break canceled outside TYH");
        helper.assertTrue(!TreasureYellowHeavenGuard.isPlaceRefused(player), "place canceled outside TYH");
        helper.assertTrue(!TreasureYellowHeavenGuard.isUseRefused(player, null),
                "right-click block or item canceled outside TYH");
        helper.assertTrue(!TreasureYellowHeavenGuard.isUseRefused(player, pig), "entity interact canceled outside TYH");
        helper.assertTrue(!TreasureYellowHeavenGuard.isAttackRefused(player), "attack canceled outside TYH");

        player.getInventory().clearContent();
        ItemStack stack = new ItemStack(Items.DIAMOND, 3);
        ItemEntity tossed = new ItemEntity(helper.getLevel(), player.getX(), player.getY(), player.getZ(), stack);
        helper.assertTrue(!TreasureYellowHeavenGuard.tryStowToss(player, tossed), "item toss canceled outside TYH");
        helper.assertValueEqual(player.getInventory().countItem(Items.DIAMOND), 0,
                "item stack restored outside TYH");

        pig.discard();
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void exitStaysAdminOnlyOutsideTyh(GameTestHelper helper) {
        ServerPlayer player = survivalMock(helper);
        var dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();

        helper.assertTrue(!parses(dispatcher, player, 0, "guworld exit"), "a player outside TYH can exit");
        helper.assertTrue(!parses(dispatcher, player, 0, "guworld exit @s"), "a player can exit others");
        helper.assertTrue(!parses(dispatcher, player, 0, "guworld enter guworld:treasure_yellow_heaven"),
                "a player can enter");
        helper.assertTrue(parses(dispatcher, player, 2, "guworld exit"), "an admin cannot exit");
        helper.assertTrue(parses(dispatcher, player, 2, "guworld exit @s"), "an admin cannot exit others");

        player.discard();
        helper.succeed();
    }

    private static boolean parses(CommandDispatcher<CommandSourceStack> dispatcher, ServerPlayer player,
                                  int permission, String command) {
        ParseResults<CommandSourceStack> parsed =
                dispatcher.parse(command, player.createCommandSourceStack().withPermission(permission));
        return !parsed.getReader().canRead() && parsed.getExceptions().isEmpty()
                && parsed.getContext().getCommand() != null;
    }

    @GameTest(template = "empty9x9x9")
    public static void guardLetsPlayersRightClickSoulTradersOnly(GameTestHelper helper) {
        SoulTraderEntity trader = helper.spawn(ModEntityTypes.TEST_TRADE_GU_IMMORTAL.get(), CENTER_POS.above());
        var pig = helper.spawn(EntityType.PIG, CENTER_POS);
        helper.assertTrue(TreasureYellowHeavenGuard.isSoulTrader(trader), "a soul trader is not exempt");
        helper.assertTrue(!TreasureYellowHeavenGuard.isSoulTrader(pig), "a pig is exempt");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void cancelledTossStowsWhatFitsAndFloatsTheRest(GameTestHelper helper) {
        ServerPlayer player = survivalMock(helper);
        player.moveTo(helper.absoluteVec(new Vec3(4.5D, 2.5D, 4.5D)));
        Inventory bag = player.getInventory();
        bag.setItem(0, new ItemStack(Items.DIRT, 62));
        for (int slot = 1; slot < Inventory.INVENTORY_SIZE; slot++) bag.setItem(slot, new ItemStack(Items.STONE, 64));

        ItemEntity overflow = new ItemEntity(helper.getLevel(), player.getX(), player.getY(), player.getZ(),
                new ItemStack(Items.DIRT, 5));
        overflow.setDeltaMovement(new Vec3(0.2D, 0.2D, 0.2D));
        helper.assertTrue(!TossedItems.stow(player, overflow),
                "a toss that does not fit was reported as stowed");
        helper.assertValueEqual(bag.countItem(Items.DIRT), 64, "the fitting part was not stowed");
        helper.assertValueEqual(overflow.getItem().getCount(), 3, "the floating remainder");
        helper.assertTrue(overflow.isNoGravity(), "the remainder falls into the void");
        helper.assertTrue(overflow.getDeltaMovement().lengthSqr() == 0.0D, "the remainder keeps drifting");

        bag.setItem(1, ItemStack.EMPTY);
        ItemEntity fits = new ItemEntity(helper.getLevel(), player.getX(), player.getY(), player.getZ(),
                new ItemStack(Items.DIAMOND, 5));
        helper.assertTrue(TossedItems.stow(player, fits), "a fitting toss was not stowed");
        helper.assertValueEqual(bag.countItem(Items.DIAMOND), 5, "the stowed diamonds");

        player.discard();
        helper.succeed();
    }

    private static ServerPlayer survivalMock(GameTestHelper helper) {
        return GameTestPlayers.survival(helper, "gzr-tyh-gametest").build();
    }
}
