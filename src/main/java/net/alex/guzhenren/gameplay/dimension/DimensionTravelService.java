package net.alex.guzhenren.gameplay.dimension;

import java.util.Objects;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.gameplay.dimension.DimensionReturnData.ReturnPoint;
import net.alex.guzhenren.gameplay.flight.FlightAbility;
import net.alex.guzhenren.registry.world.ModDimensions;
import net.alex.guzhenren.registry.world.ModDimensions.AnchoredDimension;
import net.alex.guzhenren.registry.world.ModDimensions.Arrival;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * 通用锚定维度进出与维度内飞行授权服务。
 * <ul>
 *   <li>通过 {@link DimensionReturnData} 保存唯一返回点：{@link #enter} 在首次从普通维度进入锚定维度时记录原坐标与是否
 *       正在飞行（已在目标维度或目标维度不存在时返回 {@code false}；在锚定维度之间连续跳转时保留最初的出发点快照，使
 *       {@link #exit} 始终回到最初出发处；死亡时由 {@code PlayerDataService} 清空记录）。</li>
 *   <li>{@link #exit}：若有有效返回点则传回原处（仅当进入前在飞且撤销维度授权后仍具备飞行能力时才恢复飞行状态）并返回
 *       {@code true}；无返回点时兜底传回主世界公共出生点并返回 {@code false}；两条路径均撤销维度飞行授权并清空返回点记
 *       录。</li>
 *   <li>{@link #getArrival}：{@code FLIGHT} 模式直接落在配置出生点并授予、开启飞行；{@code SURFACE} 模式先强制加载目标
 *       列区块（因未加载区块的 {@code Level.getHeight} 会读成世界最低高度，首次进入未生成位置会在服务端线程同步生成该区
 *       块），再落在地表最高阻挡格上方并撤销锚定维度飞行授权。</li>
 *   <li>⚠ 锚定维度内的飞行通过 {@link FlightAbility} 临时属性修饰符授予（不落盘），切勿直接改写随玩家存档且与游戏模式
 *       共用的 {@code Abilities.mayfly}，以免异常离开时在生存模式永久残留飞行权限。</li>
 *   <li>{@link #ensureFlight} 每 tick 补齐维度飞行授权（不强制进入飞行姿态）；{@link #revokeFlight} 在玩家离开所有授予
 *       飞行的锚定维度后每 tick 调用（无论经指令退出、原版 {@code /tp} 还是其他模组传送），撤销授权并在无其他飞行来源时
 *       关闭飞行。</li>
 *   <li>{@link #rescueIfBelowVoid} 在玩家跌出最低建筑高度时传回指定出生点并清零摔落距离。</li>
 * </ul>
 */

public final class DimensionTravelService {

    private DimensionTravelService() {}

    private static final ResourceLocation FLIGHT_MODIFIER_ID = Guzhenren.id("anchored_dimension_flight");

    public static boolean isInside(@NotNull Player player, @NotNull ResourceKey<Level> dimension) {
        return player.level().dimension().equals(Objects.requireNonNull(dimension, "dimension"));
    }

    public static boolean enter(@NotNull ServerPlayer player, @NotNull AnchoredDimension destination) {
        Objects.requireNonNull(destination, "destination");
        if (isInside(player, destination.level())) {
            return false;
        }

        ServerLevel target = player.server.getLevel(destination.level());
        if (target == null) {
            return false;
        }

        if (!ModDimensions.ANCHORED_DIMENSIONS.containsKey(player.level().dimension())) {
            ReturnPoint point = new ReturnPoint(
                    player.level().dimension(),
                    player.getX(), player.getY(), player.getZ(),
                    player.getYRot(), player.getXRot(),
                    player.getAbilities().flying);
            PlayerData.get(player).setDimensionReturn(DimensionReturnData.of(point));
        }

        Vec3 arrival = getArrival(target, destination);
        player.teleportTo(target, arrival.x, arrival.y, arrival.z, player.getYRot(), player.getXRot());
        if (destination.arrival() == Arrival.FLIGHT) {
            ensureFlight(player);
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
        } else {
            revokeFlight(player);
            player.fallDistance = 0.0F;
        }
        return true;
    }

    public static @NotNull Vec3 getArrival(@NotNull ServerLevel level, @NotNull AnchoredDimension destination) {
        Vec3 spawn = destination.spawn();
        if (destination.arrival() == Arrival.FLIGHT) {
            return spawn;
        }

        BlockPos column = BlockPos.containing(spawn);
        level.getChunkAt(column);
        return new Vec3(spawn.x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, column.getX(), column.getZ()),
                spawn.z);
    }

    public static boolean exit(@NotNull ServerPlayer player) {
        ReturnPoint point = PlayerData.get(player).getDimensionReturn().point().orElse(null);
        ServerLevel target = point == null ? null : player.server.getLevel(point.level());
        if (target != null) {
            player.teleportTo(target, point.x(), point.y(), point.z(), point.yaw(), point.pitch());
            revokeFlight(player);
            player.getAbilities().flying = point.flying() && FlightAbility.canFly(player);
            player.fallDistance = 0.0F;
            player.onUpdateAbilities();
        } else {
            fallbackToOverworldSpawn(player);
        }
        clear(player);
        return target != null;
    }

    private static void fallbackToOverworldSpawn(@NotNull ServerPlayer player) {
        ServerLevel overworld = player.server.overworld();
        BlockPos spawn = overworld.getSharedSpawnPos();
        player.teleportTo(overworld, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                player.getYRot(), player.getXRot());
        revokeFlight(player);
        player.fallDistance = 0.0F;
    }

    public static void clear(@NotNull ServerPlayer player) {
        PlayerData.get(player).setDimensionReturn(DimensionReturnData.DEFAULT);
    }

    public static void ensureFlight(@NotNull ServerPlayer player) {
        if (!FlightAbility.has(player, FLIGHT_MODIFIER_ID)) FlightAbility.grant(player, FLIGHT_MODIFIER_ID);
    }

    public static void revokeFlight(@NotNull ServerPlayer player) {
        if (!FlightAbility.revoke(player, FLIGHT_MODIFIER_ID)) return;
        if (player.getAbilities().flying && !FlightAbility.canFly(player)) {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }

    public static boolean rescueIfBelowVoid(@NotNull ServerPlayer player, @NotNull Vec3 spawn) {
        if (player.getY() < player.level().getMinBuildHeight()) {
            player.teleportTo((ServerLevel) player.level(), spawn.x, spawn.y, spawn.z,
                    player.getYRot(), player.getXRot());
            player.fallDistance = 0.0F;
            return true;
        }
        return false;
    }
}
