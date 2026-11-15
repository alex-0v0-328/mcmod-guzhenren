package net.alex.guzhenren.gametest;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.gameplay.rewind.PlayerRewindData;
import net.alex.guzhenren.gameplay.rewind.PlayerSnapshot;
import net.alex.guzhenren.gameplay.rewind.RewindClock;
import net.alex.guzhenren.item.gu.killingmove.KillingMove;
import net.alex.guzhenren.item.gu.killingmove.KillingMoveService;
import net.alex.guzhenren.item.gu.mortal.path.strength.AutoUse;
import net.alex.guzhenren.item.gu.stack.VitalGuState;
import net.alex.guzhenren.item.gu.tending.GuForcedUse;
import net.alex.guzhenren.network.ClientIntents;
import net.alex.guzhenren.registry.effect.ModEffects;
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
import static net.alex.guzhenren.gametest.GameTestGu.charge;
import static net.alex.guzhenren.gametest.GameTestGu.hunger;
import static net.alex.guzhenren.gametest.GameTestGu.refined;
import static net.alex.guzhenren.gametest.GameTestPlayers.STAND;
import static net.alex.guzhenren.gametest.GameTestPlayers.exhausted;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertFalse;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 活玩家的强行催动[Forced Use]。
 * <ul>
 *   <li>背包补食后饱食[Hunger]仍缺则警告，不付费、无冷却；5 秒内再催同蛊则效果落地后力竭而死；警告 5 秒失效，同类另一只
 *       重新警告。</li>
 *   <li>食物补得够则不警告；本命蛊只拒绝；创造模式不警告、不死；自动催动静默跳过。</li>
 *   <li>功倍蛊[Amplify Effect Gu]驱动的蛊与杀招[Killing Move]主蛊同流程，被警告时两蛊都不计费；如故蛊[As Before Gu]按点
 *       数同流程。</li>
 *   <li>花豕蛊[Flower Boar Gu]为普通催动（一转，40 真元[Essence]，条 14，每次 3，猪排每块 1）；二转自力更生蛊
 *       [Self Reliance Gu]生命低于 75% 自启（每次 3，食圆石）；人如故[Man As Before Gu]扣 9 点中 2 点。</li>
 *   <li>动作栏不捕获，警告由未发生的事推断，死亡由收件箱力竭消息判断。</li>
 * </ul>
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ForcedUseGameTests {
    private static final int BAG_SLOT = 9;
    private static final long BOAR_COST = 40L;
    private static final long MAN_COST = 163_840L;
    private static final long AMPLIFIER_COST = 2_560L;
    private static final long BLADE_COST = 10L;
    private static final int HOTBAR_SLOT = 2;

    private ForcedUseGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void aShortUseWarnsThenTheSameGuAgainLandsAndExhausts(GameTestHelper helper) {
        List<Component> inbox = new ArrayList<>();
        ServerPlayer player = holder(helper, Rank.ONE, inbox);
        ItemStack boar = refined(ModItems.FLOWER_BOAR_GU_1.get(), 2);
        long essence = ApertureEssenceService.getSpendable(player);

        click(helper, player, boar);

        assertFalse(player.hasEffect(ModEffects.FLOWER_BOAR_GU), "the warned use landed");
        assertEquals(essence, ApertureEssenceService.getSpendable(player), "the warned use paid essence");
        assertEquals(2, hunger(boar), "the warned use paid hunger");
        assertFalse(player.getCooldowns().isOnCooldown(boar.getItem()), "the warned use put a cooldown");
        assertFalse(exhausted(inbox), "the warned Gu died");

        click(helper, player, boar);

        assertTrue(player.hasEffect(ModEffects.FLOWER_BOAR_GU), "the forced use did not land");
        assertEquals(essence - BOAR_COST, ApertureEssenceService.getSpendable(player), "the forced use paid nothing");
        assertTrue(boar.isEmpty(), "the forced Gu outlived its use");
        assertTrue(exhausted(inbox), "no exhaustion message");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = GuForcedUse.WINDOW_TICKS + 40)
    public static void aWarningLapsesAfterFiveSeconds(GameTestHelper helper) {
        List<Component> inbox = new ArrayList<>();
        ServerPlayer player = holder(helper, Rank.ONE, inbox);
        ItemStack boar = refined(ModItems.FLOWER_BOAR_GU_1.get(), 2);
        click(helper, player, boar);

        helper.runAfterDelay(GuForcedUse.WINDOW_TICKS + 1, () -> {
            click(helper, player, boar);

            assertFalse(player.hasEffect(ModEffects.FLOWER_BOAR_GU), "a use past the window was forced");
            assertFalse(boar.isEmpty(), "a use past the window killed the Gu");
            assertFalse(exhausted(inbox), "a use past the window killed the Gu");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void anotherGuOfTheSameKindIsWarnedAfresh(GameTestHelper helper) {
        List<Component> inbox = new ArrayList<>();
        ServerPlayer player = holder(helper, Rank.ONE, inbox);
        ItemStack first = refined(ModItems.FLOWER_BOAR_GU_1.get(), 2);
        ItemStack second = refined(ModItems.FLOWER_BOAR_GU_1.get(), 2);

        click(helper, player, first);
        click(helper, player, second);

        assertFalse(player.hasEffect(ModEffects.FLOWER_BOAR_GU), "the first Gu's warning forced the second");
        assertFalse(second.isEmpty(), "the second Gu died on its first use");

        click(helper, player, first);

        assertFalse(player.hasEffect(ModEffects.FLOWER_BOAR_GU), "the second Gu's warning kept the first one's open");
        assertFalse(first.isEmpty() || exhausted(inbox), "a Gu died without its own second use");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void foodThatFillsTheBarToTheUseMeansNoWarning(GameTestHelper helper) {
        List<Component> inbox = new ArrayList<>();
        ServerPlayer player = holder(helper, Rank.ONE, inbox);
        ItemStack boar = refined(ModItems.FLOWER_BOAR_GU_1.get(), 1);
        ItemStack pork = new ItemStack(Items.PORKCHOP, 2);
        player.getInventory().setItem(BAG_SLOT, pork);

        click(helper, player, boar);

        assertTrue(player.hasEffect(ModEffects.FLOWER_BOAR_GU), "1 and two porkchops cover a use of 3");
        assertFalse(boar.isEmpty() || exhausted(inbox), "a Gu fed to the use died");
        assertEquals(0, hunger(boar));
        assertTrue(pork.isEmpty());
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aVitalGuIsOnlyRefusedAndLives(GameTestHelper helper) {
        List<Component> inbox = new ArrayList<>();
        ServerPlayer player = holder(helper, Rank.ONE, inbox);
        ItemStack boar = refined(ModItems.FLOWER_BOAR_GU_1.get(), 2);
        VitalGuState.bind(boar, player, 0);
        long essence = ApertureEssenceService.getSpendable(player);

        click(helper, player, boar);
        click(helper, player, boar);

        assertFalse(player.hasEffect(ModEffects.FLOWER_BOAR_GU), "a short Vital Gu was forced");
        assertFalse(boar.isEmpty() || exhausted(inbox), "a short Vital Gu died");
        assertEquals(essence, ApertureEssenceService.getSpendable(player), "a refused Vital Gu paid essence");
        assertEquals(2, hunger(boar));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void creativeIsNeverWarnedAndNeverDies(GameTestHelper helper) {
        List<Component> inbox = new ArrayList<>();
        ServerPlayer player = creativeHolder(helper, Rank.ONE, inbox);
        ItemStack boar = refined(ModItems.FLOWER_BOAR_GU_1.get(), 0);

        click(helper, player, boar);

        assertTrue(player.hasEffect(ModEffects.FLOWER_BOAR_GU), "creative was warned");
        assertFalse(exhausted(inbox), "a creative use killed the Gu");
        assertEquals(0, hunger(boar), "the bar drains to 0 for show");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void anAutomaticUseSkipsAShortGuInSilence(GameTestHelper helper) {
        List<Component> inbox = new ArrayList<>();
        ServerPlayer player = holder(helper, Rank.FIVE, inbox);
        ItemStack gu = refined(ModItems.SELF_RELIANCE_GU_2.get(), 2);
        player.getInventory().setItem(BAG_SLOT, gu);
        long essence = ApertureEssenceService.getSpendable(player);
        player.setHealth(player.getMaxHealth() * 0.24F);

        AutoUse.tryStart(player);
        AutoUse.tryStart(player);

        assertFalse(player.hasEffect(ModEffects.SELF_RELIANCE_GU), "a short Gu drove itself");
        assertFalse(gu.isEmpty() || exhausted(inbox), "a short Gu drove itself to death");
        assertEquals(essence, ApertureEssenceService.getSpendable(player), "a skipped use paid essence");
        assertEquals(2, hunger(gu));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void anAsBeforeGuShortOfPointsWarnsThenIsForced(GameTestHelper helper) {
        List<Component> inbox = new ArrayList<>();
        ServerPlayer player = holder(helper, Rank.FIVE, inbox);
        ItemStack gu = refined(ModItems.MAN_AS_BEFORE_GU_5.get(), 1);
        long now = helper.getLevel().getGameTime();
        gu.set(ModDataComponents.RECOVERED_AT, now);
        player.setHealth(player.getMaxHealth());
        PlayerData.get(player).setRewind(PlayerRewindData.DEFAULT
                .with(PlayerSnapshot.capture(player, now - RewindClock.DAY_TICKS)));
        player.setHealth(4.0F);
        long essence = ApertureEssenceService.getSpendable(player);

        click(helper, player, gu);

        assertEquals(4.0F, player.getHealth(), 0.001);
        assertEquals(essence, ApertureEssenceService.getSpendable(player), "the warned use paid essence");
        assertEquals(1, hunger(gu), "the warned use spent a point");
        assertFalse(exhausted(inbox), "the warned Gu died");

        click(helper, player, gu);

        assertEquals(player.getMaxHealth(), player.getHealth(), 0.001);
        assertEquals(essence - MAN_COST, ApertureEssenceService.getSpendable(player), "the forced use paid no essence");
        assertTrue(gu.isEmpty(), "the forced Gu outlived its use");
        assertTrue(exhausted(inbox), "no exhaustion message");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void anAmplifiedDriveShortOfTheDrivenGusHungerWarnsThenIsForced(GameTestHelper helper) {
        List<Component> inbox = new ArrayList<>();
        ServerPlayer player = holder(helper, Rank.FIVE, inbox);
        ItemStack amplifier = refined(ModItems.AMPLIFY_EFFECT_GU_3.get(), 16);
        player.setItemInHand(InteractionHand.MAIN_HAND, amplifier);
        ItemStack boar = refined(ModItems.FLOWER_BOAR_GU_1.get(), 2);
        player.getInventory().setItem(HOTBAR_SLOT, boar);
        long essence = ApertureEssenceService.getSpendable(player);

        ClientIntents.amplifyDrive(player, HOTBAR_SLOT);

        assertFalse(player.hasEffect(ModEffects.FLOWER_BOAR_GU), "the warned drive landed");
        assertEquals(essence, ApertureEssenceService.getSpendable(player), "the warned drive paid essence");
        assertEquals(2, hunger(boar), "the warned drive paid the driven Gu's hunger");
        assertEquals(16, hunger(amplifier), "the warned drive billed the amplifier");
        assertFalse(player.getCooldowns().isOnCooldown(boar.getItem()), "the warned drive put a cooldown");

        ClientIntents.amplifyDrive(player, HOTBAR_SLOT);

        assertTrue(player.hasEffect(ModEffects.FLOWER_BOAR_GU), "the forced drive did not land");
        assertEquals(essence - BOAR_COST - AMPLIFIER_COST, ApertureEssenceService.getSpendable(player));
        assertTrue(boar.isEmpty(), "the forced Gu outlived its use");
        assertTrue(exhausted(inbox), "no exhaustion message");
        assertFalse(amplifier.isEmpty(), "the amplifier died with the driven Gu");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aKillingMoveCastShortOfTheMainGusHungerWarnsThenIsForced(GameTestHelper helper) {
        List<Component> inbox = new ArrayList<>();
        ServerPlayer player = holder(helper, Rank.FIVE, inbox);
        KillingMove move = KillingMove.LITTLE_LIGHT_AMPLIFY;
        KillingMoveService.learn(player, move);
        ItemStack moonlight = refined(ModItems.MOONLIGHT_GU_1.get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, moonlight);
        ItemStack light = refined(ModItems.LITTLE_LIGHT_GU_1.get(), 12);
        player.getInventory().setItem(HOTBAR_SLOT, light);
        KillingMoveService.toggle(player, move);
        assertTrue(KillingMoveService.isActive(player, move), "the move did not switch on");
        long essence = ApertureEssenceService.getSpendable(player);

        assertTrue(KillingMoveService.drive(player, KillingMoveService.PLAIN) == null, "the warned cast answered");
        assertEquals(essence, ApertureEssenceService.getSpendable(player), "the warned cast billed a Gu");
        assertEquals(2, hunger(moonlight), "the warned cast paid the main Gu's hunger");
        assertFalse(exhausted(inbox), "the warned main Gu died");

        assertTrue(KillingMoveService.drive(player, KillingMoveService.PLAIN) != null, "the forced cast did not land");
        assertEquals(essence - BLADE_COST - move.getPartnerEssence(), ApertureEssenceService.getSpendable(player));
        assertTrue(moonlight.isEmpty(), "the forced main Gu outlived its cast");
        assertTrue(exhausted(inbox), "no exhaustion message");
        assertFalse(light.isEmpty(), "the partner died with the main Gu");
        helper.succeed();
    }

    private static void click(GameTestHelper helper, ServerPlayer player, ItemStack gu) {
        player.setItemInHand(InteractionHand.MAIN_HAND, gu);
        charge(helper, player, gu);
    }

    private static ServerPlayer holder(GameTestHelper helper, Rank rank, List<Component> inbox) {
        return GameTestGu.awakened(
                GameTestPlayers.survival(helper, "gzr-forced-use-mock").inbox(inbox).at(STAND), rank);
    }

    private static ServerPlayer creativeHolder(GameTestHelper helper, Rank rank, List<Component> inbox) {
        ServerPlayer player = GameTestPlayers.survival(helper, "gzr-forced-use-mock").creative(() -> true)
                .inbox(inbox).at(STAND);
        player.getAbilities().instabuild = true;
        return GameTestGu.awakened(player, rank);
    }
}
