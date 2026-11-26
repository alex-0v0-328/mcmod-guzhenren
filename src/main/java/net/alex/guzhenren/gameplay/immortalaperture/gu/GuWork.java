package net.alex.guzhenren.gameplay.immortalaperture.gu;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.alex.guzhenren.core.Catchable;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLandData;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandCensus;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandRules;
import net.alex.guzhenren.gameplay.immortalaperture.storage.BlissfulStorage;
import net.alex.guzhenren.registry.entity.ModEntityTypeTags;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 地灵面板「蛊虫」页：按种类清点福地里的野生蛊，多出保持数的「捕获入库」，以及托管。
 * <ul>
 *   <li>野生蛊 = 已加载区块里 {@link ModEntityTypeTags#WILD_GU} 的 {@link Mob}，能被捕获（{@link Catchable}），且是
 *       {@link LandRules#isManaged} 认可的（已命名、拴绳的不算）；按实体类型 ID 分种类。</li>
 *   <li>{@link #capture}：每种捕到只剩保持数，保持数 0 的种类不捕；捕到的是 {@link Catchable#caughtStack} 给的未炼化
 *       蛊虫物品（同右键捕获），直接进库藏，实体移除。一次至多 {@code limit} 只。</li>
 *   <li>野生蛊不繁殖，福地暂不自然刷野生蛊（Alex 定），少了不补：托管 {@link #host} 只捕多出来的。</li>
 * </ul>
 */

public final class GuWork {

    private GuWork() {}

    public static final LandCensus.Census<Map<ResourceLocation, List<Mob>>> CENSUS =
            LandCensus.Census.ofLevel(level -> census(level.getAllEntities()));
    public static final int LIMIT = 16;

    public static @NotNull ResourceLocation typeId(@NotNull Entity entity) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
    }

    public static @NotNull Map<ResourceLocation, List<Mob>> census(@NotNull Iterable<? extends Entity> entities) {
        Map<ResourceLocation, List<Mob>> swarms = new LinkedHashMap<>();
        for (Entity entity : entities) {
            if (entity instanceof Mob gu && gu instanceof Catchable && gu.getType().is(ModEntityTypeTags.WILD_GU)
                    && LandRules.isManaged(gu)) {
                swarms.computeIfAbsent(typeId(gu), type -> new ArrayList<>()).add(gu);
            }
        }
        return swarms;
    }

    public static @NotNull ItemStack caughtStack(@NotNull Mob gu) { return ((Catchable) gu).caughtStack(); }

    public static int capture(@NotNull Map<ResourceLocation, List<Mob>> swarms,
                              @NotNull Map<ResourceLocation, Integer> keep, @NotNull BlissfulStorage storage,
                              int limit) {
        int caught = 0;
        for (Map.Entry<ResourceLocation, List<Mob>> swarm : swarms.entrySet()) {
            int floor = keep.getOrDefault(swarm.getKey(), 0);
            if (floor <= 0) continue;

            int excess = swarm.getValue().size() - floor;
            for (Iterator<Mob> gus = swarm.getValue().iterator(); gus.hasNext(); ) {
                if (excess <= 0 || caught >= limit) break;
                Mob gu = gus.next();
                storage.insert(caughtStack(gu));
                gu.discard();
                gus.remove();
                excess--;
                caught++;
            }
        }
        return caught;
    }

    public static int captureCount(@NotNull Map<ResourceLocation, List<Mob>> swarms,
                                   @NotNull Map<ResourceLocation, Integer> keep, int limit) {
        int count = 0;
        for (Map.Entry<ResourceLocation, List<Mob>> swarm : swarms.entrySet()) {
            int floor = keep.getOrDefault(swarm.getKey(), 0);
            if (floor > 0) count += Math.max(0, swarm.getValue().size() - floor);
        }
        return Math.min(count, limit);
    }

    public static int host(@NotNull Map<ResourceLocation, List<Mob>> swarms,
                           @NotNull BlissfulLandData data, int limit) {
        Map<ResourceLocation, Integer> keep = new LinkedHashMap<>();
        GuState.of(data).keep().forEach((type, floor) -> {
            if (GuState.of(data).isHosted(type)) keep.put(type, floor);
        });
        return capture(swarms, keep, BlissfulStorage.of(data), limit);
    }
}
