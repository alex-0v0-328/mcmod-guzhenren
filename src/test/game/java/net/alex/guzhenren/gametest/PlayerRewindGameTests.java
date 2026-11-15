package net.alex.guzhenren.gametest;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.mind.MindPoolType;
import net.alex.guzhenren.gameplay.mind.MindService;
import net.alex.guzhenren.gameplay.rewind.PlayerRewindService;
import net.alex.guzhenren.gameplay.rewind.PlayerSnapshot;
import net.alex.guzhenren.gameplay.rewind.RewindClock;
import net.alex.guzhenren.gameplay.soul.SoulService;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertFalse;
import static net.alex.guzhenren.gametest.UnitAsserts.assertNotNull;
import static net.alex.guzhenren.gametest.UnitAsserts.assertNull;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 人如故[Man As Before Gu]依赖的身体记忆测试。快照恢复生命、饱食度、效果、魂魄、念头；环形缓冲每间隔记一次，
 * 取一天前快照；死后新身体无过去。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PlayerRewindGameTests {

    private PlayerRewindGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void restorePutsTheBodyBack(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survival(helper, "gzr-rewind-body-mock").build();
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        SoulService.setCurrent(player, 80L);
        MindService.setCurrent(player, MindPoolType.THOUGHTS, 30L);
        PlayerSnapshot snapshot = PlayerSnapshot.capture(player, 0L);

        player.setHealth(4.0F);
        player.getFoodData().setFoodLevel(3);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600));
        SoulService.setCurrent(player, 10L);
        MindService.setCurrent(player, MindPoolType.THOUGHTS, 5L);
        PlayerRewindService.restore(player, snapshot);

        assertEquals(player.getMaxHealth(), player.getHealth(), 0.001);
        assertEquals(20, player.getFoodData().getFoodLevel(), "food level");
        assertFalse(player.hasEffect(MobEffects.MOVEMENT_SPEED), "an effect gained since is gone");
        assertEquals(80L, SoulService.get(player).currentSoul(), "soul");
        assertEquals(30L, MindService.getCurrent(player, MindPoolType.THOUGHTS), "thoughts");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void recordIsGatedByTheIntervalAndPicksADayBack(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survival(helper, "gzr-rewind-ring-mock").build();

        assertTrue(PlayerRewindService.record(player, 0L), "the first snapshot is due");
        assertFalse(PlayerRewindService.record(player, 100L), "nothing is due inside the interval");
        assertTrue(PlayerRewindService.record(player, RewindClock.INTERVAL_TICKS), "the next interval is due");
        assertEquals(2, PlayerRewindService.get(player).snapshots().size());

        PlayerSnapshot dayBack = assertNotNull(PlayerRewindService.pick(player, RewindClock.DAY_TICKS),
                () -> "a day on, the ring serves a snapshot");
        assertEquals(0L, dayBack.takenAt());
        assertNull(PlayerRewindService.pick(player, RewindClock.MIN_AGE_TICKS - 1));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aDeathLeavesNoMemory(GameTestHelper helper) {
        ServerPlayer before = GameTestPlayers.survival(helper, "gzr-rewind-death-mock").build();
        ServerPlayer after = GameTestPlayers.survival(helper, "gzr-rewind-death-mock").build();
        PlayerRewindService.record(before, 0L);

        after.copyAttachmentsFrom(before, true);

        assertTrue(PlayerRewindService.get(after).isEmpty(), "the new body starts with no past");
        helper.succeed();
    }
}
