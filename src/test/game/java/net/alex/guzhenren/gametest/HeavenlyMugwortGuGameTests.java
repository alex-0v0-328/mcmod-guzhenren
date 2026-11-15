package net.alex.guzhenren.gametest;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.body.ShieldData;
import net.alex.guzhenren.gameplay.body.ShieldLayer;
import net.alex.guzhenren.gameplay.body.ShieldService;
import net.alex.guzhenren.gameplay.body.ShieldType;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.GameTestGu.click;
import static net.alex.guzhenren.gametest.GameTestGu.hold;
import static net.alex.guzhenren.gametest.GameTestGu.hunger;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertFalse;
import static net.alex.guzhenren.gametest.UnitAsserts.assertNotNull;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 活玩家的天蓬蛊[Heavenly Mugwort Gu]：催动一次升 180 点护盾 30 秒并附效果，计 2,560 真元[Essence]、3 饱食[Hunger]；挡
 * 住的攻击不伤血；破盾一击放过余量，护甲只减余量，并结束效果；整击吸收仍触发无敌帧，帧内只扣超出已受伤害的部分，故岩浆
 * 每帧窗口扣一次而非每 tick；牛奶清不掉；到期护盾清除。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HeavenlyMugwortGuGameTests {
    private HeavenlyMugwortGuGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void oneUseRaisesAOneHundredEightyPointShield(GameTestHelper helper) {
        ServerPlayer player = awakened(helper);
        ItemStack gu = hold(player, ModItems.HEAVENLY_MUGWORT_GU_3.get(), 50);
        long essence = ApertureEssenceService.getSpendable(player);

        click(helper, player, gu);

        ShieldLayer shield = assertNotNull(ShieldService.find(player, ShieldType.HEAVENLY_MUGWORT),
                () -> "no shield raised");
        assertEquals(180.0D, shield.points());
        assertEquals(180.0D, shield.max());
        assertEquals(30L * Ticks.SECOND, ShieldService.getTicksLeft(player, ShieldType.HEAVENLY_MUGWORT));
        MobEffectInstance effect = assertNotNull(player.getEffect(ModEffects.HEAVENLY_MUGWORT_GU),
                () -> "shield effect missing");
        assertEquals(30 * Ticks.SECOND, effect.getDuration());
        assertEquals(essence - 2_560L, ApertureEssenceService.getSpendable(player));
        assertEquals(50 - 3, hunger(gu));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theShieldHoldsAHitThenBreaksAndPassesTheRest(GameTestHelper helper) {
        ServerPlayer player = awakened(helper);
        ShieldService.raise(player, ShieldType.HEAVENLY_MUGWORT, 16.0F, 30 * Ticks.SECOND);
        float health = player.getHealth();

        player.hurt(player.damageSources().generic(), 10.0F);
        assertEquals(health, player.getHealth());
        assertEquals(6.0D, points(player));

        player.invulnerableTime = 0;
        player.hurt(player.damageSources().generic(), 10.0F);
        assertEquals(health - 4.0F, player.getHealth(), 0.001D);
        assertFalse(ShieldService.isUp(player));
        assertFalse(player.hasEffect(ModEffects.HEAVENLY_MUGWORT_GU));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void armorWorksOnlyOnWhatTheShieldPasses(GameTestHelper helper) {
        ServerPlayer player = awakened(helper);
        player.getAttribute(Attributes.ARMOR).setBaseValue(8.0D);
        ShieldService.raise(player, ShieldType.HEAVENLY_MUGWORT, 16.0F, 30 * Ticks.SECOND);
        float health = player.getHealth();
        float expected = CombatRules.getDamageAfterAbsorb(player, 4.0F, player.damageSources().cactus(),
                player.getArmorValue(), (float) player.getAttributeValue(Attributes.ARMOR_TOUGHNESS));

        player.hurt(player.damageSources().cactus(), 20.0F);
        assertFalse(ShieldService.isUp(player));
        assertEquals(health - expected, player.getHealth(), 0.001D);
        assertTrue(expected < 4.0F, () -> "the armor reduced nothing");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aHitTheShieldTakesWholeStillStartsInvulnerabilityFrames(GameTestHelper helper) {
        ServerPlayer player = awakened(helper);
        ShieldService.raise(player, ShieldType.HEAVENLY_MUGWORT, 16.0F, 30 * Ticks.SECOND);

        for (int hit = 0; hit < 10; hit++) player.hurt(player.damageSources().lava(), 4.0F);
        assertEquals(12.0D, points(player));
        assertTrue(player.invulnerableTime > 10, () -> "no invulnerability frames after a shielded hit");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void insideInvulnerabilityFramesTheShieldPaysOnlyTheExcess(GameTestHelper helper) {
        ServerPlayer player = awakened(helper);
        player.hurt(player.damageSources().generic(), 5.0F);
        float health = player.getHealth();
        ShieldService.raise(player, ShieldType.HEAVENLY_MUGWORT, 16.0F, 30 * Ticks.SECOND);

        player.hurt(player.damageSources().generic(), 5.0F);
        assertEquals(16.0D, points(player));

        player.hurt(player.damageSources().generic(), 8.0F);
        assertEquals(13.0D, points(player));
        assertEquals(health, player.getHealth());
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void milkCannotStripTheShieldAndItsTimeEndsIt(GameTestHelper helper) {
        ServerPlayer player = awakened(helper);
        ShieldService.raise(player, ShieldType.HEAVENLY_MUGWORT, 16.0F, 30 * Ticks.SECOND);
        player.removeAllEffects();

        ShieldService.tickShield(player);
        assertTrue(player.hasEffect(ModEffects.HEAVENLY_MUGWORT_GU));

        ShieldService.raise(player, ShieldType.HEAVENLY_MUGWORT, 16.0F, 0);
        assertFalse(ShieldService.isUp(player));
        assertEquals(ShieldData.DEFAULT, ShieldService.get(player));
        assertFalse(player.hasEffect(ModEffects.HEAVENLY_MUGWORT_GU));
        helper.succeed();
    }

    private static double points(ServerPlayer player) {
        ShieldLayer layer = ShieldService.find(player, ShieldType.HEAVENLY_MUGWORT);
        return layer == null ? 0.0D : layer.points();
    }

    private static ServerPlayer awakened(GameTestHelper helper) {
        return GameTestGu.awakened(GameTestPlayers.survival(helper, "gzr-heavenly-mugwort-gametest")
                .pastSpawnInvulnerability().onGround(), Rank.THREE);
    }
}
