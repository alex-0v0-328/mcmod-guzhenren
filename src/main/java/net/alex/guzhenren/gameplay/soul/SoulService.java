package net.alex.guzhenren.gameplay.soul;

import com.google.common.math.LongMath;
import net.alex.guzhenren.registry.attachment.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * The only runtime writer of Soul [魂魄], the one pool whose bottom is lethal. Static service; the compact ctor
 * of {@link SoulData} already clamps current to {@code [0, max]}, so this service is mostly a
 * pass-through -- but it owns the {@code revive} and {@code refill} shapes the lifecycle needs.
 *
 * <p>⚠ Nothing here KILLS: emptying the soul only sets up a lethal state that the heartbeat's last step
 * ({@code checkLethalState}) notices -- there is no "kill" call to search for. ⚠ A cap of 0 also lands
 * current at 0 (one check catches both), so {@code revive} must restore {@code DEFAULT_MAX_SOUL} when
 * the cap itself was 0 -- a respawn may never hand back a value the lethal check would fire on. ⚠
 * Guts Gu [胆识蛊] raises {@code maxSoul}; do not "fix" the refining cost by softening the numbers.
 *
 * @author Alex
 * @version 1.0.0
 * @see SoulData
 * @since 1.0.0
 */

public final class SoulService {

    private SoulService() {}

    public static @NotNull SoulData get(@NotNull Player player) { return player.getData(ModAttachments.SOUL); }

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

    public static void refill(@NotNull ServerPlayer player) { store(player, get(player).refilled()); }

    public static void revive(@NotNull ServerPlayer player) { store(player, get(player).revived()); }

    private static void store(ServerPlayer player, SoulData data) { player.setData(ModAttachments.SOUL, data); }

    public static boolean consume(@NotNull ServerPlayer player, long amount) {
        if (amount <= 0L) return true;
        SoulData soul = get(player);
        if (soul.currentSoul() < amount) return false;
        setCurrent(player, soul.currentSoul() - amount);
        return true;
    }
}
