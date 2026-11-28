package net.alex.guzhenren.gameplay.immortalaperture.herd;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLandData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.NotNull;

/**
 * 牧场收拢：地图牧场对象栏上的「收拢」把福地里栏外的、这个牧场指定的那种动物传进来，不设冷却，也没有自动收拢（Alex 定）。
 * <ul>
 *   <li>{@link #gather}：候选只留种类等于指定动物的，余下交 {@link PastureWork#gather}（栏外、可支使、非捕食者，至多
 *       {@value HerdRules#GATHER_LIMIT} 头）；没指定动物的牧场收 0 头。</li>
 *   <li>{@link #outside} 数福地已加载、不在任何牧场里的这种可支使动物（对象栏「收拢」键角上的数字）。</li>
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
        ResourceLocation type = PastureSettings.of(data).animal(corner);
        if (type == null) return 0;
        List<Animal> wanted = new ArrayList<>();
        for (Entity entity : candidates) {
            if (entity instanceof Animal animal && type.equals(HerdWork.typeId(animal))) wanted.add(animal);
        }
        return PastureWork.gather(level, data, pasture, random, wanted);
    }

    public static int outside(@NotNull List<Animal> herd, @NotNull List<PastureWork.Pasture> pastures) {
        int count = 0;
        for (Animal animal : herd) {
            if (!PastureWork.isPredator(animal) && !PastureWork.isPenned(pastures, animal)) count++;
        }
        return count;
    }
}
