package net.alex.guzhenren.gameplay.immortalaperture.state;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.alex.guzhenren.gameplay.immortalaperture.farm.FarmWork;
import net.alex.guzhenren.gameplay.immortalaperture.gu.GuWork;
import net.alex.guzhenren.gameplay.immortalaperture.herd.HerdWork;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

/**
 * 福地动物、野生蛊与作物清点的同刻缓存：同一游戏刻里总览、养殖、蛊虫、种植页与托管轮共用一次扫描。
 * <ul>
 *   <li>按维度存，记清点时的游戏刻；作物清点另记是哪份存档算的（GameTest 会拿自建存档配主世界）。</li>
 *   <li>⚠ 宰杀会从清点表里删动物：面板操作后（{@code EarthSpiritPanel#handle}）与托管轮后
 *       （{@code BlissfulLands#host}）必须 {@link #invalidate}；停服 {@link #clear}。</li>
 * </ul>
 */

public final class LandCensus {

    private LandCensus() {}

    private record Herds(long time, Map<ResourceLocation, List<Animal>> herds) {}

    private record Swarms(long time, Map<ResourceLocation, List<Mob>> gus) {}

    private record Crops(long time, BlissfulLandData data, Map<Block, FarmWork.Count> crops) {}

    private static final Map<ResourceKey<Level>, Herds> HERDS = new HashMap<>();
    private static final Map<ResourceKey<Level>, Swarms> SWARMS = new HashMap<>();
    private static final Map<ResourceKey<Level>, Crops> CROPS = new HashMap<>();

    public static @NotNull Map<ResourceLocation, List<Animal>> herds(@NotNull ServerLevel level) {
        Herds cached = HERDS.get(level.dimension());
        if (cached != null && cached.time() == level.getGameTime()) return cached.herds();

        Map<ResourceLocation, List<Animal>> herds = HerdWork.census(level);
        HERDS.put(level.dimension(), new Herds(level.getGameTime(), herds));
        return herds;
    }

    public static @NotNull Map<ResourceLocation, List<Mob>> gus(@NotNull ServerLevel level) {
        Swarms cached = SWARMS.get(level.dimension());
        if (cached != null && cached.time() == level.getGameTime()) return cached.gus();

        Map<ResourceLocation, List<Mob>> gus = GuWork.census(level.getAllEntities());
        SWARMS.put(level.dimension(), new Swarms(level.getGameTime(), gus));
        return gus;
    }

    public static @NotNull Map<Block, FarmWork.Count> crops(@NotNull ServerLevel level,
                                                            @NotNull BlissfulLandData data) {
        Crops cached = CROPS.get(level.dimension());
        if (cached != null && cached.time() == level.getGameTime() && cached.data() == data) return cached.crops();

        Map<Block, FarmWork.Count> crops = FarmWork.census(level, data);
        CROPS.put(level.dimension(), new Crops(level.getGameTime(), data, crops));
        return crops;
    }

    public static void invalidate(@NotNull ServerLevel level) {
        HERDS.remove(level.dimension());
        SWARMS.remove(level.dimension());
        CROPS.remove(level.dimension());
    }

    public static void clear() {
        HERDS.clear();
        SWARMS.clear();
        CROPS.clear();
    }
}
