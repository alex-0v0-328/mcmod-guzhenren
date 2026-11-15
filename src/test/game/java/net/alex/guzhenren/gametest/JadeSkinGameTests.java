package net.alex.guzhenren.gametest;

import java.util.List;
import java.util.Objects;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.display.InfoModel;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.body.BodyData;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.Bone;
import net.alex.guzhenren.gameplay.body.Flesh;
import net.alex.guzhenren.gameplay.body.Skin;
import net.alex.guzhenren.gameplay.body.SkinService;
import net.alex.guzhenren.gameplay.body.Tendon;
import net.alex.guzhenren.item.GuItem;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.mortal.behavior.SkinBehavior;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.alex.guzhenren.lifecycle.Heartbeat;
import net.alex.guzhenren.lifecycle.PlayerDataEvents;
import net.alex.guzhenren.lifecycle.PlayerDataService;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.GameTestGu.click;
import static net.alex.guzhenren.gametest.GameTestGu.hold;
import static net.alex.guzhenren.gametest.GameTestGu.hunger;
import static net.alex.guzhenren.gametest.GameTestPlayers.CENTER;
import static net.alex.guzhenren.gametest.GameTestPlayers.ground;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertFalse;
import static net.alex.guzhenren.gametest.UnitAsserts.assertNotNull;
import static net.alex.guzhenren.gametest.UnitAsserts.assertNull;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 活玩家的玉皮：穿上并投射效果；到期脱落（心跳亦然）；真实攻击固定减伤，整击抵消则取消该击；穿皮时的门槛；重生脱落，末
 * 地返回不脱；死亡重置永久部位。生效门槛为该蛊 {@code SkinBehavior}，按右键方式直接询问；永久皮、筋、骨归
 * {@code BodyTemperingGameTests}。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class JadeSkinGameTests {
    private JadeSkinGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void wearingWritesTheSkinAndProjectsItsEffect(GameTestHelper helper) {
        ServerPlayer player = survival(helper);

        BodyService.wearSkin(player, Skin.JADE_SKIN, 60 * Ticks.SECOND);
        SkinService.tickSkin(player);

        assertEquals(Skin.JADE_SKIN, BodyService.getSkin(player));
        assertEquals(60L * Ticks.SECOND, BodyService.getSkinTicksLeft(player));
        MobEffectInstance effect = player.getEffect(ModEffects.JADE_SKIN_GU);
        assertNotNull(effect, () -> "jade skin effect not projected");
        assertEquals(60 * Ticks.SECOND, effect.getDuration());
        assertFalse(player.hasEffect(ModEffects.WHITE_JADE_GU));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aSkinThatRanOutIsShedAndItsEffectRemoved(GameTestHelper helper) {
        ServerPlayer player = survival(helper);
        BodyService.wearSkin(player, Skin.WHITE_JADE_SKIN, 60 * Ticks.SECOND);
        SkinService.tickSkin(player);
        assertTrue(player.hasEffect(ModEffects.WHITE_JADE_GU));

        BodyService.wearSkin(player, Skin.WHITE_JADE_SKIN, 0);
        SkinService.tickSkin(player);

        assertEquals(Skin.NATIVE, BodyService.getSkin(player));
        assertEquals(BodyData.UNTRACKED, BodyService.get(player).skinEndTick());
        assertFalse(player.hasEffect(ModEffects.WHITE_JADE_GU));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void milkCannotStrandTheSkinTheHeartbeatReappliesIt(GameTestHelper helper) {
        ServerPlayer player = survival(helper);
        BodyService.wearSkin(player, Skin.JADE_SKIN, 30 * Ticks.SECOND);
        SkinService.tickSkin(player);

        player.removeAllEffects();
        assertFalse(player.hasEffect(ModEffects.JADE_SKIN_GU));
        SkinService.tickSkin(player);

        assertEquals(Skin.JADE_SKIN, BodyService.getSkin(player));
        assertTrue(player.hasEffect(ModEffects.JADE_SKIN_GU));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aHitLosesTheFlatReductionAndBypassingDamagePasses(GameTestHelper helper) {
        ServerPlayer player = survival(helper);
        DamageSource generic = player.damageSources().generic();
        DamageSource kill = player.damageSources().genericKill();
        DamageSource starve = player.damageSources().starve();

        assertEquals(10.0F, SkinService.reduceDamage(player, generic, 10.0F));
        BodyService.wearSkin(player, Skin.WHITE_JADE_SKIN, 60 * Ticks.SECOND);
        assertEquals(6.0F, SkinService.reduceDamage(player, generic, 10.0F));
        assertEquals(0.0F, SkinService.reduceDamage(player, generic, 3.0F));
        assertEquals(10.0F, SkinService.reduceDamage(player, kill, 10.0F));
        assertEquals(10.0F, SkinService.reduceDamage(player, starve, 10.0F));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aRealHitOnAWhiteJadeSkinCostsFourLess(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survival(helper, "gzr-jade-skin-gametest")
                .pastSpawnInvulnerability().at(CENTER);
        float before = player.getHealth();
        BodyService.wearSkin(player, Skin.WHITE_JADE_SKIN, 60 * Ticks.SECOND);

        assertTrue(player.hurt(player.damageSources().generic(), 8.0F), "the hit was refused");

        assertEquals(before - 4.0F, player.getHealth(), 0.001D);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void eitherGuIsRefusedWhileAnySkinIsWorn(GameTestHelper helper) {
        ServerPlayer player = survival(helper);
        MortalGuItem jade = (MortalGuItem) ModItems.JADE_SKIN_GU_1.get();
        MortalGuItem white = (MortalGuItem) ModItems.WHITE_JADE_GU_2.get();

        assertNull(payoutGate(jade, player));
        assertNull(payoutGate(white, player));

        BodyService.wearSkin(player, Skin.JADE_SKIN, 60 * Ticks.SECOND);
        GuItem.Refusal jadeRefusal = payoutGate(jade, player);
        GuItem.Refusal whiteRefusal = payoutGate(white, player);
        assertNotNull(jadeRefusal, () -> "the jade skin Gu stacked on a worn skin");
        assertNotNull(whiteRefusal, () -> "the white jade Gu replaced a worn skin");
        assertEquals("guzhenren.item.failed.tempering_taken", jadeRefusal.key());
        assertEquals("guzhenren.item.failed.tempering_taken", whiteRefusal.key());
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void respawnShedsTheSkin(GameTestHelper helper) {
        ServerPlayer player = survival(helper);
        BodyService.wearSkin(player, Skin.WHITE_JADE_SKIN, 60 * Ticks.SECOND);

        BodyService.revive(player);
        SkinService.tickSkin(player);

        assertEquals(Skin.NATIVE, BodyService.getSkin(player));
        assertFalse(player.hasEffect(ModEffects.WHITE_JADE_GU));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theTwoGuWearTheirOwnSkinForTheirOwnTime(GameTestHelper helper) {
        SkinBehavior jade = skinOf((MortalGuItem) ModItems.JADE_SKIN_GU_1.get());
        SkinBehavior white = skinOf((MortalGuItem) ModItems.WHITE_JADE_GU_2.get());

        assertEquals(Skin.JADE_SKIN, jade.getGrade());
        assertEquals(60 * Ticks.SECOND, jade.getDurationTicks());
        assertEquals(Skin.WHITE_JADE_SKIN, white.getGrade());
        assertEquals(90 * Ticks.SECOND, white.getDurationTicks());
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theHeartbeatShedsARunOutJadeSkin(GameTestHelper helper) {
        ServerPlayer player = survival(helper);
        BodyService.wearSkin(player, Skin.JADE_SKIN, 0);
        SkinService.tickSkin(player);
        player.tickCount = Heartbeat.HEARTBEAT_TICKS;

        Heartbeat.beat(player);

        assertEquals(Skin.NATIVE, BodyService.getSkin(player));
        assertFalse(player.hasEffect(ModEffects.JADE_SKIN_GU));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aRunOutSkinTurnsNothingBeforeTheHeartbeatShedsIt(GameTestHelper helper) {
        ServerPlayer player = survival(helper);
        BodyService.wearSkin(player, Skin.WHITE_JADE_SKIN, 0);

        assertEquals(10.0F, SkinService.reduceDamage(player, player.damageSources().generic(), 10.0F));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aHitTurnedWholeIsCancelledWithoutKnockbackOrInvulnerability(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survival(helper, "gzr-jade-skin-gametest")
                .pastSpawnInvulnerability().at(CENTER);
        float before = player.getHealth();
        BodyService.wearSkin(player, Skin.WHITE_JADE_SKIN, 60 * Ticks.SECOND);

        assertFalse(player.hurt(player.damageSources().generic(), 3.0F), "a hit turned whole still landed");

        assertEquals(before, player.getHealth(), 0.001D);
        assertEquals(0, player.invulnerableTime);
        assertTrue(player.hurt(player.damageSources().generic(), 5.0F), "a hit the skin only dented was refused");
        assertEquals(before - 1.0F, player.getHealth(), 0.001D);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theEndReturnIsNoRespawnAndKeepsTheSkin(GameTestHelper helper) {
        ServerPlayer player = survival(helper);
        BodyService.wearSkin(player, Skin.JADE_SKIN, 60 * Ticks.SECOND);

        PlayerDataEvents.onRespawn(new PlayerEvent.PlayerRespawnEvent(player, true));
        assertEquals(Skin.JADE_SKIN, BodyService.getSkin(player));

        PlayerDataEvents.onRespawn(new PlayerEvent.PlayerRespawnEvent(player, false));
        assertEquals(Skin.NATIVE, BodyService.getSkin(player));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void onlyADeathWithoutKeepInventoryResetsThePermanentParts(GameTestHelper helper) {
        ground(helper);
        ServerPlayer from = GameTestPlayers.survival(helper, "gzr-jade-skin-gametest").at(CENTER);
        ServerPlayer to = GameTestPlayers.survival(helper, "gzr-jade-skin-gametest").at(CENTER);
        BodyService.wearSkin(to, Skin.IRON_SKIN_2, 0);
        BodyService.temperBone(to, Bone.IRON_BONE_3);

        PlayerDataService.onClone(from, to, true, true);
        assertEquals(Skin.IRON_SKIN_2, BodyService.getSkin(to));
        assertEquals(Bone.IRON_BONE_3, BodyService.getBone(to));

        PlayerDataService.onClone(from, to, true, false);
        assertEquals(Skin.NATIVE, BodyService.getSkin(to));
        assertEquals(Bone.NATIVE, BodyService.getBone(to));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aRefinedJadeSkinGuPaysEssenceAndHungerAndWearsTheSkin(GameTestHelper helper) {
        ServerPlayer player = survival(helper);
        GameTestGu.awakened(player, Rank.ONE);
        TendedGuItem item = (TendedGuItem) ModItems.JADE_SKIN_GU_1.get();
        ItemStack stack = hold(player, item, 14);
        long essence = ApertureEssenceService.getSpendable(player);

        click(helper, player, stack);

        assertEquals(Skin.JADE_SKIN, BodyService.getSkin(player));
        assertEquals(essence - 40L, ApertureEssenceService.getSpendable(player));
        assertEquals(14 - 3, hunger(stack));
        assertTrue(player.getCooldowns().isOnCooldown(item), "no item cooldown after the use");

        click(helper, player, stack);
        assertEquals(essence - 40L, ApertureEssenceService.getSpendable(player), "a refused use still paid");
        assertEquals(14 - 3, hunger(stack), "a refused use still ate");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void ironBonesSurviveJoinAndCloneAndFallWithTheReset(GameTestHelper helper) {
        ground(helper);
        ServerPlayer from = GameTestPlayers.survival(helper, "gzr-jade-skin-gametest").at(CENTER);
        ServerPlayer to = GameTestPlayers.survival(helper, "gzr-jade-skin-gametest").at(CENTER);
        double baseHealth = to.getMaxHealth();
        double baseKnockback = to.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
        BodyService.temperBone(to, Bone.IRON_BONE_4);

        PlayerDataService.onJoin(to);
        assertEquals(baseHealth + 10.0D, to.getMaxHealth(), 0.001D);
        assertEquals(baseKnockback + 0.6D, to.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.001D);

        PlayerDataService.onClone(from, to, true, true);
        assertEquals(baseHealth + 10.0D, to.getMaxHealth(), 0.001D);
        assertEquals(baseKnockback + 0.6D, to.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.001D);

        PlayerDataService.resetAll(to);
        assertEquals(baseHealth, to.getMaxHealth(), 0.001D);
        assertEquals(baseKnockback, to.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.001D);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theBodyRowsNameTheTendonsBonesSkinAndFleshInOrder(GameTestHelper helper) {
        ServerPlayer player = survival(helper);
        BodyService.wearSkin(player, Skin.IRON_SKIN_3, 0);
        BodyService.temperTendon(player, Tendon.STEEL_TENDON_3);
        BodyService.temperBone(player, Bone.IRON_BONE_3);

        List<InfoModel.Entry> entries = InfoModel.buildBodyRows(player).stream().map(InfoModel.Row::entry).toList();
        int tendon = entries.indexOf(new InfoModel.TendonRow(Tendon.STEEL_TENDON_3));

        assertTrue(tendon >= 0, "tendon row missing");
        assertEquals(List.of(new InfoModel.TendonRow(Tendon.STEEL_TENDON_3), new InfoModel.BoneRow(Bone.IRON_BONE_3),
                        new InfoModel.SkinRow(Skin.IRON_SKIN_3), new InfoModel.FleshRow(Flesh.NATIVE)),
                entries.subList(tendon, tendon + 4));
        helper.succeed();
    }

    private static GuItem.Refusal payoutGate(MortalGuItem gu, Player player) {
        return skinOf(gu).checkEffect(gu, player, new ItemStack(gu));
    }

    private static SkinBehavior skinOf(MortalGuItem gu) {
        return Objects.requireNonNull(gu.getBehavior(SkinBehavior.class));
    }

    private static ServerPlayer survival(GameTestHelper helper) {
        return GameTestPlayers.survival(helper, "gzr-jade-skin-gametest").onGround();
    }
}
