package net.alex.guzhenren.gametest;

import java.util.List;
import java.util.Objects;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.body.Appendage;
import net.alex.guzhenren.gameplay.body.BodyData;
import net.alex.guzhenren.gameplay.body.BodyPart;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.Bone;
import net.alex.guzhenren.gameplay.body.Flesh;
import net.alex.guzhenren.gameplay.body.Injury;
import net.alex.guzhenren.gameplay.body.Organ;
import net.alex.guzhenren.gameplay.body.Skin;
import net.alex.guzhenren.gameplay.body.SkinService;
import net.alex.guzhenren.gameplay.body.Tendon;
import net.alex.guzhenren.gameplay.path.strength.StrengthService;
import net.alex.guzhenren.item.GuItem;
import net.alex.guzhenren.item.gu.GuBehavior;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.mortal.behavior.SkinBehavior;
import net.alex.guzhenren.item.gu.mortal.behavior.TemperingBehavior;
import net.alex.guzhenren.item.gu.mortal.path.metal.BoneBehavior;
import net.alex.guzhenren.item.gu.mortal.path.metal.TendonBehavior;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.alex.guzhenren.registry.item.ModItemTags;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.GameTestGu.hold;
import static net.alex.guzhenren.gametest.GameTestGu.hunger;
import static net.alex.guzhenren.gametest.GameTestPlayers.command;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertFalse;
import static net.alex.guzhenren.gametest.UnitAsserts.assertNotNull;
import static net.alex.guzhenren.gametest.UnitAsserts.assertNull;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 活玩家的炼体：铜皮、铁皮永久生效，无计时无效果；门槛只放同家族更高转，其上拒玉皮；重生保留；钢筋、铁骨炼成即改承受、
 * 最大生命与击退抗性；灌注[Channel]一轮耗锭即永久炼成，创造模式无锭、饱食[Hunger]在下限亦可；{@code /gzr body} 按家族与
 * 转数列各部位，也写肢体与七窍伤势、附加部位。生效门槛为该蛊 {@code TemperingBehavior}，按右键方式直接询问。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BodyTemperingGameTests {
    private BodyTemperingGameTests() {}

    private static final int BAG_SLOT = 9;

    @GameTest(template = "empty9x9x9")
    public static void aPermanentSkinHasNoTimerAndProjectsNoEffect(GameTestHelper helper) {
        ServerPlayer player = survival(helper);

        BodyService.wearSkin(player, Skin.IRON_SKIN_2, 0);
        SkinService.tickSkin(player);

        assertEquals(Skin.IRON_SKIN_2, BodyService.getSkin(player));
        assertEquals(BodyData.UNTRACKED, BodyService.get(player).skinEndTick());
        assertFalse(BodyService.hasSkinRunOut(player));
        assertEquals(0L, BodyService.getSkinTicksLeft(player));
        assertFalse(player.hasEffect(ModEffects.JADE_SKIN_GU));
        assertFalse(player.hasEffect(ModEffects.WHITE_JADE_GU));
        assertEquals(7.0F, SkinService.reduceDamage(player, player.damageSources().generic(), 10.0F));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void onlyAHigherRankOfTheSameFamilyReplacesAWornSkin(GameTestHelper helper) {
        ServerPlayer player = survival(helper);
        MortalGuItem bronze1 = (MortalGuItem) ModItems.BRONZE_SKIN_GU_1.get();
        MortalGuItem bronze2 = (MortalGuItem) ModItems.BRONZE_SKIN_GU_2.get();
        MortalGuItem bronze4 = (MortalGuItem) ModItems.BRONZE_SKIN_GU_4.get();
        MortalGuItem iron3 = (MortalGuItem) ModItems.IRON_SKIN_GU_3.get();
        MortalGuItem jade = (MortalGuItem) ModItems.JADE_SKIN_GU_1.get();

        assertNull(payoutGate(bronze1, player));
        BodyService.wearSkin(player, Skin.BRONZE_SKIN_2, 0);

        assertNotNull(payoutGate(bronze1, player), () -> "a lower bronze skin replaced a higher one");
        assertNotNull(payoutGate(bronze2, player), () -> "the same bronze skin was worn twice");
        assertNull(payoutGate(bronze4, player));
        GuItem.Refusal crossFamily = payoutGate(iron3, player);
        assertNotNull(crossFamily, () -> "an iron skin replaced a bronze skin");
        assertEquals("guzhenren.item.failed.tempering_taken", crossFamily.key());
        assertNotNull(payoutGate(jade, player), () -> "a jade skin went over a bronze skin");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void respawnKeepsAPermanentSkinAndTheTemperedParts(GameTestHelper helper) {
        ServerPlayer player = survival(helper);
        BodyService.wearSkin(player, Skin.IRON_SKIN_4, 0);
        BodyService.temperTendon(player, Tendon.STEEL_TENDON_3);
        BodyService.temperBone(player, Bone.IRON_BONE_3);

        BodyService.revive(player);
        SkinService.tickSkin(player);

        assertEquals(Skin.IRON_SKIN_4, BodyService.getSkin(player));
        assertEquals(Tendon.STEEL_TENDON_3, BodyService.getTendon(player));
        assertEquals(Bone.IRON_BONE_3, BodyService.getBone(player));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void steelTendonsRaiseTheCapacityAndClimbByRank(GameTestHelper helper) {
        ServerPlayer player = survival(helper);
        MortalGuItem steel3 = (MortalGuItem) ModItems.STEEL_TENDON_GU_3.get();
        MortalGuItem steel4 = (MortalGuItem) ModItems.STEEL_TENDON_GU_4.get();
        int base = StrengthService.getCapacity(player);

        assertNull(payoutGate(steel3, player));
        BodyService.temperTendon(player, Tendon.STEEL_TENDON_3);
        assertEquals(base + Tendon.STEEL_TENDON_3.getCapacityBonus(), StrengthService.getCapacity(player));
        GuItem.Refusal again = payoutGate(steel3, player);
        assertNotNull(again, () -> "the same steel tendons were tempered twice");
        assertEquals("guzhenren.item.failed.tempering_taken", again.key());

        assertNull(payoutGate(steel4, player));
        BodyService.temperTendon(player, Tendon.STEEL_TENDON_4);
        assertEquals(base + Tendon.STEEL_TENDON_4.getCapacityBonus(), StrengthService.getCapacity(player));
        assertNotNull(payoutGate(steel3, player), () -> "lower steel tendons replaced higher ones");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void ironBonesMoveMaxHealthAndKnockbackResistanceAtOnce(GameTestHelper helper) {
        ServerPlayer player = survival(helper);
        MortalGuItem iron3 = (MortalGuItem) ModItems.IRON_BONE_GU_3.get();
        MortalGuItem iron4 = (MortalGuItem) ModItems.IRON_BONE_GU_4.get();
        double baseHealth = player.getMaxHealth();
        double baseKnockback = player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);

        assertNull(payoutGate(iron3, player));
        BodyService.temperBone(player, Bone.IRON_BONE_3);
        assertEquals(baseHealth + 4.0D, player.getMaxHealth(), 0.001D);
        assertEquals(baseKnockback + 0.3D, player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.001D);
        GuItem.Refusal again = payoutGate(iron3, player);
        assertNotNull(again, () -> "the same iron bones were tempered twice");
        assertEquals("guzhenren.item.failed.tempering_taken", again.key());

        assertNull(payoutGate(iron4, player));
        BodyService.temperBone(player, Bone.IRON_BONE_4);
        assertEquals(baseHealth + 10.0D, player.getMaxHealth(), 0.001D);
        assertEquals(baseKnockback + 0.6D, player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.001D);
        assertNotNull(payoutGate(iron3, player), () -> "rank-three iron bones replaced rank-four ones");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void eachTemperingGuNamesItsGradeAndTheJadeOnesAloneTakeATime(GameTestHelper helper) {
        assertEquals(Skin.BRONZE_SKIN_3, moduleOf(ModItems.BRONZE_SKIN_GU_3.get(), SkinBehavior.class).getGrade());
        assertEquals(Skin.BRONZE_SKIN_4, moduleOf(ModItems.BRONZE_SKIN_GU_4.get(), SkinBehavior.class).getGrade());
        assertEquals(Skin.IRON_SKIN_1, moduleOf(ModItems.IRON_SKIN_GU_1.get(), SkinBehavior.class).getGrade());
        assertEquals(Skin.IRON_SKIN_4, moduleOf(ModItems.IRON_SKIN_GU_4.get(), SkinBehavior.class).getGrade());
        assertEquals(0, moduleOf(ModItems.IRON_SKIN_GU_2.get(), SkinBehavior.class).getDurationTicks());
        assertEquals(60 * Ticks.SECOND, moduleOf(ModItems.JADE_SKIN_GU_1.get(), SkinBehavior.class).getDurationTicks());
        assertEquals(Tendon.STEEL_TENDON_4, moduleOf(ModItems.STEEL_TENDON_GU_4.get(), TendonBehavior.class).getGrade());
        assertEquals(Bone.IRON_BONE_4, moduleOf(ModItems.IRON_BONE_GU_4.get(), BoneBehavior.class).getGrade());
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theBodyCommandSetsEachPartByFamilyAndRank(GameTestHelper helper) {
        ServerPlayer player = survival(helper);

        command(player, "gzr body skin set bronze_skin 3");
        assertEquals(Skin.BRONZE_SKIN_3, BodyService.getSkin(player));
        command(player, "gzr body skin set iron_skin");
        assertEquals(Skin.IRON_SKIN_1, BodyService.getSkin(player));
        command(player, "gzr body skin set bronze_skin 5");
        assertEquals(Skin.IRON_SKIN_1, BodyService.getSkin(player), "rank five is out of range");
        command(player, "gzr body skin set jade_skin 5");
        assertEquals(Skin.JADE_SKIN, BodyService.getSkin(player));
        assertEquals(5L * Ticks.SECOND, BodyService.getSkinTicksLeft(player));
        assertTrue(player.hasEffect(ModEffects.JADE_SKIN_GU));
        command(player, "gzr body skin set white_jade_skin");
        assertEquals(Skin.WHITE_JADE_SKIN, BodyService.getSkin(player));
        assertEquals(90L * Ticks.SECOND, BodyService.getSkinTicksLeft(player));
        command(player, "gzr body skin set native");
        assertEquals(Skin.NATIVE, BodyService.getSkin(player));
        assertFalse(player.hasEffect(ModEffects.WHITE_JADE_GU));

        command(player, "gzr body tendon set steel_tendon");
        assertEquals(Tendon.STEEL_TENDON_3, BodyService.getTendon(player));
        command(player, "gzr body tendon set steel_tendon 4");
        assertEquals(Tendon.STEEL_TENDON_4, BodyService.getTendon(player));
        command(player, "gzr body bone set iron_bone 4");
        assertEquals(Bone.IRON_BONE_4, BodyService.getBone(player));
        command(player, "gzr body bone set iron_bone 3");
        assertEquals(Bone.IRON_BONE_3, BodyService.getBone(player), "a command may lower a grade");
        command(player, "gzr body bone set native");
        assertEquals(Bone.NATIVE, BodyService.getBone(player));
        assertEquals(Flesh.NATIVE, BodyService.get(player).flesh());
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theBodyCommandSetsInjuriesAndAppendages(GameTestHelper helper) {
        ServerPlayer player = survival(helper);

        command(player, "gzr body injury limb left_arm severe");
        command(player, "gzr body injury organ eyes crippled");
        assertEquals(Injury.SEVERE, BodyService.getAnatomy(player).getInjury(BodyPart.LEFT_ARM));
        assertEquals(Injury.CRIPPLED, BodyService.getAnatomy(player).getInjury(Organ.EYES));
        assertEquals(Injury.NONE, BodyService.getAnatomy(player).getInjury(BodyPart.RIGHT_ARM));
        command(player, "gzr body appendage add wings");
        assertTrue(BodyService.getAnatomy(player).hasAppendage(Appendage.WINGS));
        command(player, "gzr body injury heal");
        assertTrue(BodyService.getAnatomy(player).isUninjured());
        assertTrue(BodyService.getAnatomy(player).hasAppendage(Appendage.WINGS), "healing keeps appendages");
        command(player, "gzr body appendage remove wings");
        assertFalse(BodyService.getAnatomy(player).hasAppendage(Appendage.WINGS));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void oneChanneledRoundOnCopperTempersABronzeSkinAndStops(GameTestHelper helper) {
        ServerPlayer player = survival(helper);
        GameTestGu.awakened(player, Rank.FIVE);
        ItemStack gu = hold(player, ModItems.BRONZE_SKIN_GU_1.get(), 12);
        player.getInventory().setItem(BAG_SLOT, new ItemStack(Items.COPPER_INGOT, 8));

        gu.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        assertTrue(player.isUsingItem(), "the channel did not start");
        for (int remaining = player.getUseItemRemainingTicks(); remaining >= 1 && player.isUsingItem(); remaining--) {
            gu.onUseTick(helper.getLevel(), player, remaining);
        }

        assertEquals(Skin.BRONZE_SKIN_1, BodyService.getSkin(player));
        assertFalse(player.isUsingItem(), "the channel went on past a tempered skin");
        assertTrue(player.getInventory().getItem(BAG_SLOT).getCount() < 8, "the round ate no copper from the bag");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aCreativeChannelAtTheHungerFloorWithNoCopperStillTempers(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survival(helper, "gzr-tempering-gametest").creative(() -> true)
                .onGround();
        player.getAbilities().instabuild = true;
        GameTestGu.awakened(player, Rank.FIVE);
        ItemStack gu = hold(player, ModItems.BRONZE_SKIN_GU_1.get(), 2);

        gu.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        assertTrue(player.isUsingItem(), "creative was refused at the hunger floor");
        for (int remaining = player.getUseItemRemainingTicks(); remaining >= 1 && player.isUsingItem(); remaining--) {
            gu.onUseTick(helper.getLevel(), player, remaining);
        }

        assertEquals(Skin.BRONZE_SKIN_1, BodyService.getSkin(player));
        assertEquals(0, hunger(gu), "creative should still show the bar drain");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void thePermanentGuChannelOnIngotsAndTheJadeOnesStayInstant(GameTestHelper helper) {
        for (var gu : List.of(ModItems.BRONZE_SKIN_GU_1, ModItems.BRONZE_SKIN_GU_4, ModItems.IRON_SKIN_GU_4,
                ModItems.STEEL_TENDON_GU_3, ModItems.IRON_BONE_GU_4)) {
            assertEquals(0L, ((TendedGuItem) gu.get()).getUseEssence(), () -> gu.getId() + " is not channeled");
        }
        assertEquals(40L, ((TendedGuItem) ModItems.JADE_SKIN_GU_1.get()).getUseEssence());
        assertTrue(new ItemStack(Items.COPPER_INGOT).is(ModItemTags.BRONZE_SKIN_FEED));
        assertFalse(new ItemStack(Items.COPPER_BLOCK).is(ModItemTags.BRONZE_SKIN_FEED));
        assertTrue(new ItemStack(Items.IRON_INGOT).is(ModItemTags.IRON_SKIN_FEED));
        assertTrue(new ItemStack(Items.IRON_INGOT).is(ModItemTags.IRON_BONE_FEED));
        assertFalse(new ItemStack(Items.BONE).is(ModItemTags.IRON_BONE_FEED));
        assertTrue(new ItemStack(ModItems.STEEL_INGOT.get()).is(ModItemTags.STEEL_TENDON_FEED));
        assertFalse(new ItemStack(Items.IRON_INGOT).is(ModItemTags.STEEL_TENDON_FEED));
        helper.succeed();
    }

    private static GuItem.Refusal payoutGate(MortalGuItem gu, Player player) {
        return moduleOf(gu, TemperingBehavior.class).checkEffect(gu, player, new ItemStack(gu));
    }

    private static <M extends GuBehavior> M moduleOf(Item gu, Class<M> type) {
        return Objects.requireNonNull(((MortalGuItem) gu).getBehavior(type));
    }

    private static ServerPlayer survival(GameTestHelper helper) {
        return GameTestPlayers.survival(helper, "gzr-tempering-gametest").onGround();
    }
}
