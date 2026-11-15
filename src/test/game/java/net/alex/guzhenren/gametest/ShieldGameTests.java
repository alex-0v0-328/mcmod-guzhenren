package net.alex.guzhenren.gametest;

import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.body.ShieldData;
import net.alex.guzhenren.gameplay.body.ShieldLayer;
import net.alex.guzhenren.gameplay.body.ShieldService;
import net.alex.guzhenren.gameplay.body.ShieldType;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.GameTestPlayers.CENTER;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertFalse;
import static net.alex.guzhenren.gametest.UnitAsserts.assertNotNull;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 在线玩家护盾测试。
 * <ul>
 *   <li>各层从最新升起的先耗。</li>
 *   <li>整击被护盾吃下按格挡处理：不掉血、开始受击无敌帧、击退近战攻击者、霸体原地不动。</li>
 *   <li>破盾一击不破坏无敌帧伤害记账，帧内后续一击只付超出破盾一击的部分。</li>
 * </ul>
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ShieldGameTests {
    private ShieldGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void theLayersSpendNewestFirst(GameTestHelper helper) {
        ServerPlayer player = awakened(helper);
        long endTick = player.level().getGameTime() + 30L * Ticks.SECOND;
        PlayerData.get(player).setShield(new ShieldData(List.of(
                new ShieldLayer(ShieldType.HEAVENLY_MUGWORT, 5.0F, 5.0F, endTick),
                new ShieldLayer(ShieldType.HEAVENLY_MUGWORT, 20.0F, 20.0F, endTick))));
        float health = player.getHealth();

        player.hurt(player.damageSources().generic(), 8.0F);
        assertEquals(health, player.getHealth());
        List<ShieldLayer> layers = ShieldService.get(player).layers();
        assertEquals(1L, layers.size());
        assertEquals(17.0D, layers.getFirst().points());
        assertEquals(20.0D, layers.getFirst().max());
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aHitTheShieldTakesWholePlaysAsABlock(GameTestHelper helper) {
        ServerPlayer player = awakened(helper);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, CENTER.add(2.0D, 0.0D, 0.0D));
        ShieldService.raise(player, ShieldType.HEAVENLY_MUGWORT, 180.0F, 30 * Ticks.SECOND);
        float health = player.getHealth();
        Vec3 motion = player.getDeltaMovement();
        DamageSource source = player.damageSources().mobAttack(zombie);
        float scaled = source.type().scaling().getScalingFunction()
                .scaleDamage(source, player, 6.0F, player.level().getDifficulty());

        assertFalse(player.hurt(source, 6.0F), "the blocked hit landed");
        assertEquals(health, player.getHealth());
        ShieldLayer shield = assertNotNull(ShieldService.find(player, ShieldType.HEAVENLY_MUGWORT),
                () -> "the shield broke");
        assertEquals(180.0D - scaled, shield.points(), 0.001D);
        assertEquals(20L, player.invulnerableTime);
        assertEquals(motion, player.getDeltaMovement());
        Vec3 away = zombie.position().subtract(player.position());
        assertTrue(zombie.getDeltaMovement().x * away.x + zombie.getDeltaMovement().z * away.z > 0.0D,
                "the attacker was not knocked away");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void insideTheFramesAHitAfterTheBreakPaysOnlyItsExcess(GameTestHelper helper) {
        ServerPlayer player = awakened(helper);
        ShieldService.raise(player, ShieldType.HEAVENLY_MUGWORT, 4.0F, 30 * Ticks.SECOND);
        float health = player.getHealth();

        player.hurt(player.damageSources().generic(), 10.0F);
        assertFalse(ShieldService.isUp(player), "the shield outlived the breaking hit");
        assertEquals(health - 6.0F, player.getHealth(), 0.001D);

        player.hurt(player.damageSources().generic(), 10.0F);
        assertEquals(health - 6.0F, player.getHealth(), 0.001D);

        player.hurt(player.damageSources().generic(), 12.0F);
        assertEquals(health - 8.0F, player.getHealth(), 0.001D);
        helper.succeed();
    }

    private static ServerPlayer awakened(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survival(helper, "gzr-shield-gametest").pastSpawnInvulnerability()
                .onGround();
        ApertureService.awaken(player, 80);
        ApertureService.setRank(player, Rank.THREE);
        player.setHealth(player.getMaxHealth());
        return player;
    }
}
