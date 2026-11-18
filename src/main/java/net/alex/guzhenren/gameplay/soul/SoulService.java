package net.alex.guzhenren.gameplay.soul;

import com.google.common.math.LongMath;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * 玩家魂魄状态的唯一运行时写入服务。
 * <ul>
 *   <li>{@link SoulData} 紧凑构造器已负责数值钳位，本类写入方法多为直通，另提供复活重置（{@link #revive}）、回
 *       满（{@link #refill}）与消耗扣减（{@link #consume}）。</li>
 *   <li>⚠ 本类不直接执行击杀：将魂魄扣至 0 仅使 {@link SoulData#isCollapsed()} 为真，实际致死由每秒心跳末步的
 *       致死检查统一判定并施加伤害。</li>
 *   <li>⚠ 上限为 0 时当前魂魄也必为 0（一并触发触底判定），因此 {@link #revive} 必须同时兜底恢复默认上限，避
 *       免玩家重生即死。</li>
 *   <li>⚠ 胆识蛊[Guts Gu]用于提高 {@code maxSoul}，魂魄不足或炼化代价过高时不得通过直接压低消耗数值来规避限
 *       制。</li>
 * </ul>
 */

public final class SoulService {

    private SoulService() {}

    public static @NotNull SoulData get(@NotNull Player player) { return PlayerData.get(player).getSoul(); }

    public static void setMax(@NotNull ServerPlayer player, long value) {
        store(player, get(player).withMaxSoul(value));
    }

    public static void addMax(@NotNull ServerPlayer player, long delta) {
        setMax(player, LongMath.saturatedAdd(get(player).maxSoul(), delta));
    }

    public static void setCurrent(@NotNull ServerPlayer player, long value) {
        store(player, get(player).withCurrentSoul(value));
    }

    public static void addCurrent(@NotNull ServerPlayer player, long delta) {
        setCurrent(player, LongMath.saturatedAdd(get(player).currentSoul(), delta));
    }

    public static void refill(@NotNull ServerPlayer player) { store(player, get(player).withFullSoul()); }

    public static void revive(@NotNull ServerPlayer player) { store(player, get(player).withRevived()); }

    private static void store(ServerPlayer player, SoulData data) {
        PlayerData.get(player).setSoul(data);
    }

    public static boolean consume(@NotNull ServerPlayer player, long amount) {
        if (amount <= 0L) return true;
        SoulData soul = get(player);
        if (soul.currentSoul() < amount) return false;
        setCurrent(player, soul.currentSoul() - amount);
        return true;
    }
}
