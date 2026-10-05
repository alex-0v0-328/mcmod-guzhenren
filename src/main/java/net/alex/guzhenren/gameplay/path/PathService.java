package net.alex.guzhenren.gameplay.path;

import com.google.common.math.LongMath;
import java.util.Map;
import net.alex.guzhenren.registry.attachment.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * The only runtime writer of Path [流派] progress: attainment and Dao marks [道痕].
 *
 * <p>Static service over the {@code path_data} attachment; reads take {@link Player}, writes take
 * {@link ServerPlayer}. Every write names a {@link MarkTag}; the command path writes {@code NATURAL}
 * and takes NO tag argument. {@code RACE} and {@code EXTREME_PHYSIQUE} marks are written by their
 * respective systems; do not re-add a tag argument or a gate enum. ⚠ {@code attainment} is a GRADE,
 * so a race grant that shifts it up is never "set" -- it MOVES, and the revoke shifts back down; nothing
 * can tell a granted master from an earned one.
 *
 * @author Alex
 * @version 1.0.0
 * @see PathData
 * @see PathEntry
 * @since 1.0.0
 */

public final class PathService {

    private PathService() {}

    public static @NotNull PathData get(@NotNull Player player) { return player.getData(ModAttachments.PATH); }

    public static @NotNull PathEntry getEntry(@NotNull Player player, @NotNull GuPath path) {
        return get(player).get(path);
    }

    public static @NotNull GuAttainment getAttainment(@NotNull Player player, @NotNull GuPath path) {
        return getEntry(player, path).attainment();
    }

    public static long getMark(@NotNull Player player, @NotNull GuPath path, @NotNull MarkTag tag) {
        return getEntry(player, path).mark(tag);
    }

    public static @NotNull Map<GuPath, PathEntry> getVisibleEntries(@NotNull Player player) {
        return get(player).entries();
    }

    private static void store(ServerPlayer player, PathData data) { player.setData(ModAttachments.PATH, data); }

    public static void setMark(@NotNull ServerPlayer player, @NotNull GuPath path, @NotNull MarkTag tag, long value) {
        store(player, get(player).with(path, getEntry(player, path).withMark(tag, value)));
    }

    public static void addMark(@NotNull ServerPlayer player, @NotNull GuPath path, @NotNull MarkTag tag, long delta) {
        setMark(player, path, tag, LongMath.saturatedAdd(getMark(player, path, tag), delta));
    }

    public static void shiftAttainment(@NotNull ServerPlayer player, @NotNull GuPath path, int delta) {
        setAttainment(player, path, getAttainment(player, path).shift(delta));
    }

    public static void setAttainment(@NotNull ServerPlayer player, @NotNull GuPath path,
                                     @NotNull GuAttainment attainment) {
        store(player, get(player).with(path, getEntry(player, path).withAttainment(attainment)));
    }
}
