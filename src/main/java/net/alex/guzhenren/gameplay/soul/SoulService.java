package net.alex.guzhenren.gameplay.soul;

import com.google.common.math.LongMath;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * 魂魄唯一的运行时写入者，静态服务。
 * <ul>
 *   <li>{@link SoulData} 构造器已钳值，本类多为直通，另持有生命周期所需的 {@code revive}、{@code refill}。</li>
 *   <li>⚠ 本类不杀人：清空魂魄只布下致死状态，由心跳末步 {@code checkLethalState} 发现；没有可搜的 kill 调用。</li>
 *   <li>⚠ 上限 0 时当前值亦为 0（一检兼顾），{@code revive} 须恢复 {@code DEFAULT_MAX_SOUL}，免重生即死。</li>
 *   <li>⚠ 胆识蛊[Guts Gu] 提高 {@code maxSoul}；别靠调低数值“修”炼化代价。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see SoulData
 * @since 1.0.0
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
