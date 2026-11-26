package net.alex.guzhenren.gametest;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.effect.timed.VitalityLeafGuEffect;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorageService;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.gameplay.mind.KillingMoveData;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.killingmove.KillingMove;
import net.alex.guzhenren.item.gu.killingmove.KillingMoveService;
import net.alex.guzhenren.lifecycle.PlayerDataService;
import net.alex.guzhenren.network.ClientIntents;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.alex.guzhenren.registry.entity.ModEntityTypes;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;
import static net.alex.guzhenren.gametest.GameTestGu.hold;
import static net.alex.guzhenren.gametest.GameTestGu.hunger;
import static net.alex.guzhenren.gametest.GameTestGu.inSlot;
import static net.alex.guzhenren.gametest.GameTestGu.refined;
import static net.alex.guzhenren.gametest.GameTestPlayers.exhausted;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertFalse;
import static net.alex.guzhenren.gametest.UnitAsserts.assertNotNull;
import static net.alex.guzhenren.gametest.UnitAsserts.assertNull;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 活玩家的杀招[Killing Move]（经 {@code ClientIntents.killingMove}）。
 * <ul>
 *   <li>未开招时轻点由主手月光蛊[Moonlight Gu]射一道月刃，小光蛊[Little Light Gu]不计费；不领招的血月蛊[Blood Moon Gu]
 *       开招时也射普通月刃。</li>
 *   <li>选招即开：配合蛊扣 3 饱食[Hunger]、不扣真元[Essence]，配合蛊在快捷栏、主背包、副手或空窍存储皆可（存储者写
 *       回）；再选免费关。</li>
 *   <li>开招时轻点射双倍伤害月刃，配合蛊计 20 真元、不扣饱食；月刃伤害读自被击中铁傀儡（100 血，无甲）。</li>
 *   <li>拒绝：真元不够付两蛊（不计费、招仍开）；配合蛊不在（提示并关招）；配合蛊低于启动饱食（不开）；未知招、蛊不领的
 *       招、野生配合或主蛊（开不了）。</li>
 *   <li>死亡关招、仍保留学会；功倍蛊[Amplify Effect Gu]计费仍使饱食见底、从主手驱动的增幅蛊力竭。</li>
 *   <li>轮盘列表（{@code KillingMoveService.listUsable}）只列已学会且带配合蛊或服务端标为存储支撑的招。</li>
 *   <li>杀招提示：仅落地的施展回应 {@code Cast}，写明招与两蛊（主蛊在前），取施展刚结束时状态；开关招、普通催动、真元不
 *       足、未学会均不回应。</li>
 *   <li>学会：读书即学会并耗书，创造模式不耗，再读拒绝且不耗；未学会时施展不落地，普通催动照射；无
 *       {@code keepInventory} 死亡忘招，有则记住。</li>
 * </ul>
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class KillingMoveGameTests {
    private KillingMoveGameTests() {}

    private static final Vec3 TARGET = new Vec3(4.5D, 1.0D, 7.5D);
    private static final BlockPos BLADE_POS = new BlockPos(4, 2, 4);
    private static final int PARTNER_SLOT = 2;
    private static final String MOVE = KillingMove.LITTLE_LIGHT_AMPLIFY.getId();
    private static final long BLADE_ESSENCE = 10L;
    private static final int BLADE_HUNGER = 3;
    private static final long BLOOD_MOON_ESSENCE = 640L;
    private static final long PARTNER_ESSENCE = KillingMove.LITTLE_LIGHT_AMPLIFY.getPartnerEssence();
    private static final int START_HUNGER = KillingMove.LITTLE_LIGHT_AMPLIFY.getStartHunger();

    @GameTest(template = "empty9x9x9")
    public static void aPlainUseFiresOneBladeAndLeavesThePartnerUnbilled(GameTestHelper helper) {
        ServerPlayer player = awakened(helper, null);
        IronGolem golem = target(helper);
        ItemStack moonlight = hold(player, ModItems.MOONLIGHT_GU_1.get(), 12);
        ItemStack light = inSlot(player, PARTNER_SLOT, ModItems.LITTLE_LIGHT_GU_1.get(), 12);
        long essence = ApertureEssenceService.getSpendable(player);

        ClientIntents.killingMove(player, KillingMoveService.PLAIN, KillingMoveService.NO_TARGET);

        assertEquals(1, blades(helper));
        assertEquals(essence - BLADE_ESSENCE, ApertureEssenceService.getSpendable(player));
        assertEquals(12 - BLADE_HUNGER, hunger(moonlight));
        assertEquals(12, hunger(light));
        assertTrue(player.getCooldowns().isOnCooldown(moonlight.getItem()), "the plain use took no cooldown");
        helper.succeedWhen(() -> assertEquals(100.0D - 6.0D, golem.getHealth()));
    }

    @GameTest(template = "empty9x9x9")
    public static void readingTheBookLearnsTheMoveAndUsesItUp(GameTestHelper helper) {
        ServerPlayer player = reader(helper);
        assertFalse(KillingMoveService.knows(player, KillingMove.LITTLE_LIGHT_AMPLIFY));

        ItemStack book = read(player);

        assertTrue(KillingMoveService.knows(player, KillingMove.LITTLE_LIGHT_AMPLIFY));
        assertTrue(book.isEmpty());
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aSecondReadRefusesAndKeepsTheBook(GameTestHelper helper) {
        ServerPlayer player = reader(helper);
        KillingMoveService.learn(player, KillingMove.LITTLE_LIGHT_AMPLIFY);

        ItemStack book = read(player);

        assertEquals(1, book.getCount());
        assertFalse(KillingMoveService.learn(player, KillingMove.LITTLE_LIGHT_AMPLIFY));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aCreativeReadLearnsAndKeepsTheBook(GameTestHelper helper) {
        ServerPlayer player = reader(helper);
        player.getAbilities().instabuild = true;

        ItemStack book = read(player);

        assertTrue(KillingMoveService.knows(player, KillingMove.LITTLE_LIGHT_AMPLIFY));
        assertEquals(1, book.getCount());
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void anUnlearnedMoveIsNotListedAndOnlyThePlainUseLands(GameTestHelper helper) {
        ServerPlayer player = awakened(helper, null);
        PlayerData.get(player).setKillingMoves(KillingMoveData.DEFAULT);
        hold(player, ModItems.MOONLIGHT_GU_1.get(), 12);
        ItemStack light = inSlot(player, PARTNER_SLOT, ModItems.LITTLE_LIGHT_GU_1.get(), 12);
        long essence = ApertureEssenceService.getSpendable(player);

        assertTrue(KillingMoveService.listUsable(player, ModItems.MOONLIGHT_GU_1.get()).isEmpty(),
                "an unlearned move was listed");
        ClientIntents.killingMove(player, MOVE, KillingMoveService.NO_TARGET);
        assertEquals(0, blades(helper));
        assertEquals(essence, ApertureEssenceService.getSpendable(player));
        assertEquals(12, hunger(light));

        ClientIntents.killingMove(player, KillingMoveService.PLAIN, KillingMoveService.NO_TARGET);
        assertEquals(1, blades(helper));
        assertEquals(12, hunger(light));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void onlyADeathWithoutKeepInventoryForgetsTheMove(GameTestHelper helper) {
        ServerPlayer from = reader(helper);
        ServerPlayer to = reader(helper);
        KillingMoveService.learn(to, KillingMove.LITTLE_LIGHT_AMPLIFY);

        PlayerDataService.onClone(from, to, true, true);
        assertTrue(KillingMoveService.knows(to, KillingMove.LITTLE_LIGHT_AMPLIFY));

        PlayerDataService.onClone(from, to, true, false);
        assertFalse(KillingMoveService.knows(to, KillingMove.LITTLE_LIGHT_AMPLIFY));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aSwitchedOnMoveDoublesTheBladeAndBillsThePartnerEssenceOnly(GameTestHelper helper) {
        ServerPlayer player = awakened(helper, null);
        IronGolem golem = target(helper);
        ItemStack moonlight = hold(player, ModItems.MOONLIGHT_GU_1.get(), 12);
        ItemStack light = inSlot(player, PARTNER_SLOT, ModItems.LITTLE_LIGHT_GU_1.get(), 12);
        long essence = ApertureEssenceService.getSpendable(player);

        ClientIntents.killingMove(player, MOVE, KillingMoveService.NO_TARGET);
        assertTrue(KillingMoveService.isActive(player, KillingMove.LITTLE_LIGHT_AMPLIFY), "the pick did not switch on");
        assertEquals(0, blades(helper));
        assertEquals(essence, ApertureEssenceService.getSpendable(player), "the start took essence");
        assertEquals(12 - START_HUNGER, hunger(light));

        ClientIntents.killingMove(player, KillingMoveService.PLAIN, KillingMoveService.NO_TARGET);
        float afterwards = KillingMoveService.getDamageFactor(player, (MortalGuItem) moonlight.getItem());

        assertEquals(1, blades(helper));
        assertEquals(essence - BLADE_ESSENCE - PARTNER_ESSENCE, ApertureEssenceService.getSpendable(player));
        assertEquals(12 - BLADE_HUNGER, hunger(moonlight));
        assertEquals(12 - START_HUNGER, hunger(light), "a cast took the partner's hunger");
        assertEquals(1.0D, afterwards);
        assertTrue(KillingMoveService.isActive(player, KillingMove.LITTLE_LIGHT_AMPLIFY), "a cast switched off");
        helper.succeedWhen(() -> assertEquals(100.0D - 12.0D, golem.getHealth()));
    }

    @GameTest(template = "empty9x9x9")
    public static void onlyALandedCastAnswersTheTooltip(GameTestHelper helper) {
        ServerPlayer player = awakened(helper, null);
        inSlot(player, PARTNER_SLOT, ModItems.LITTLE_LIGHT_GU_1.get(), 12);
        Item moonlight = ModItems.MOONLIGHT_GU_1.get();
        hold(player, moonlight, 12);

        assertNull(KillingMoveService.drive(player, MOVE));
        KillingMoveService.Cast cast = assertNotNull(KillingMoveService.drive(player, KillingMoveService.PLAIN),
                () -> "a landed cast answered no tooltip");
        assertEquals(KillingMove.LITTLE_LIGHT_AMPLIFY, cast.move());
        assertEquals(2, cast.used().size());
        KillingMoveService.Used main = cast.used().get(0);
        KillingMoveService.Used partner = cast.used().get(1);
        assertEquals(moonlight, main.stack().getItem());
        assertEquals(12 - BLADE_HUNGER, hunger(main.stack()));
        assertEquals(ModItems.LITTLE_LIGHT_GU_1.get(), partner.stack().getItem());
        assertEquals(12 - START_HUNGER, hunger(partner.stack()));
        assertFalse(main.spent() || partner.spent(), "a living Gu came back spent");

        assertNull(KillingMoveService.drive(player, MOVE));
        hold(player, moonlight, 12);
        player.getCooldowns().removeCooldown(moonlight);
        assertNull(KillingMoveService.drive(player, KillingMoveService.PLAIN));
        PlayerData.get(player).setKillingMoves(KillingMoveData.DEFAULT);
        assertNull(KillingMoveService.drive(player, MOVE));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void tooLittleEssenceForBothRefusesAndBillsNothing(GameTestHelper helper) {
        ServerPlayer player = awakened(helper, null);
        ItemStack moonlight = hold(player, ModItems.MOONLIGHT_GU_1.get(), 12);
        ItemStack light = inSlot(player, PARTNER_SLOT, ModItems.LITTLE_LIGHT_GU_1.get(), 12);
        ClientIntents.killingMove(player, MOVE, KillingMoveService.NO_TARGET);
        long left = BLADE_ESSENCE + PARTNER_ESSENCE - 1L;
        ApertureEssenceService.consume(player, ApertureEssenceService.getSpendable(player) - left);

        assertNull(KillingMoveService.drive(player, KillingMoveService.PLAIN));

        assertEquals(0, blades(helper));
        assertEquals(left, ApertureEssenceService.getSpendable(player));
        assertEquals(12, hunger(moonlight));
        assertEquals(12 - START_HUNGER, hunger(light));
        assertFalse(player.getCooldowns().isOnCooldown(moonlight.getItem()), "a refused cast took a cooldown");
        assertTrue(KillingMoveService.isActive(player, KillingMove.LITTLE_LIGHT_AMPLIFY), "a refused cast switched off");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aWrongMoveOrNoUsablePartnerSwitchesNothingOn(GameTestHelper helper) {
        ServerPlayer player = awakened(helper, null);
        ItemStack moonlight = hold(player, ModItems.MOONLIGHT_GU_1.get(), 12);
        player.getInventory().setItem(3, new ItemStack(ModItems.LITTLE_LIGHT_GU_1.get()));
        inSlot(player, 5, ModItems.MOONGLOW_GU_2.get(), 24);
        long essence = ApertureEssenceService.getSpendable(player);

        assertTrue(KillingMoveService.listUsable(player, ModItems.MOONLIGHT_GU_1.get()).isEmpty(),
                "a move with only a wild partner was listed");
        for (String wrong : new String[] {MOVE, "km_mortal_unknown", "moonlight_gu_1"}) {
            ClientIntents.killingMove(player, wrong, KillingMoveService.NO_TARGET);
        }
        assertNull(KillingMoveService.getActive(player));
        ItemStack light = inSlot(player, 20, ModItems.LITTLE_LIGHT_GU_1.get(), 12);
        player.getCooldowns().addCooldown(light.getItem(), 20);
        ClientIntents.killingMove(player, MOVE, KillingMoveService.NO_TARGET);

        assertTrue(KillingMoveService.isActive(player, KillingMove.LITTLE_LIGHT_AMPLIFY),
                "a refined partner on the vanilla cooldown kept the move from switching on");
        assertEquals(0, blades(helper));
        assertEquals(essence, ApertureEssenceService.getSpendable(player));
        assertEquals(12, hunger(moonlight));
        assertEquals(12 - START_HUNGER, hunger(light));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aPartnerInTheMainInventoryBacksTheMove(GameTestHelper helper) {
        ServerPlayer player = awakened(helper, null);
        IronGolem golem = target(helper);
        hold(player, ModItems.MOONLIGHT_GU_1.get(), 12);
        ItemStack light = inSlot(player, 20, ModItems.LITTLE_LIGHT_GU_1.get(), 12);

        assertEquals(List.of(KillingMove.LITTLE_LIGHT_AMPLIFY),
                KillingMoveService.listUsable(player, ModItems.MOONLIGHT_GU_1.get()));
        ClientIntents.killingMove(player, MOVE, KillingMoveService.NO_TARGET);
        ClientIntents.killingMove(player, KillingMoveService.PLAIN, KillingMoveService.NO_TARGET);

        assertEquals(1, blades(helper));
        assertEquals(12 - START_HUNGER, hunger(light));
        helper.succeedWhen(() -> assertEquals(100.0D - 12.0D, golem.getHealth()));
    }

    @GameTest(template = "empty9x9x9")
    public static void aPartnerInTheOffhandBacksTheMove(GameTestHelper helper) {
        ServerPlayer player = awakened(helper, null);
        IronGolem golem = target(helper);
        hold(player, ModItems.MOONLIGHT_GU_1.get(), 12);
        ItemStack light = refined(ModItems.LITTLE_LIGHT_GU_1.get(), 12);
        player.setItemInHand(InteractionHand.OFF_HAND, light);

        assertEquals(List.of(KillingMove.LITTLE_LIGHT_AMPLIFY),
                KillingMoveService.listUsable(player, ModItems.MOONLIGHT_GU_1.get()));
        ClientIntents.killingMove(player, MOVE, KillingMoveService.NO_TARGET);
        ClientIntents.killingMove(player, KillingMoveService.PLAIN, KillingMoveService.NO_TARGET);

        assertEquals(1, blades(helper));
        assertEquals(12 - START_HUNGER, hunger(light));
        helper.succeedWhen(() -> assertEquals(100.0D - 12.0D, golem.getHealth()));
    }

    @GameTest(template = "empty9x9x9")
    public static void aStoredPartnerBacksTheMoveAndIsWrittenBack(GameTestHelper helper) {
        ServerPlayer player = awakened(helper, null);
        IronGolem golem = target(helper);
        hold(player, ModItems.MOONLIGHT_GU_1.get(), 12);
        ApertureStorageService.set(player, ApertureData.PRIMARY,
                List.of(new ItemStack(Items.STICK), refined(ModItems.LITTLE_LIGHT_GU_1.get(), 12)));
        long essence = ApertureEssenceService.getSpendable(player);

        assertTrue(KillingMoveService.listUsable(player, ModItems.MOONLIGHT_GU_1.get()).isEmpty(),
                "a stored partner was listed before the server marked it");
        KillingMoveService.refreshStoreBacked(player);
        assertEquals(List.of(KillingMove.LITTLE_LIGHT_AMPLIFY),
                KillingMoveService.listUsable(player, ModItems.MOONLIGHT_GU_1.get()));
        ClientIntents.killingMove(player, MOVE, KillingMoveService.NO_TARGET);
        assertEquals(12 - START_HUNGER, hunger(ApertureStorageService.getItems(player, ApertureData.PRIMARY).get(1)),
                "the start hunger was not written back to the store");
        ClientIntents.killingMove(player, KillingMoveService.PLAIN, KillingMoveService.NO_TARGET);

        assertEquals(1, blades(helper));
        assertEquals(essence - BLADE_ESSENCE - PARTNER_ESSENCE, ApertureEssenceService.getSpendable(player));
        assertEquals(12 - START_HUNGER, hunger(ApertureStorageService.getItems(player, ApertureData.PRIMARY).get(1)));
        helper.succeedWhen(() -> assertEquals(100.0D - 12.0D, golem.getHealth()));
    }

    @GameTest(template = "empty9x9x9")
    public static void aPartnerBelowTheStartHungerRefusesToStart(GameTestHelper helper) {
        ServerPlayer player = awakened(helper, null);
        hold(player, ModItems.MOONLIGHT_GU_1.get(), 12);
        ItemStack light = inSlot(player, PARTNER_SLOT, ModItems.LITTLE_LIGHT_GU_1.get(), START_HUNGER - 1);
        long essence = ApertureEssenceService.getSpendable(player);

        ClientIntents.killingMove(player, MOVE, KillingMoveService.NO_TARGET);

        assertNull(KillingMoveService.getActive(player));
        assertEquals(START_HUNGER - 1, hunger(light));
        assertEquals(0, blades(helper));
        assertEquals(essence, ApertureEssenceService.getSpendable(player));

        ClientIntents.killingMove(player, KillingMoveService.PLAIN, KillingMoveService.NO_TARGET);
        assertEquals(1, blades(helper));
        assertEquals(essence - BLADE_ESSENCE, ApertureEssenceService.getSpendable(player), "a plain tap billed more");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aWildMainLandsNothing(GameTestHelper helper) {
        ServerPlayer player = awakened(helper, null);
        inSlot(player, PARTNER_SLOT, ModItems.LITTLE_LIGHT_GU_1.get(), 12);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MOONLIGHT_GU_1.get()));
        long essence = ApertureEssenceService.getSpendable(player);

        ClientIntents.killingMove(player, KillingMoveService.PLAIN, KillingMoveService.NO_TARGET);
        ClientIntents.killingMove(player, MOVE, KillingMoveService.NO_TARGET);

        assertEquals(0, blades(helper));
        assertEquals(essence, ApertureEssenceService.getSpendable(player));
        assertTrue(KillingMoveService.asBladeGu(player.getMainHandItem()) == null, "a wild Moonlight Gu was taken");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aBloodMoonGuFiresItsPlainBladeButNoMove(GameTestHelper helper) {
        ServerPlayer player = awakened(helper, null);
        ItemStack light = inSlot(player, PARTNER_SLOT, ModItems.LITTLE_LIGHT_GU_1.get(), 12);
        ItemStack bloodMoon = hold(player, ModItems.BLOOD_MOON_GU_3.get(), 48);

        assertNotNull(KillingMoveService.asBladeGu(bloodMoon), () -> "a refined Blood Moon Gu is not a blade Gu");
        assertTrue(KillingMoveService.listUsable(player, bloodMoon.getItem()).isEmpty(),
                "a Blood Moon Gu listed a killing move");
        ClientIntents.killingMove(player, MOVE, KillingMoveService.NO_TARGET);
        assertEquals(0, blades(helper));
        assertEquals(48, hunger(bloodMoon));

        ClientIntents.killingMove(player, KillingMoveService.PLAIN, KillingMoveService.NO_TARGET);
        assertEquals(1, blades(helper));
        assertEquals(12, hunger(light));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void outsideAKillingMoveTheFactorIsOneAndBillsNothing(GameTestHelper helper) {
        ServerPlayer player = awakened(helper, null);
        ItemStack moonlight = hold(player, ModItems.MOONLIGHT_GU_1.get(), 12);
        ItemStack light = inSlot(player, PARTNER_SLOT, ModItems.LITTLE_LIGHT_GU_1.get(), 12);
        ItemStack offhand = refined(ModItems.LITTLE_LIGHT_GU_1.get(), 12);
        player.setItemInHand(InteractionHand.OFF_HAND, offhand);
        long essence = ApertureEssenceService.getSpendable(player);

        float factor = KillingMoveService.getDamageFactor(player, (MortalGuItem) moonlight.getItem());

        assertEquals(1.0D, factor);
        assertEquals(essence, ApertureEssenceService.getSpendable(player));
        assertEquals(12, hunger(light));
        assertEquals(12, hunger(offhand));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aPartnerGoneMidMoveRefusesTheTapAndSwitchesOff(GameTestHelper helper) {
        ServerPlayer player = awakened(helper, null);
        ItemStack moonlight = hold(player, ModItems.MOONLIGHT_GU_1.get(), 12);
        inSlot(player, PARTNER_SLOT, ModItems.LITTLE_LIGHT_GU_1.get(), 12);
        ClientIntents.killingMove(player, MOVE, KillingMoveService.NO_TARGET);
        player.getInventory().setItem(PARTNER_SLOT, ItemStack.EMPTY);
        long essence = ApertureEssenceService.getSpendable(player);

        ClientIntents.killingMove(player, KillingMoveService.PLAIN, KillingMoveService.NO_TARGET);

        assertEquals(0, blades(helper));
        assertEquals(essence, ApertureEssenceService.getSpendable(player));
        assertEquals(12, hunger(moonlight));
        assertNull(KillingMoveService.getActive(player));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aSecondPickSwitchesTheMoveOffForFree(GameTestHelper helper) {
        ServerPlayer player = awakened(helper, null);
        hold(player, ModItems.MOONLIGHT_GU_1.get(), 12);
        ItemStack light = inSlot(player, PARTNER_SLOT, ModItems.LITTLE_LIGHT_GU_1.get(), 12);
        long essence = ApertureEssenceService.getSpendable(player);

        ClientIntents.killingMove(player, MOVE, KillingMoveService.NO_TARGET);
        ClientIntents.killingMove(player, MOVE, KillingMoveService.NO_TARGET);

        assertNull(KillingMoveService.getActive(player));
        assertEquals(12 - START_HUNGER, hunger(light), "switching off changed the partner's hunger");
        assertEquals(essence, ApertureEssenceService.getSpendable(player));
        ClientIntents.killingMove(player, KillingMoveService.PLAIN, KillingMoveService.NO_TARGET);
        assertEquals(1, blades(helper));
        assertEquals(essence - BLADE_ESSENCE, ApertureEssenceService.getSpendable(player), "a plain tap billed more");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void anActiveMoveLeavesABloodMoonTapPlain(GameTestHelper helper) {
        ServerPlayer player = awakened(helper, null);
        hold(player, ModItems.MOONLIGHT_GU_1.get(), 12);
        ItemStack light = inSlot(player, PARTNER_SLOT, ModItems.LITTLE_LIGHT_GU_1.get(), 12);
        ClientIntents.killingMove(player, MOVE, KillingMoveService.NO_TARGET);
        hold(player, ModItems.BLOOD_MOON_GU_3.get(), 48);
        long essence = ApertureEssenceService.getSpendable(player);

        ClientIntents.killingMove(player, KillingMoveService.PLAIN, KillingMoveService.NO_TARGET);

        assertEquals(1, blades(helper));
        assertEquals(essence - BLOOD_MOON_ESSENCE, ApertureEssenceService.getSpendable(player),
                "a Blood Moon tap billed the partner");
        assertEquals(12 - START_HUNGER, hunger(light));
        assertTrue(KillingMoveService.isActive(player, KillingMove.LITTLE_LIGHT_AMPLIFY), "the move switched off");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aDeathSwitchesTheMoveOffButKeepsItLearned(GameTestHelper helper) {
        ServerPlayer from = reader(helper);
        ServerPlayer to = reader(helper);
        PlayerData.get(to).setKillingMoves(KillingMoveData.DEFAULT.withLearned(MOVE).withActive(MOVE));

        PlayerDataService.onClone(from, to, true, true);

        assertTrue(KillingMoveService.knows(to, KillingMove.LITTLE_LIGHT_AMPLIFY));
        assertNull(KillingMoveService.getActive(to));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void anAmplifierOnAnEmptyBarStillDiesAfterItsRadialDrive(GameTestHelper helper) {
        List<Component> inbox = new ArrayList<>();
        ServerPlayer player = awakened(helper, inbox);
        hold(player, ModItems.AMPLIFY_EFFECT_GU_2.get(), 0);
        player.getInventory().setItem(4, new ItemStack(ModItems.VITALITY_LEAF_GU_1.get()));

        ClientIntents.amplifyDrive(player, 4);

        MobEffectInstance leaf = assertNotNull(player.getEffect(ModEffects.VITALITY_LEAF_GU),
                () -> "the driven Vitality Leaf Gu landed nothing");
        assertEquals(VitalityLeafGuEffect.DURATION_TICKS * 2, leaf.getDuration());
        assertTrue(player.getMainHandItem().isEmpty(), "the exhausted amplifier stayed in the main hand");
        assertTrue(exhausted(inbox), "no exhaustion message");
        helper.succeed();
    }

    private static ServerPlayer awakened(GameTestHelper helper, @Nullable List<Component> inbox) {
        ServerPlayer player = GameTestGu.awakened(
                GameTestPlayers.survival(helper, "gzr-killing-move-gametest").inbox(inbox).onGround(), Rank.FIVE);
        KillingMoveService.learn(player, KillingMove.LITTLE_LIGHT_AMPLIFY);
        return player;
    }

    private static ServerPlayer reader(GameTestHelper helper) {
        return GameTestPlayers.survival(helper, "gzr-killing-move-gametest").onGround();
    }

    private static ItemStack read(ServerPlayer player) {
        ItemStack book = new ItemStack(ModItems.KM_MORTAL_LITTLE_LIGHT_AMPLIFY.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, book);
        book.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
        return book;
    }

    private static IronGolem target(GameTestHelper helper) {
        return helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, TARGET);
    }

    private static int blades(GameTestHelper helper) {
        return helper.getEntities(ModEntityTypes.MOON_BLADE.get(), BLADE_POS, 3.0D).size();
    }
}
