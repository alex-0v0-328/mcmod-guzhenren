package net.alex.guzhenren.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.blissful.BlissfulLand;
import net.alex.guzhenren.gameplay.blissful.BlissfulLandData;
import net.alex.guzhenren.gameplay.blissful.storage.BlissfulStorage;
import net.alex.guzhenren.item.gu.stack.RefinedGuState;
import net.alex.guzhenren.item.gu.tending.GuHunger;
import net.alex.guzhenren.item.gu.tending.GuUpkeep;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.alex.guzhenren.registry.item.ModDataComponents;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.GameTestPlayers.messages;

/**
 * 持续照顾型蛊[Tended Gu]的饱食[Hunger]日钟，及各饱食条与生命池背后的转数公式。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GuHungerGameTests {
    private static final String HUNGRY_KEY = "guzhenren.item.gu.hungry";
    private static final String STARVED_KEY = "guzhenren.item.gu.starved";

    private GuHungerGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void tendedGuHungerClock(GameTestHelper helper) {
        List<Component> inbox = new ArrayList<>();
        ServerPlayer player = GameTestPlayers.survival(helper, "gzr-gametest-mock").inbox(inbox).unconnected()
                .build();
        ItemStack gu = new ItemStack(ModItems.WHITE_BOAR_GU_1.get());
        gu.set(ModDataComponents.REFINED_GU_STATE, new RefinedGuState(true, 0, 0, 3, 0));
        player.getInventory().setItem(0, gu);

        GuUpkeep.tickCarried(player, 0L);
        helper.assertValueEqual(TendedGuItem.getState(gu).hunger(), 3, "hunger without a day boundary");
        helper.assertTrue(messages(inbox, HUNGRY_KEY).isEmpty(), "hungry broadcast without a day boundary");

        GuUpkeep.tickCarried(player, 1L);
        helper.assertValueEqual(TendedGuItem.getState(gu).hunger(), 2, "hunger after one day");
        helper.assertTrue(messages(inbox, HUNGRY_KEY).isEmpty(), "hungry broadcast while above the threshold");

        GuUpkeep.tickCarried(player, 1L);
        helper.assertValueEqual(TendedGuItem.getState(gu).hunger(), 1, "hunger at the hungry threshold");
        helper.assertValueEqual(messages(inbox, HUNGRY_KEY).size(), 1, "hungry broadcast count");

        GuUpkeep.tickCarried(player, 0L);
        helper.assertValueEqual(messages(inbox, HUNGRY_KEY).size(), 1,
                "hungry broadcast latched to once per day");

        GuUpkeep.tickCarried(player, 1L);
        helper.assertTrue(player.getInventory().getItem(0).isEmpty(), "starved Gu was removed from the inventory");
        helper.assertValueEqual(TendedGuItem.getState(gu).hunger(), 0, "hunger after starving");
        helper.assertValueEqual(messages(inbox, STARVED_KEY).size(), 1, "starved broadcast count");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void hungryGuEatFromTheLandStorageAfterTheBag(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survival(helper, "gzr-gametest-larder-mock").unconnected().build();
        ItemStack gu = new ItemStack(ModItems.WHITE_BOAR_GU_1.get());
        gu.set(ModDataComponents.REFINED_GU_STATE, new RefinedGuState(true, 0, 0, 3, 0));
        player.getInventory().setItem(0, gu);
        TendedGuItem item = (TendedGuItem) gu.getItem();
        GuHunger.HungerBar bar = (GuHunger.HungerBar) item.getGuProperties().buildHunger();
        Item food = BuiltInRegistries.ITEM.stream()
                .filter(each -> item.getGuProperties().feedUnits(new ItemStack(each)) > 0).findFirst().orElseThrow();
        BlissfulStorage storage = BlissfulLandData.create(BlissfulLand.FOX_IMMORTAL).storage();
        storage.insert(new ItemStack(food, 200));

        bar.feed(item, player, gu);
        helper.assertValueEqual(TendedGuItem.getState(gu).hunger(), 3,
                "outside the owner's land the storage is not touched");
        bar.feedFrom(item, player, gu, storage);
        helper.assertValueEqual(TendedGuItem.getState(gu).hunger(), bar.max(), "storage food fills the bar");
        long left = storage.count(stack -> stack.is(food));
        helper.assertTrue(left < 200 && left > 0, "only what the gu ate leaves storage, the rest goes back");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void rankedGuFollowTheHungerAndHealthFormula(GameTestHelper helper) {
        List<Supplier<Item>> ranked = List.of(ModItems.FLOWER_BOAR_GU_1, ModItems.BRUTE_FORCE_LONGHORN_BEETLE_GU_1,
                ModItems.HORIZONTAL_CRASH_GU_3, ModItems.VERTICAL_CRASH_GU_3, ModItems.CHARGING_CRASH_GU_4,
                ModItems.CHARGING_CRASH_GU_5, ModItems.SELF_RELIANCE_GU_2, ModItems.SELF_RELIANCE_GU_3,
                ModItems.SELF_RELIANCE_GU_4, ModItems.HARDSHIP_STRENGTH_GU_4, ModItems.ALL_OUT_EFFORT_GU_3,
                ModItems.ALL_OUT_EFFORT_GU_4, ModItems.ALL_OUT_EFFORT_GU_5, ModItems.HEAVENLY_MUGWORT_GU_3,
                ModItems.JADE_SKIN_GU_1, ModItems.WHITE_JADE_GU_2, ModItems.AMPLIFY_EFFECT_GU_2,
                ModItems.AMPLIFY_EFFECT_GU_3, ModItems.AMPLIFY_EFFECT_GU_4, ModItems.AMPLIFY_EFFECT_GU_5,
                ModItems.MOONLIGHT_GU_1, ModItems.MOONGLOW_GU_2, ModItems.BLOOD_MOON_GU_3, ModItems.LITTLE_LIGHT_GU_1);
        int[] bars = {0, 14, 26, 50, 98, 194};
        int[] healths = {0, 8, 12, 16, 20, 24};
        for (Supplier<Item> entry : ranked) {
            TendedGuItem gu = (TendedGuItem) entry.get();
            int rank = gu.getGuProperties().getRank().ordinal();
            GuHunger.HungerBar bar = (GuHunger.HungerBar) gu.getGuProperties().buildHunger();
            helper.assertValueEqual(bar.max(), bars[rank], gu + " bar");
            helper.assertValueEqual(bar.perUse(), 3, gu + " use");
            helper.assertValueEqual(bar.perDay(), 1, gu + " day");
            helper.assertValueEqual(bar.unitsPerHunger(), 1, gu + " food rate");
            helper.assertValueEqual(gu.getGuProperties().getFeedUnits(), 1, gu + " food");
            helper.assertValueEqual(gu.getMaxHealth(), healths[rank], gu + " health");
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void scaledHungerBarDrainsItsDailyShare(GameTestHelper helper) {
        List<Component> inbox = new ArrayList<>();
        ServerPlayer player = GameTestPlayers.survival(helper, "gzr-gametest-mock").inbox(inbox).unconnected()
                .build();
        ItemStack gu = new ItemStack(ModItems.SELF_RELIANCE_GU_4.get());
        gu.set(ModDataComponents.REFINED_GU_STATE, new RefinedGuState(true, 0, 0, 3, 0));
        player.getInventory().setItem(0, gu);

        helper.assertValueEqual(((TendedGuItem) gu.getItem()).getMaxHealth(), 20,
                "scaled Gu health");
        GuUpkeep.tickCarried(player, 1L);
        helper.assertValueEqual(TendedGuItem.getState(gu).hunger(), 2, "hunger after one day");
        helper.assertTrue(messages(inbox, HUNGRY_KEY).isEmpty(), "hungry broadcast above one day's share");

        GuUpkeep.tickCarried(player, 1L);
        helper.assertValueEqual(TendedGuItem.getState(gu).hunger(), 1, "hunger after two days");
        helper.assertValueEqual(messages(inbox, HUNGRY_KEY).size(), 1,
                "hungry once the next rollover kills");

        GuUpkeep.tickCarried(player, 1L);
        helper.assertTrue(player.getInventory().getItem(0).isEmpty(), "starved Gu was removed from the inventory");
        helper.succeed();
    }
}
