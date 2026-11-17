package net.alex.guzhenren.gametest;

import java.util.List;
import java.util.function.Supplier;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.Skin;
import net.alex.guzhenren.gameplay.path.PathEffectService;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.HumanStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.gameplay.path.strength.StrengthPathBranch;
import net.alex.guzhenren.item.GuItem.Refusal;
import net.alex.guzhenren.item.gu.GuBehavior;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.mortal.behavior.TimedBuffBehavior;
import net.alex.guzhenren.item.gu.mortal.behavior.TimedEffects;
import net.alex.guzhenren.item.gu.mortal.path.rule.AmplifyEffectGuItem.Amplification;
import net.alex.guzhenren.item.gu.mortal.path.strength.BeastStrengthBehavior;
import net.alex.guzhenren.item.gu.mortal.path.strength.HumanStrengthBehavior;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.alex.guzhenren.registry.effect.ModPathEffectTypes;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;
import static net.alex.guzhenren.gametest.GameTestGu.click;
import static net.alex.guzhenren.gametest.GameTestGu.hunger;
import static net.alex.guzhenren.gametest.GameTestGu.refined;
import static net.alex.guzhenren.gametest.GameTestPlayers.command;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertFalse;
import static net.alex.guzhenren.gametest.UnitAsserts.assertNotNull;
import static net.alex.guzhenren.gametest.UnitAsserts.assertNull;
import static net.alex.guzhenren.gametest.UnitAsserts.assertSame;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 活玩家的道痕[Path Marks]互斥：持兽力[Beast Strength]虚影流（含运行中兽力增益）的身体拒一切人力蛊，持人力钧力流的拒一
 * 切兽力蛊，永久或限时皆然；服务与力道命令拒同样写入，移除照常；基础力道蛊不受影响。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MarkInterferenceGameTests {
    private MarkInterferenceGameTests() {}

    private static final String MARK_INTERFERENCE = "guzhenren.item.failed.mark_interference";
    private static final List<Supplier<Item>> HUMAN_GU = List.of(ModItems.JIN_STRENGTH_GU_1,
            ModItems.TENS_JIN_STRENGTH_GU_2, ModItems.JUN_STRENGTH_GU_3, ModItems.TENS_JUN_STRENGTH_GU_4);
    private static final List<Supplier<Item>> BEAST_GU = List.of(ModItems.WHITE_BOAR_GU_1,
            ModItems.BLACK_BOAR_GU_1, ModItems.BEAR_STRENGTH_GU_1);
    private static final List<Supplier<Item>> BEAST_BUFF_GU = List.of(ModItems.FLOWER_BOAR_GU_1,
            ModItems.BRUTE_FORCE_LONGHORN_BEETLE_GU_1);

    @GameTest(template = "empty9x9x9")
    public static void aBeastBodyIsRefusedEveryHumanStrengthGu(GameTestHelper helper) {
        ServerPlayer player = awakened(helper);
        PathStrengthService.grant(player, BeastStrength.WHITE_BOAR);

        for (Supplier<Item> item : HUMAN_GU) assertInterferes(check(player, item.get(), HumanStrengthBehavior.class));
        assertFalse(PathStrengthService.addHumanStrength(player, HumanStrength.JIN, 1), "the service wrote a layer");
        assertEquals(0, PathStrengthService.getHumanStrength(player, HumanStrength.JIN));

        ItemStack stack = refined(ModItems.JIN_STRENGTH_GU_1.get(), 12);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        long essence = ApertureEssenceService.getSpendable(player);
        click(helper, player, stack);
        assertEquals(0, PathStrengthService.getHumanStrength(player, HumanStrength.JIN), "the refused use wrote");
        assertEquals(essence, ApertureEssenceService.getSpendable(player), "the refused use spent essence");
        assertEquals(12, hunger(stack), "the refused use fed on the Gu");
        assertSame(stack, player.getMainHandItem());
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aHumanBodyIsRefusedEveryBeastGuPermanentOrTimed(GameTestHelper helper) {
        ServerPlayer player = awakened(helper);
        PathStrengthService.addHumanStrength(player, HumanStrength.JIN, 1);

        for (Supplier<Item> item : BEAST_GU) assertInterferes(check(player, item.get(), BeastStrengthBehavior.class));
        for (Supplier<Item> item : BEAST_BUFF_GU) assertInterferes(check(player, item.get(), TimedBuffBehavior.class));
        assertFalse(PathStrengthService.grant(player, BeastStrength.BEAR), "the service granted a beast");
        assertTrue(PathStrengthService.get(player).beasts().isEmpty(), "a beast strength was stored");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aRunningBeastBuffRefusesHumanStrengthUntilItEnds(GameTestHelper helper) {
        ServerPlayer player = awakened(helper);
        TimedEffects.grantBuff(player, (TendedGuItem) ModItems.FLOWER_BOAR_GU_1.get(), ModEffects.FLOWER_BOAR_GU, 0,
                600L, Amplification.NONE);

        assertSame(StrengthPathBranch.BEAST_STRENGTH_PHANTOM,
                PathStrengthService.getInterference(player, StrengthPathBranch.HUMAN_JUN_STRENGTH));
        assertInterferes(check(player, ModItems.JIN_STRENGTH_GU_1.get(), HumanStrengthBehavior.class));

        PathEffectService.removeType(player, ModPathEffectTypes.BUFF);
        assertNull(check(player, ModItems.JIN_STRENGTH_GU_1.get(), HumanStrengthBehavior.class));
        assertTrue(PathStrengthService.addHumanStrength(player, HumanStrength.JIN, 1), "the ended buff still blocked");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theStrengthCommandsRefuseTheInterferingWriteAndLetRemovalsThrough(GameTestHelper helper) {
        ServerPlayer player = awakened(helper);
        command(player, "gzr path strength grant bear");
        command(player, "gzr path strength human jin add 1");
        command(player, "gzr path strength human jin set 3");
        assertEquals(0, PathStrengthService.getHumanStrength(player, HumanStrength.JIN), "a command wrote a layer");

        command(player, "gzr path strength revoke bear");
        assertFalse(PathStrengthService.has(player, BeastStrength.BEAR), "the revoke was refused");
        command(player, "gzr path strength human jin add 2");
        assertEquals(2, PathStrengthService.getHumanStrength(player, HumanStrength.JIN));

        command(player, "gzr path strength grant white_boar");
        assertFalse(PathStrengthService.has(player, BeastStrength.WHITE_BOAR), "a command granted a beast");
        command(player, "gzr path strength human jin sub 1");
        assertEquals(1, PathStrengthService.getHumanStrength(player, HumanStrength.JIN), "the sub was refused");
        command(player, "gzr path strength human jin set 0");
        command(player, "gzr path strength grant white_boar");
        assertTrue(PathStrengthService.has(player, BeastStrength.WHITE_BOAR), "the emptied body was still blocked");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void eachBranchKeepsItsOwnGatesAndNormalGuStayFree(GameTestHelper helper) {
        ServerPlayer player = awakened(helper);
        BodyService.wearSkin(player, Skin.IRON_SKIN_4, 0);
        PathStrengthService.grant(player, BeastStrength.WHITE_BOAR);
        assertNull(check(player, ModItems.BEAR_STRENGTH_GU_1.get(), BeastStrengthBehavior.class));
        assertNull(check(player, ModItems.FLOWER_BOAR_GU_1.get(), TimedBuffBehavior.class));
        assertEquals("guzhenren.item.failed.beast_strength_held",
                check(player, ModItems.WHITE_BOAR_GU_1.get(), BeastStrengthBehavior.class).key());
        assertNull(check(player, ModItems.HORIZONTAL_CRASH_GU_3.get(), TimedBuffBehavior.class));

        PathStrengthService.clear(player);
        PathStrengthService.setHumanStrength(player, HumanStrength.JIN, HumanStrength.JIN.getMaxLayers());
        assertNull(check(player, ModItems.TENS_JIN_STRENGTH_GU_2.get(), HumanStrengthBehavior.class));
        assertEquals("guzhenren.item.failed.human_strength_full",
                check(player, ModItems.JIN_STRENGTH_GU_1.get(), HumanStrengthBehavior.class).key());
        assertNull(check(player, ModItems.HORIZONTAL_CRASH_GU_3.get(), TimedBuffBehavior.class));
        helper.succeed();
    }

    private static <M extends GuBehavior> @Nullable Refusal check(ServerPlayer player, Item item, Class<M> type) {
        MortalGuItem gu = (MortalGuItem) item;
        M behavior = gu.getBehavior(type);
        assertNotNull(behavior, () -> item + " lacks its " + type.getSimpleName());
        return behavior.checkEffect(gu, player, new ItemStack(item));
    }

    private static void assertInterferes(@Nullable Refusal refusal) {
        assertNotNull(refusal, () -> "an interfering Gu was allowed");
        assertEquals(MARK_INTERFERENCE, refusal.key());
    }

    private static ServerPlayer awakened(GameTestHelper helper) {
        return GameTestGu.awakened(
                GameTestPlayers.survival(helper, "gzr-mark-interference-gametest").onGround(), Rank.ONE);
    }
}
