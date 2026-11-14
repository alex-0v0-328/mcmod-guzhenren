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
 * 通用锚定维度旅行服务。
 * <ul>
 *   <li>一个 {@link DimensionReturnData} 附件存唯一返回点；{@link #enter} 传送前记原位与是否在飞，已在目标或无目标维度
 *       返回 {@code false}；锚定维度间再进入保留首个快照，退出仍回出发处；死亡经
 *       {@link net.alex.guzhenren.lifecycle.PlayerDataService} 清记录。</li>
 *   <li>{@link #exit}：有返回点则传回，进入时在飞且去掉本授权仍能飞才续飞，返回 {@code true}；无记录兜底送主世界公共出
 *       生点，返回 {@code false}；两路都撤授权、清记录，{@link #clear(ServerPlayer)} 无记录也安全。</li>
 *   <li>{@link #getArrival}：{@code FLIGHT} 落在出生点并授飞、开飞；{@code SURFACE} 落在该列顶上并撤销飞行维度带来的授
 *       权。{@code Level.getHeight} 把未加载区块读成世界底部，故先加载该列区块，未生成则在服务端线程生成，首入新位置会
 *       卡顿。</li>
 *   <li>⚠ 维度内飞行是 {@link FlightAbility} 来源（不存档），不直接写 {@code Abilities.mayfly}：该字段随玩家存档、
 *       与游戏模式及他模组共用，直接写曾在未配对恢复就离开时泄漏生存飞行。</li>
 *   <li>{@link #ensureFlight} 每 tick 补授权，不强制开飞；{@link #revokeFlight} 撤授权，无其他来源时一并关飞，玩家在所
 *       有给飞行的锚定维度外每 tick 调用，故退出、原版 {@code /tp}、他模组传送等任何途径离开都撤销。</li>
 *   <li>{@link #rescueIfBelowVoid}：低于最低建造高度时传回给定出生点，传送了返回 {@code true}。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see DimensionReturnData
 * @since 1.0.0
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
