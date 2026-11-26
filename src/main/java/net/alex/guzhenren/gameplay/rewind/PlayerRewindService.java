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
 * 人如故[Man As Before Gu]的玩家身体快照记录与回溯还原服务。
 * <ul>
 *   <li>{@link #restore} 直接写回原版生命值、饥饿度、饱和度、氧气、着火时间并整体替换药水效果列表，魂魄、脑海与临时僵尸
 *       化剩余时间则经各自领域服务写回（随后心跳自动重投状态栏标记并重跑致死检查）。</li>
 *   <li>⚠ {@link #record} 严格以环形队列末尾的时间戳经 {@link RewindClock#isDue} 判断是否到点，不依赖外层调用频率，因
 *       此玩家重登或同时携带多只人如故也不会重复记录快照。</li>
 * </ul>
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
        BodyService.restoreTemporaryZombie(player, snapshot.temporaryZombieTicksLeft());
    }
}
