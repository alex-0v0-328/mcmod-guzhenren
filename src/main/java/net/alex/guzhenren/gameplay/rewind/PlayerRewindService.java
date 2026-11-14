package net.alex.guzhenren.gameplay.rewind;

import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.gameplay.mind.MindService;
import net.alex.guzhenren.gameplay.soul.SoulService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 人如故[Man As Before Gu]的身体记忆服务。还原直接写原版身体（生命、饥饿值、空气、着火，效果列表整体替换），模组数值经
 * 各自服务写入，派生状态按所有者方式刷新；随后心跳重投临时僵尸化效果并重跑致死检查。⚠ {@code record} 按环末时间戳由
 * {@link RewindClock#isDue} 把关，不靠调用节奏，重登或两个持有者都不会重复快照。
 *
 * @author Alex
 * @version 1.0.0
 * @see PlayerSnapshot
 * @see PlayerRewindData
 * @since 1.0.0
 */

public final class PlayerRewindService {

    private PlayerRewindService() {}

    public static @NotNull PlayerRewindData get(@NotNull Player player) { return PlayerData.get(player).getRewind(); }

    public static boolean record(@NotNull ServerPlayer player, long now) {
        PlayerRewindData data = get(player);
        if (!RewindClock.isDue(data.getLastTakenAt(), now)) return false;

        PlayerData.get(player).setRewind(data.with(PlayerSnapshot.capture(player, now)));
        return true;
    }

    public static @Nullable PlayerSnapshot pick(@NotNull Player player, long now) { return get(player).pick(now); }

    public static void restore(@NotNull ServerPlayer player, @NotNull PlayerSnapshot snapshot) {
        player.setHealth(snapshot.health());
        player.getFoodData().setFoodLevel(snapshot.foodLevel());
        player.getFoodData().setSaturation(snapshot.saturation());
        player.setAirSupply(snapshot.airSupply());
        player.setRemainingFireTicks(snapshot.fireTicks());
        player.removeAllEffects();
        for (MobEffectInstance effect : snapshot.effects()) player.addEffect(new MobEffectInstance(effect));

        SoulService.setCurrent(player, snapshot.soul());
        MindService.restoreThoughts(player, snapshot.thoughts(), snapshot.taggedThoughts());
        BodyService.restoreTempZombie(player, snapshot.tempZombieTicksLeft());
    }
}
