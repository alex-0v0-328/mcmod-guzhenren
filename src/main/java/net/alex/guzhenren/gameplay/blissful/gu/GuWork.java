package net.alex.guzhenren.gameplay.blissful.gu;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.alex.guzhenren.core.Catchable;
import net.alex.guzhenren.gameplay.blissful.land.BlissfulLandData;
import net.alex.guzhenren.gameplay.blissful.mountain.MountainWork;
import net.alex.guzhenren.gameplay.blissful.storage.BlissfulStorage;
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
 *       {@link MountainWork#isManaged} 认可的（已命名、拴绳的不算）；按实体类型 ID 分种类。</li>
 *   <li>{@link #capture}：每种捕到只剩保持数，保持数 0 的种类不捕；捕到的是 {@link Catchable#caughtStack} 给的未炼化
 *       蛊虫物品（同右键捕获），直接进库藏，实体移除。一次至多 {@code limit} 只。</li>
 *   <li>野生蛊不繁殖，福地暂不自然刷野生蛊（Alex 定），少了不补：托管 {@link #host} 只捕多出来的。</li>
 * </ul>
 */

public final class GuWork {

    private GuWork() {}

    public static final int LIMIT = 16;

    public static @NotNull ResourceLocation typeId(@NotNull Entity entity) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
    }

    public static @NotNull Map<ResourceLocation, List<Mob>> census(@NotNull Iterable<? extends Entity> entities) {
        Map<ResourceLocation, List<Mob>> swarms = new LinkedHashMap<>();
        for (Entity entity : entities) {
            if (entity instanceof Mob gu && gu instanceof Catchable && gu.getType().is(ModEntityTypeTags.WILD_GU)
                    && MountainWork.isManaged(gu)) {
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
        data.gu().keep().forEach((type, floor) -> {
            if (data.gu().isHosted(type)) keep.put(type, floor);
        });
        return capture(swarms, keep, data.storage(), limit);
    }
}
