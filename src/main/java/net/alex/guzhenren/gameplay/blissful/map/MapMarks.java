package net.alex.guzhenren.gameplay.blissful.map;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import net.alex.guzhenren.gameplay.blissful.BlissfulLand;
import net.alex.guzhenren.gameplay.blissful.BlissfulLandData;
import net.alex.guzhenren.gameplay.blissful.EarthSpiritEntity;
import net.alex.guzhenren.gameplay.blissful.LandCensus;
import net.alex.guzhenren.gameplay.blissful.herd.PastureWork;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 地图活物标记的取样：福地维度里视口内的玩家、动物（栏内与栏外分开）、野生蛊与地灵，按离视口中心近到远取至多
 * {@value #LIMIT} 个。
 * <ul>
 *   <li>动物与野生蛊用 {@link LandCensus} 的同刻清点（与面板共用一次实体遍历）；地灵按存档记的 UUID 取。</li>
 *   <li>{@link #penned} 是「栏内」判定的唯一接入点：落在任一牧场的 2×2 区块内即栏内（{@code PastureWork#penner}）。
 *       </li>
 *   <li>⚠ 每秒每个订阅者一次；清点表同刻缓存，多人同刻取样只遍历一次实体。</li>
 * </ul>
 */

public final class MapMarks {

    private MapMarks() {}

    public static final int LIMIT = 256;

    private record Candidate(MapMark mark, double distance) {}

    public static @NotNull List<MapMark> collect(@NotNull ServerLevel level, @NotNull BlissfulLandData data,
                                                 @NotNull MapView view) {
        List<Candidate> found = new ArrayList<>();
        for (ServerPlayer player : level.players()) add(found, view, player, MapMark.PLAYER, player.getUUID());
        Predicate<Entity> penned = penned(data.land(), data);
        for (List<Animal> herd : LandCensus.herds(level).values()) {
            for (Animal animal : herd) {
                add(found, view, animal, penned.test(animal) ? MapMark.PENNED : MapMark.LOOSE, null);
            }
        }
        for (List<Mob> swarm : LandCensus.gus(level).values()) {
            for (Mob gu : swarm) add(found, view, gu, MapMark.WILD_GU, null);
        }
        UUID spirit = data.spirit();
        if (spirit != null && level.getEntity(spirit) instanceof EarthSpiritEntity entity && entity.isAlive()) {
            add(found, view, entity, MapMark.SPIRIT, null);
        }
        found.sort(Comparator.comparingDouble(Candidate::distance));
        List<MapMark> marks = new ArrayList<>(Math.min(LIMIT, found.size()));
        for (int i = 0; i < found.size() && i < LIMIT; i++) marks.add(found.get(i).mark());
        return marks;
    }

    public static @NotNull Predicate<Entity> penned(@NotNull BlissfulLand land, @NotNull BlissfulLandData data) {
        return PastureWork.penner(data);
    }

    private static void add(List<Candidate> found, MapView view, Entity entity, byte kind, @Nullable UUID player) {
        int x = entity.getBlockX();
        int z = entity.getBlockZ();
        if (!view.contains(x >> 4, z >> 4)) return;
        MapMark mark = new MapMark(kind, x, (short) entity.getBlockY(), z, Optional.ofNullable(player));
        found.add(new Candidate(mark, view.blockDistanceSq(entity.getX(), entity.getZ())));
    }
}
