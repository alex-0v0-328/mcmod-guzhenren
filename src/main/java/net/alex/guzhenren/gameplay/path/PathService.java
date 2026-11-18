package net.alex.guzhenren.gameplay.path;

import com.google.common.math.LongMath;
import java.util.EnumSet;
import java.util.Set;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * 玩家流派进度（造诣与肉身裸道痕[Path Marks]）的唯一运行时写入服务。
 * <ul>
 *   <li>裸道痕指不依附于任何流派效果、仅留在肉体上的道痕条与点（当前仅由管理员指令写入）；种族、体质与蛊虫带来的效果道
 *       痕归 {@link PathEffectService} 管理。</li>
 *   <li>{@link #getMarks} 在读取时将裸道痕（{@link #getBareMarks}）与效果道痕（{@link PathEffectService#getMarks}）实时
 *       相加，返回该流派的完整道痕总数。</li>
 *   <li>⚠ 流派造诣只存当前等级而不区分来源：异人种族授予或撤销天赋时通过 {@link #shiftAttainment} 相对平移等级，切勿用
 *       硬编码等级直接覆盖玩家修得的造诣。</li>
 * </ul>
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

    public static void setBarePoints(@NotNull ServerPlayer player, @NotNull GuPath path, long points) {
        storeBare(player, path, getBareMarks(player, path).withPoints(points));
    }

    public static void addBarePoints(@NotNull ServerPlayer player, @NotNull GuPath path, long delta) {
        setBarePoints(player, path, LongMath.saturatedAdd(getBareMarks(player, path).points(), delta));
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
