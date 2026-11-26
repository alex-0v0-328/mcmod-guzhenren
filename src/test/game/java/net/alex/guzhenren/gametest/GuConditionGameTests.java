package net.alex.guzhenren.gametest;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorage;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorageService;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.item.gu.stack.GuCondition;
import net.alex.guzhenren.item.gu.stack.GuCondition.Stage;
import net.alex.guzhenren.item.gu.stack.VitalGuState;
import net.alex.guzhenren.item.gu.tending.GuHealth;
import net.alex.guzhenren.item.gu.tending.TendedGuAccess;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.GameTestGu.charge;
import static net.alex.guzhenren.gametest.GameTestGu.hold;
import static net.alex.guzhenren.gametest.GameTestGu.refined;
import static net.alex.guzhenren.gametest.GameTestPlayers.STAND;
import static net.alex.guzhenren.gametest.GameTestPlayers.messages;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertFalse;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 蛊虫伤势三态：受伤 9 次转濒死、濒死 3 次伤重而亡；计次走真实催动与后援；创造不死；无饱食条不计；炼蛊失败按档掷骰；
 * 本命蛊借玩家回血升档并扣 1/5 最大生命。掷骰用定值随机数。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GuConditionGameTests {

    private static final String DYING_KEY = "guzhenren.item.gu.dying";
    private static final String WOUND_DEAD_KEY = "guzhenren.item.gu.wound_dead";
    private static final String REFINE_DEAD_KEY = "guzhenren.item.gu.refine_dead";
    private static final String MENDED_KEY = "guzhenren.item.gu.mended";

    private GuConditionGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void anInjuredGuWearsNineUsesThenTurnsDying(GameTestHelper helper) {
        List<Component> inbox = new ArrayList<>();
        ServerPlayer player = holder(helper, inbox, Rank.ONE);
        ItemStack boar = hold(player, ModItems.FLOWER_BOAR_GU_1.get(), 14);
        GuHealth.set(boar, Stage.INJURED);

        charge(helper, player, boar);
        assertTrue(player.hasEffect(ModEffects.FLOWER_BOAR_GU), "the injured Gu's use did not land");
        assertEquals(8, usesLeft(boar), "a drive did not wear a use");

        TendedGuItem gu = (TendedGuItem) boar.getItem();
        for (int i = 0; i < 7; i++) assertFalse(TendedGuAccess.wearUse(gu, player, boar), "injured use " + i);
        assertEquals(Stage.INJURED, GuHealth.getStage(boar), "turned dying early");
        assertFalse(TendedGuAccess.wearUse(gu, player, boar), "the ninth use killed an injured Gu");
        assertEquals(Stage.DYING, GuHealth.getStage(boar), "the ninth use did not turn it dying");
        assertEquals(3, usesLeft(boar), "dying starts with three uses");
        assertEquals(1, messages(inbox, DYING_KEY).size(), "no dying notice");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aDyingGuDiesOnItsThirdUse(GameTestHelper helper) {
        List<Component> inbox = new ArrayList<>();
        ServerPlayer player = holder(helper, inbox, Rank.ONE);
        ItemStack boar = hold(player, ModItems.FLOWER_BOAR_GU_1.get(), 14);
        GuHealth.set(boar, Stage.DYING);
        TendedGuItem gu = (TendedGuItem) boar.getItem();

        assertFalse(TendedGuAccess.wearUse(gu, player, boar), "first dying use killed it");
        assertFalse(TendedGuAccess.wearUse(gu, player, boar), "second dying use killed it");
        assertTrue(TendedGuAccess.wearUse(gu, player, boar), "the third dying use did not kill it");
        assertEquals(1, messages(inbox, WOUND_DEAD_KEY).size(), "no wound death notice");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void creativeWearsDyingUsesButNeverDies(GameTestHelper helper) {
        ServerPlayer player = holder(helper, null, Rank.ONE);
        player.getAbilities().instabuild = true;
        ItemStack boar = hold(player, ModItems.FLOWER_BOAR_GU_1.get(), 14);
        GuHealth.set(boar, Stage.DYING);
        TendedGuItem gu = (TendedGuItem) boar.getItem();

        for (int i = 0; i < 4; i++) assertFalse(TendedGuAccess.wearUse(gu, player, boar), "creative use " + i);
        assertEquals(Stage.DYING, GuHealth.getStage(boar));
        assertEquals(0, usesLeft(boar), "creative uses were not worn");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void partnerBillsWearAndBarlessGuNever(GameTestHelper helper) {
        ServerPlayer player = holder(helper, null, Rank.THREE);
        ItemStack amplifier = hold(player, ModItems.AMPLIFY_EFFECT_GU_3.get(), 20);
        GuHealth.set(amplifier, Stage.INJURED);
        ItemStack elder = refined(ModItems.PRIMEVAL_ELDER_GU_1.get(), 0);
        GuHealth.set(elder, Stage.INJURED);

        assertTrue(((TendedGuItem) amplifier.getItem()).billAsPartner(player, amplifier), "the partner bill failed");
        assertEquals(8, usesLeft(amplifier), "a partner bill did not wear a use");
        assertFalse(TendedGuAccess.wearUse((TendedGuItem) elder.getItem(), player, elder), "a barless Gu died");
        assertEquals(9, usesLeft(elder), "a Gu without a hunger bar wore a use");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void refineFailureWorsensOneStageByChance(GameTestHelper helper) {
        List<Component> inbox = new ArrayList<>();
        ServerPlayer player = holder(helper, inbox, Rank.ONE);
        Item boar = ModItems.FLOWER_BOAR_GU_1.get();

        assertEquals(Stage.INJURED, rollFrom(player, boar, Stage.NORMAL, 0.19F), "20% did not injure");
        assertEquals(Stage.NORMAL, rollFrom(player, boar, Stage.NORMAL, 0.2F), "past 20% injured");
        assertEquals(Stage.DYING, rollFrom(player, boar, Stage.INJURED, 0.49F), "50% did not worsen");
        assertEquals(Stage.INJURED, rollFrom(player, boar, Stage.INJURED, 0.5F), "past 50% worsened");
        ItemStack dying = refined(boar, 14);
        GuHealth.set(dying, Stage.DYING);
        assertFalse(GuHealth.rollRefineFailure(player, dying, fixed(0.8F)), "past 80% ruined a dying Gu");
        assertTrue(GuHealth.rollRefineFailure(player, dying, fixed(0.79F)), "80% did not ruin a dying Gu");
        assertEquals(1, messages(inbox, REFINE_DEAD_KEY).size(), "no ruin notice");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aVitalGuMendsOneStageForAFullHealthOfHealing(GameTestHelper helper) {
        List<Component> inbox = new ArrayList<>();
        ServerPlayer player = holder(helper, inbox, Rank.ONE);
        ItemStack vital = refined(ModItems.WHITE_BOAR_GU_1.get(), 20);
        GuHealth.set(vital, Stage.DYING);
        VitalGuState.bind(vital, player, ApertureData.PRIMARY);
        PlayerData.get(player).setApertureStorage(
                ApertureStorage.DEFAULT.withVital(ApertureData.PRIMARY, vital.copy()));
        ApertureStorageService.adoptVital(player, ApertureData.PRIMARY, vital);
        float full = player.getMaxHealth();

        player.setHealth(full);
        player.heal(4.0F);
        assertEquals(0.0D, stored(player).mended(), 1.0E-4D);

        player.setHealth(1.0F);
        player.heal(full);
        assertEquals(Stage.DYING, stored(player).stage(), "mended before a full health of healing");
        player.setHealth(1.0F);
        player.heal(5.0F);
        assertEquals(Stage.INJURED, stored(player).stage(), "a full health of healing did not mend a stage");
        assertEquals(4.0D, stored(player).mended(), 1.0E-4D);
        assertEquals(6.0D, player.getHealth(), 1.0E-4D);
        assertEquals(1, messages(inbox, MENDED_KEY).size(), "no mend notice");
        helper.succeed();
    }

    private static ServerPlayer holder(GameTestHelper helper, List<Component> inbox, Rank rank) {
        ServerPlayer player = GameTestPlayers.survival(helper, "gzr-wound-mock").inbox(inbox).at(STAND);
        return GameTestGu.awakened(player, rank);
    }

    private static int usesLeft(ItemStack stack) {
        GuCondition condition = GuHealth.get(stack);
        return condition == null ? -1 : condition.usesLeft();
    }

    private static Stage rollFrom(ServerPlayer player, Item item, Stage from, float roll) {
        ItemStack stack = refined(item, 14);
        GuHealth.set(stack, from);
        assertFalse(GuHealth.rollRefineFailure(player, stack, fixed(roll)), from + " died at " + roll);
        return GuHealth.getStage(stack);
    }

    private static GuCondition stored(ServerPlayer player) {
        GuCondition condition = GuHealth.get(ApertureStorageService.getVital(player, ApertureData.PRIMARY));
        assertTrue(condition != null, "the stored vital lost its wound record");
        return condition;
    }

    private static RandomSource fixed(float roll) {
        return new LegacyRandomSource(0L) {
            @Override
            public float nextFloat() { return roll; }
        };
    }
}
