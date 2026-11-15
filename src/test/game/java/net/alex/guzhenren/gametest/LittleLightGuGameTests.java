package net.alex.guzhenren.gametest;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.item.gu.mortal.path.light.LittleLightGuItem;
import net.alex.guzhenren.registry.item.ModDataComponents;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;
import static net.alex.guzhenren.gametest.GameTestGu.charge;
import static net.alex.guzhenren.gametest.GameTestGu.hold;
import static net.alex.guzhenren.gametest.GameTestGu.hunger;
import static net.alex.guzhenren.gametest.GameTestPlayers.STAND;
import static net.alex.guzhenren.gametest.GameTestPlayers.exhausted;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertFalse;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 小光蛊[Little Light Gu]的运行时覆盖。
 * <ul>
 *   <li>开亮为一轮，按属性付一次催动饱食[Hunger]、不扣真元[Essence]；再点提前关、不退；开启后
 *       {@link LittleLightGuItem#BRIGHT_TICKS} 自行结束，持否皆然，期间不扣。</li>
 *   <li>差一轮且背包无萤石粉则警告、不亮，有粉则扣量前后各吃一次；强行催动[Forced Use]的一轮照常亮满，熄灭（到期或点
 *       击）时蛊死；创造模式饥饿也能开，不耗食物。</li>
 *   <li>点击为真实 {@code use} + {@code finishUsingItem}；测试体在一 tick 内跑完，故截止时间相对当前改写，并直调
 *       {@code inventoryTick}。</li>
 * </ul>
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LittleLightGuGameTests {
    private static final int ROUND_HUNGER = 3;

    private LittleLightGuGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void clickPaysOneRoundAndTheSecondTurnsOff(GameTestHelper helper) {
        ServerPlayer player = holder(helper);
        ItemStack gu = hold(player, ModItems.LITTLE_LIGHT_GU_1.get(), 14);
        long essence = ApertureEssenceService.getSpendable(player);
        long now = helper.getLevel().getGameTime();

        charge(helper, player, gu);
        assertTrue(LittleLightGuItem.isBright(gu), "the first click turns it bright");
        assertEquals(14 - ROUND_HUNGER, hunger(gu), "the round pays its use hunger");
        assertEquals((Object) (now + LittleLightGuItem.BRIGHT_TICKS),
                gu.get(ModDataComponents.LITTLE_LIGHT_BRIGHT_UNTIL), "the round lasts two minutes");
        charge(helper, player, gu);
        assertFalse(LittleLightGuItem.isBright(gu), "the second click turns it off");
        assertFalse(gu.has(ModDataComponents.LITTLE_LIGHT_BRIGHT), "off removes the component");
        assertFalse(gu.has(ModDataComponents.LITTLE_LIGHT_BRIGHT_UNTIL), "off removes the deadline");
        assertEquals(14 - ROUND_HUNGER, hunger(gu), "turning off is free and refunds nothing");
        assertEquals(essence, ApertureEssenceService.getSpendable(player), "the switch bills no essence");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void shortWithoutFoodIsWarnedAndStaysDark(GameTestHelper helper) {
        ServerPlayer player = holder(helper);
        ItemStack gu = hold(player, ModItems.LITTLE_LIGHT_GU_1.get(), 1);

        charge(helper, player, gu);

        assertFalse(LittleLightGuItem.isBright(gu), "a warned click turned it bright");
        assertEquals(1, hunger(gu), "a warned click paid the round");
        assertFalse(player.getCooldowns().isOnCooldown(gu.getItem()), "a warned click put a cooldown");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aForcedRoundShinesInFullThenTheGuDiesWhenItGoesOut(GameTestHelper helper) {
        List<Component> inbox = new ArrayList<>();
        ServerPlayer player = holder(helper, inbox);
        ItemStack gu = hold(player, ModItems.LITTLE_LIGHT_GU_1.get(), 1);
        long now = helper.getLevel().getGameTime();
        charge(helper, player, gu);
        charge(helper, player, gu);

        assertTrue(LittleLightGuItem.isBright(gu), "the confirmed click stayed dark");
        assertTrue(gu.has(ModDataComponents.LITTLE_LIGHT_FORCED), "the forced round is unmarked");
        assertEquals((Object) (now + LittleLightGuItem.BRIGHT_TICKS),
                gu.get(ModDataComponents.LITTLE_LIGHT_BRIGHT_UNTIL), "a forced round is cut short");
        tick(helper, player, gu, 1L);
        assertFalse(gu.isEmpty() || exhausted(inbox), "the Gu died while it still shone");

        tick(helper, player, gu, 0L);

        assertTrue(gu.isEmpty(), "the Gu outlived its forced round");
        assertTrue(exhausted(inbox), "no exhaustion message");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void turningAForcedRoundOffEarlyKillsTheGu(GameTestHelper helper) {
        List<Component> inbox = new ArrayList<>();
        ServerPlayer player = holder(helper, inbox);
        ItemStack gu = hold(player, ModItems.LITTLE_LIGHT_GU_1.get(), 1);
        charge(helper, player, gu);
        charge(helper, player, gu);

        charge(helper, player, gu);

        assertTrue(gu.isEmpty(), "the Gu outlived its forced round");
        assertTrue(exhausted(inbox), "no exhaustion message");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void hungryWithFoodEatsAroundTheRound(GameTestHelper helper) {
        ServerPlayer player = holder(helper);
        ItemStack gu = hold(player, ModItems.LITTLE_LIGHT_GU_1.get(), 1);
        ItemStack dust = new ItemStack(Items.GLOWSTONE_DUST, 20);
        player.getInventory().setItem(10, dust);

        charge(helper, player, gu);

        assertTrue(LittleLightGuItem.isBright(gu));
        assertEquals(14, hunger(gu), "it ate before and after the drop");
        assertEquals(20 - 13 - ROUND_HUNGER, dust.getCount(), "13 to fill it, 3 to refill the round");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void roundEndsByItselfAndDrainsNothing(GameTestHelper helper) {
        ServerPlayer player = holder(helper);
        ItemStack gu = hold(player, ModItems.LITTLE_LIGHT_GU_1.get(), 14);
        charge(helper, player, gu);

        tick(helper, player, gu, 1L);
        assertTrue(LittleLightGuItem.isBright(gu), "one tick short of the round it still shines");
        assertEquals(14 - ROUND_HUNGER, hunger(gu), "nothing drains during the round");
        tick(helper, player, gu, 0L);

        assertFalse(LittleLightGuItem.isBright(gu), "the round is over");
        assertFalse(gu.has(ModDataComponents.LITTLE_LIGHT_BRIGHT_UNTIL));
        assertEquals(14 - ROUND_HUNGER, hunger(gu), "going out costs nothing");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void roundRunsInTheBagToo(GameTestHelper helper) {
        ServerPlayer player = holder(helper);
        ItemStack gu = hold(player, ModItems.LITTLE_LIGHT_GU_1.get(), 14);
        charge(helper, player, gu);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.getInventory().setItem(10, gu);

        tick(helper, player, gu, 0L);

        assertFalse(LittleLightGuItem.isBright(gu), "the round counts world time, held or not");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void brightWithoutADeadlineGoesOut(GameTestHelper helper) {
        ServerPlayer player = holder(helper);
        ItemStack gu = hold(player, ModItems.LITTLE_LIGHT_GU_1.get(), 14);
        gu.set(ModDataComponents.LITTLE_LIGHT_BRIGHT, true);

        gu.getItem().inventoryTick(gu, helper.getLevel(), player, 0, true);

        assertFalse(LittleLightGuItem.isBright(gu));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void creativeTurnsOnHungryAndKeepsTheFood(GameTestHelper helper) {
        ServerPlayer player = holder(helper);
        player.getAbilities().instabuild = true;
        ItemStack gu = hold(player, ModItems.LITTLE_LIGHT_GU_1.get(), 1);
        ItemStack dust = new ItemStack(Items.GLOWSTONE_DUST, 20);
        player.getInventory().setItem(10, dust);

        charge(helper, player, gu);

        assertTrue(LittleLightGuItem.isBright(gu), "creative skips the hungry gate");
        assertEquals(20, dust.getCount(), "creative meals use nothing up");
        helper.succeed();
    }

    /** 本轮还剩 {@code ticksLeft} 时的一次 {@code inventoryTick}。 */
    private static void tick(GameTestHelper helper, ServerPlayer player, ItemStack gu, long ticksLeft) {
        gu.set(ModDataComponents.LITTLE_LIGHT_BRIGHT_UNTIL, helper.getLevel().getGameTime() + ticksLeft);
        gu.getItem().inventoryTick(gu, helper.getLevel(), player, 0, true);
    }

    private static ServerPlayer holder(GameTestHelper helper) { return holder(helper, null); }

    private static ServerPlayer holder(GameTestHelper helper, @Nullable List<Component> inbox) {
        return GameTestGu.awakened(GameTestPlayers.survival(helper, "gzr-little-light-mock").inbox(inbox).at(STAND),
                Rank.FIVE);
    }
}
