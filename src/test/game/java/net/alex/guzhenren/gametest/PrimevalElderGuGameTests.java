package net.alex.guzhenren.gametest;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.aperture.Stage;
import net.alex.guzhenren.item.gu.mortal.path.space.PrimevalElderGuItem;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.alex.guzhenren.network.ClientIntents;
import net.alex.guzhenren.network.payload.PrimevalElderAction;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.GameTestGu.elder;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertFalse;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 元老蛊[Primeval Elder Gu]两种意图测试。点按免费存入身上全部元石[Primeval Stone]，无蓄力故无蓄力条；滑块按组（每组
 * 64）取出，只进背包，受空位限制，不掉落，不动改名元石堆；创造模式点按不看元石直接填满；旁观者与副手够不到；野生的仍长按
 * 蓄力炼化。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PrimevalElderGuGameTests {
    private static final int BAG_SLOT = 9;
    private static final int BAG_SLOTS = 36;
    private static final int REFINE_TICKS = 1_000;

    private PrimevalElderGuGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void aTapDepositsEveryStoneWithNoCharge(GameTestHelper helper) {
        ServerPlayer player = holder(helper, "gzr-elder-tap");
        ItemStack elder = elder(0);
        player.setItemInHand(InteractionHand.MAIN_HAND, elder);
        player.getInventory().setItem(BAG_SLOT, stones(100));

        ClientIntents.primevalElder(player, PrimevalElderAction.DEPOSIT, 0);
        assertEquals(100L, PrimevalElderGuItem.getStored(elder), "a tap deposits every carried stone");
        assertEquals(0, player.getInventory().countItem(ModItems.PRIMEVAL_STONE.get()));

        assertEquals(0, elder.getUseDuration(player), "a refined one has no charge, so no charge bar");
        player.getCooldowns().removeCooldown(elder.getItem());
        player.getInventory().setItem(BAG_SLOT, stones(30));
        elder.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        assertFalse(player.isUsingItem(), "a vanilla click never starts a charge");
        assertEquals(130L, PrimevalElderGuItem.getStored(elder), "a vanilla click lands the deposit at once");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void creativeTapFillsTheStoreWithOrWithoutStones(GameTestHelper helper) {
        ServerPlayer player = GameTestGu.awakened(
                GameTestPlayers.survival(helper, "gzr-elder-creative-fill").creative(() -> true).build(), Rank.THREE);
        player.getAbilities().instabuild = true;
        ItemStack elder = elder(0);
        player.setItemInHand(InteractionHand.MAIN_HAND, elder);

        ClientIntents.primevalElder(player, PrimevalElderAction.DEPOSIT, 0);
        assertEquals(1_000L, PrimevalElderGuItem.getStored(elder), "an empty-handed creative tap fills the store");

        player.getCooldowns().removeCooldown(elder.getItem());
        ((PrimevalElderGuItem) elder.getItem()).drawStones(elder, 900);
        player.getInventory().setItem(BAG_SLOT, stones(30));
        ClientIntents.primevalElder(player, PrimevalElderAction.DEPOSIT, 0);
        assertEquals(1_000L, PrimevalElderGuItem.getStored(elder), "carried stones change nothing, it still fills");
        assertEquals(30, player.getInventory().countItem(ModItems.PRIMEVAL_STONE.get()), "and takes none of them");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aPickWithdrawsThatManyStacksIntoTheBag(GameTestHelper helper) {
        ServerPlayer player = holder(helper, "gzr-elder-pick");
        ItemStack elder = elder(500);
        player.setItemInHand(InteractionHand.MAIN_HAND, elder);

        assertEquals(8, PrimevalElderGuItem.getWithdrawableStacks(player, elder), "500 stones are 8 stacks");
        ClientIntents.primevalElder(player, PrimevalElderAction.WITHDRAW, 3);
        assertEquals(308L, PrimevalElderGuItem.getStored(elder), "three stacks of 64 came out");
        assertEquals(192, player.getInventory().countItem(ModItems.PRIMEVAL_STONE.get()));
        assertTrue(player.getOffhandItem().isEmpty(), "nothing goes to the offhand");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aPickStopsAtTheBagRoomAndDropsNothing(GameTestHelper helper) {
        ServerPlayer player = holder(helper, "gzr-elder-room");
        ItemStack elder = elder(500);
        player.setItemInHand(InteractionHand.MAIN_HAND, elder);
        for (int slot = 1; slot < BAG_SLOTS - 2; slot++) {
            player.getInventory().setItem(slot, new ItemStack(Items.DIRT));
        }
        player.getInventory().setItem(BAG_SLOT, stones(40));

        assertEquals(152, PrimevalElderGuItem.getBagRoom(player), "two empty slots and 24 on the partial stack");
        assertEquals(3, PrimevalElderGuItem.getWithdrawableStacks(player, elder), "152 stones round up to 3");
        ClientIntents.primevalElder(player, PrimevalElderAction.WITHDRAW, 10);
        assertEquals(348L, PrimevalElderGuItem.getStored(elder), "only what fits came out");
        assertEquals(192, player.getInventory().countItem(ModItems.PRIMEVAL_STONE.get()), "all of it in the bag");
        assertEquals(64, player.getInventory().getItem(BAG_SLOT).getCount(), "the partial stack filled first");
        assertEquals(0, PrimevalElderGuItem.getBagRoom(player), "the bag is now full");

        player.getCooldowns().removeCooldown(elder.getItem());
        ClientIntents.primevalElder(player, PrimevalElderAction.WITHDRAW, 1);
        assertEquals(348L, PrimevalElderGuItem.getStored(elder), "a full bag takes nothing");
        assertEquals(0, PrimevalElderGuItem.getWithdrawableStacks(player, elder));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aRenamedStoneStackIsLeftAlone(GameTestHelper helper) {
        ServerPlayer player = holder(helper, "gzr-elder-renamed");
        ItemStack elder = elder(100);
        player.setItemInHand(InteractionHand.MAIN_HAND, elder);
        ItemStack renamed = stones(10);
        renamed.set(DataComponents.CUSTOM_NAME, Component.literal("keepsake"));
        player.getInventory().setItem(BAG_SLOT, renamed);

        ClientIntents.primevalElder(player, PrimevalElderAction.WITHDRAW, 1);
        assertEquals(10, player.getInventory().getItem(BAG_SLOT).getCount(), "the renamed stack took none");
        assertEquals(74, player.getInventory().countItem(ModItems.PRIMEVAL_STONE.get()), "a fresh stack of 64");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aSpectatorReachesNothing(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survival(helper, "gzr-elder-spectator").spectator(() -> true).build();
        ItemStack elder = elder(100);
        player.setItemInHand(InteractionHand.MAIN_HAND, elder);

        ClientIntents.primevalElder(player, PrimevalElderAction.WITHDRAW, 1);
        assertEquals(100L, PrimevalElderGuItem.getStored(elder), "a spectator withdraws nothing");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theOffhandStaysSilent(GameTestHelper helper) {
        ServerPlayer player = holder(helper, "gzr-elder-offhand");
        ItemStack elder = elder(500);
        player.setItemInHand(InteractionHand.OFF_HAND, elder);
        player.getInventory().setItem(BAG_SLOT, stones(100));

        InteractionResult result = elder.use(helper.getLevel(), player, InteractionHand.OFF_HAND).getResult();
        assertEquals(InteractionResult.PASS, result, "an offhand click passes");
        ClientIntents.primevalElder(player, PrimevalElderAction.DEPOSIT, 0);
        ClientIntents.primevalElder(player, PrimevalElderAction.WITHDRAW, 1);
        assertEquals(500L, PrimevalElderGuItem.getStored(elder), "neither intent reaches an offhand one");
        assertEquals(100, player.getInventory().countItem(ModItems.PRIMEVAL_STONE.get()));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aWildOneIsStillRefinedByAHeldCharge(GameTestHelper helper) {
        ServerPlayer player = holder(helper, "gzr-elder-wild");
        ItemStack elder = new ItemStack(ModItems.PRIMEVAL_ELDER_GU_1.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, elder);

        elder.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        assertTrue(player.isUsingItem(), "a wild one charges its refining");
        assertTrue(elder.getUseDuration(player) > 0, "with its refining bar");
        int start = player.getUseItemRemainingTicks();
        for (int remaining = start; remaining > start - REFINE_TICKS && player.isUsingItem(); remaining--) {
            elder.onUseTick(helper.getLevel(), player, remaining);
        }
        assertTrue(((TendedGuItem) elder.getItem()).isRefined(elder), "holding the click refines it");
        assertFalse(player.isUsingItem(), "the refining charge ends once refined");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void creativeRefinesOnAPoolBelowTheFloor(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survival(helper, "gzr-elder-creative").creative(() -> true).build();
        player.getAbilities().instabuild = true;
        ApertureService.awaken(player, 44);
        ApertureEssenceService.set(player, ApertureData.PRIMARY, 19L);
        player.getInventory().setItem(BAG_SLOT, stones(64));
        ItemStack elder = new ItemStack(ModItems.PRIMEVAL_ELDER_GU_1.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, elder);

        elder.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        int start = player.getUseItemRemainingTicks();
        for (int remaining = start; remaining > start - REFINE_TICKS && player.isUsingItem(); remaining--) {
            elder.onUseTick(helper.getLevel(), player, remaining);
        }
        assertTrue(((TendedGuItem) elder.getItem()).isRefined(elder), "creative draws no stone, yet still refines");
        assertEquals(64, player.getInventory().countItem(ModItems.PRIMEVAL_STONE.get()), "and burns none");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void creativeRefinesAtThePaceOfTheStonesInReach(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survival(helper, "gzr-elder-creative-pace").creative(() -> true).build();
        player.getAbilities().instabuild = true;
        ApertureService.awaken(player, 80);
        ApertureService.setRank(player, Rank.THREE);
        ApertureService.setStage(player, Stage.PEAK);
        ApertureEssenceService.set(player, ApertureData.PRIMARY, 0L);
        for (int slot = BAG_SLOT; slot < BAG_SLOT + 4; slot++) player.getInventory().setItem(slot, stones(64));
        ItemStack elder = new ItemStack(ModItems.PRIMEVAL_ELDER_GU_3.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, elder);

        elder.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        int start = player.getUseItemRemainingTicks();
        for (int remaining = start; remaining > start - 40 && player.isUsingItem(); remaining--) {
            elder.onUseTick(helper.getLevel(), player, remaining);
        }
        assertTrue(((TendedGuItem) elder.getItem()).isRefined(elder),
                "an empty rank III pool sized 256 stones' worth a cycle, 51,200 inside two");
        assertEquals(256, player.getInventory().countItem(ModItems.PRIMEVAL_STONE.get()), "and burns none");
        helper.succeed();
    }

    private static ServerPlayer holder(GameTestHelper helper, String name) {
        return GameTestGu.awakened(GameTestPlayers.survival(helper, name).build(), Rank.THREE);
    }

    private static ItemStack stones(int count) { return new ItemStack(ModItems.PRIMEVAL_STONE.get(), count); }
}
