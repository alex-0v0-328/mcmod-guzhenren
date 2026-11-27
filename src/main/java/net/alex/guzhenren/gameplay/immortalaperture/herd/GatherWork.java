package net.alex.guzhenren.gameplay.immortalaperture.herd;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLandData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 牧场收拢按设置走：地图牧场卡片上的手动收拢与托管轮里的自动收拢共用 {@link #gather}，不设冷却（Alex 定）。
 * <ul>
 *   <li>{@link #gather}：候选先按 {@link PastureSettings#gathers} 筛掉这个牧场不收的种类，余下交
 *       {@link PastureWork#gather}（栏外、可支使、非捕食者，至多 {@value HerdRules#GATHER_LIMIT} 头）。</li>
 *   <li>{@link #host} 是托管任务：按牧场顺序，开着自动收拢的各收拢一次，候选是维度里已加载的动物；预算按牧场各
 *       {@value HerdRules#GATHER_LIMIT} 头，不占养殖托管的预算。排在养殖托管之前，收进来的这一轮就算栏内。</li>
 *   <li>带 {@link PastureWork.Pasture} 的重载给 GameTest 用测试区里的小方格，设置仍按 {@code corner} 取。</li>
 * </ul>
 */

public final class GatherWork {

    private GatherWork() {}

    public static int gather(@NotNull ServerLevel level, @NotNull BlissfulLandData data, @NotNull ChunkPos corner,
                             @NotNull RandomSource random, @NotNull Iterable<? extends Entity> candidates) {
        return gather(level, data, corner, PastureWork.Pasture.at(corner.x, corner.z), random, candidates);
    }

    public static int gather(@NotNull ServerLevel level, @NotNull BlissfulLandData data, @NotNull ChunkPos corner,
                             @NotNull PastureWork.Pasture pasture, @NotNull RandomSource random,
                             @NotNull Iterable<? extends Entity> candidates) {
        PastureSettings settings = PastureSettings.of(data);
        List<Animal> wanted = new ArrayList<>();
        for (Entity entity : candidates) {
            if (entity instanceof Animal animal && settings.gathers(corner, HerdWork.typeId(animal))) {
                wanted.add(animal);
            }
        }
        return PastureWork.gather(level, data, pasture, random, wanted);
    }

    public static int host(@NotNull ServerLevel level, @NotNull BlissfulLandData data, @Nullable ServerPlayer owner) {
        List<ChunkPos> corners = new ArrayList<>();
        for (ChunkPos corner : HerdState.of(data).pastures()) {
            if (PastureSettings.of(data).autoGather(corner)) corners.add(corner);
        }
        if (corners.isEmpty()) return 0;

        List<Animal> animals = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof Animal animal) animals.add(animal);
        }
        int moved = 0;
        for (ChunkPos corner : corners) moved += gather(level, data, corner, level.getRandom(), animals);
        return moved;
    }
}
