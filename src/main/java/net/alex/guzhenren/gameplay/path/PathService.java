package net.alex.guzhenren.gameplay.path;

import com.google.common.math.LongMath;
import java.util.EnumSet;
import java.util.Set;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * 流派进度（造诣与纯道痕[Path Marks]）的唯一运行时写入方。
 *
 * <ul>
 *   <li>纯道痕是肉体上不起作用的条与丝，目前只有命令写入，一次写一个计数；随效果来的道痕（种族、体质、蛊）归
 *       {@link PathEffectService}，从不算纯道痕。</li>
 *   <li>{@link #getMarks} 是一条道[Path]的完整总数，每次读取时两半按计数相加。</li>
 *   <li>⚠ {@code attainment} 是等级：种族授予时上移、撤销时下移，从不「设置」；无法区分授予的大师与修来的大师。</li>
 * </ul>
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

    public static @NotNull PathMarks getBareMarks(@NotNull Player player, @NotNull GuPath path) {
        return getEntry(player, path).marks();
    }

    public static @NotNull PathMarks getMarks(@NotNull Player player, @NotNull GuPath path) {
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

    private static void storeBare(ServerPlayer player, GuPath path, PathMarks marks) {
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
