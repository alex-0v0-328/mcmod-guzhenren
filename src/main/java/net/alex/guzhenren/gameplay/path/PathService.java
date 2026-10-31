package net.alex.guzhenren.gameplay.path;

import com.google.common.math.LongMath;
import java.util.EnumSet;
import java.util.Set;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * The only runtime writer of Path [流派] progress: attainment and the bare Dao marks [道痕].
 *
 * <p>Static service over the {@code path_data} attachment; reads take {@link Player}, writes take
 * {@link ServerPlayer}. Bare marks are whole marks [条] and strands [丝] on the body [肉体] that do nothing; the
 * command writes them, one counter at a time, and nothing else does yet. Marks that come with an effect -- a race,
 * a physique, a Gu -- are {@link PathEffectService}'s, never bare. {@link #getMarks} is a path's whole total, both
 * halves summed per counter on every read. ⚠ {@code attainment} is a GRADE, so a race grant that shifts it up is
 * never "set" -- it MOVES, and the revoke shifts back down; nothing can tell a granted master from an earned one.
 *
 * @author Alex
 * @version 1.0.0
 * @see PathData
 * @see PathEntry
 * @since 1.0.0
 */

public final class PathService {

    private PathService() {}

    public static @NotNull PathData get(@NotNull Player player) { return PlayerData.get(player).getPath(); }

    public static @NotNull PathEntry getEntry(@NotNull Player player, @NotNull GuPath path) {
        return get(player).get(path);
    }

    public static @NotNull GuAttainment getAttainment(@NotNull Player player, @NotNull GuPath path) {
        return getEntry(player, path).attainment();
    }

    public static @NotNull DaoMarks getBareMarks(@NotNull Player player, @NotNull GuPath path) {
        return getEntry(player, path).marks();
    }

    public static @NotNull DaoMarks getMarks(@NotNull Player player, @NotNull GuPath path) {
        return getBareMarks(player, path).add(PathEffectService.getMarks(player, path));
    }

    public static @NotNull Set<GuPath> getShownPaths(@NotNull Player player) {
        Set<GuPath> shown = EnumSet.noneOf(GuPath.class);
        for (GuPath path : GuPath.values()) {
            if (getAttainment(player, path) != GuAttainment.NONE || !getMarks(player, path).isEmpty()) shown.add(path);
        }
        return shown;
    }

    private static void store(ServerPlayer player, PathData data) {
        PlayerData.get(player).setPath(data);
    }

    public static void setBareMarks(@NotNull ServerPlayer player, @NotNull GuPath path, long marks) {
        storeBare(player, path, getBareMarks(player, path).withMarks(marks));
    }

    public static void setBareStrands(@NotNull ServerPlayer player, @NotNull GuPath path, long strands) {
        storeBare(player, path, getBareMarks(player, path).withStrands(strands));
    }

    public static void addBareStrands(@NotNull ServerPlayer player, @NotNull GuPath path, long delta) {
        setBareStrands(player, path, LongMath.saturatedAdd(getBareMarks(player, path).strands(), delta));
    }

    private static void storeBare(ServerPlayer player, GuPath path, DaoMarks marks) {
        store(player, get(player).with(path, getEntry(player, path).withMarks(marks)));
    }

    public static void shiftAttainment(@NotNull ServerPlayer player, @NotNull GuPath path, int delta) {
        setAttainment(player, path, getAttainment(player, path).shift(delta));
    }

    public static void setAttainment(@NotNull ServerPlayer player, @NotNull GuPath path,
                                     @NotNull GuAttainment attainment) {
        store(player, get(player).with(path, getEntry(player, path).withAttainment(attainment)));
    }
}
