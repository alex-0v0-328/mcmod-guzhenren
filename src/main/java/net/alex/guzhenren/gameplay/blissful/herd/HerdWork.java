package net.alex.guzhenren.gameplay.blissful.herd;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.alex.guzhenren.gameplay.blissful.BlissfulLandData;
import net.alex.guzhenren.gameplay.blissful.mountain.MountainWork;
import net.alex.guzhenren.gameplay.blissful.storage.BlissfulStorage;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 地灵面板「养殖」页：按种类清点、喂养繁殖、宰杀收取，以及托管的「每种保持 N 只」。
 * <ul>
 *   <li>动物 = 已加载区块里的原版 {@link Animal}，且是 {@link MountainWork#isManaged} 认可的；按实体类型 ID 分种类，幼
 *       崽计入数量但不宰、不喂。</li>
 *   <li>{@link #breed}：每种取尚未发情、能发情的成年动物，凑成对；每只扣 1 份该种能吃的饲料（库藏里第一种
 *       {@code Animal#isFood} 为真的物品，不够就少喂），{@code setInLove} 后由原版繁殖 AI 自己配对。</li>
 *   <li>{@link #slaughter}：每种宰到只剩保持数，保持数为 0 的种类不宰；按该动物的战利品表掷产物进库藏，经验按原版死亡经
 *       验直接加给主人（主人为空即作废），然后直接移除（不触发死亡事件，不长胆石）。</li>
 *   <li>托管 {@link #host} 只管托管开着的种类；一键宰杀照所有种类的保持数算；{@link #slaughterCount} 用同一规则预估一
 *       键宰杀会宰几头（不动清点表）。</li>
 *   <li>两个操作共用一份对象预算（每次至多 {@code limit} 只），托管 {@link #host} 先宰后喂；宰掉的动物从清点表里删去，
 *       后面的喂养按删后的只数算缺口（每对只补一只崽）。</li>
 * </ul>
 */

public final class HerdWork {

    private HerdWork() {}

    public static final int LIMIT = 64;

    public record Breed(int animals, int food) {}

    public record Hosted(int slaughtered, int bred) {

        public int total() { return slaughtered + bred; }
    }

    public static @NotNull ResourceLocation typeId(@NotNull Entity entity) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
    }

    public static @NotNull Map<ResourceLocation, List<Animal>> census(@NotNull ServerLevel level) {
        return census(level.getAllEntities());
    }

    public static @NotNull Map<ResourceLocation, List<Animal>> census(@NotNull Iterable<? extends Entity> entities) {
        Map<ResourceLocation, List<Animal>> herds = new LinkedHashMap<>();
        for (Entity entity : entities) {
            if (entity instanceof Animal animal && MountainWork.isManaged(animal)) {
                herds.computeIfAbsent(typeId(animal), type -> new ArrayList<>()).add(animal);
            }
        }
        return herds;
    }

    //region Breed [喂养繁殖]
    public static @NotNull Breed breed(@NotNull Map<ResourceLocation, List<Animal>> herds,
                                       @NotNull BlissfulStorage storage, int limit,
                                       @Nullable Map<ResourceLocation, Integer> wantedPairs) {
        int animals = 0;
        int food = 0;
        for (Map.Entry<ResourceLocation, List<Animal>> herd : herds.entrySet()) {
            int maxPairs = wantedPairs == null ? Integer.MAX_VALUE : wantedPairs.getOrDefault(herd.getKey(), 0);
            List<Animal> ready = new ArrayList<>();
            for (Animal animal : herd.getValue()) {
                if (animal.getAge() == 0 && animal.canFallInLove()) ready.add(animal);
            }
            if (ready.size() < 2 || maxPairs <= 0) continue;

            Animal sample = ready.getFirst();
            Predicate<ItemStack> feed = sample::isFood;
            long stocked = storage.count(feed);
            int pairs = (int) Math.min(Math.min(ready.size() / 2, maxPairs),
                    Math.min(stocked / 2, (limit - animals) / 2));
            if (pairs <= 0) continue;

            int eaten = storage.consume(feed, pairs * 2);
            for (int i = 0; i < eaten; i++) ready.get(i).setInLove(null);
            animals += eaten;
            food += eaten;
        }
        return new Breed(animals, food);
    }
    //endregion

    //region Slaughter [宰杀收取]
    public static int slaughter(@NotNull ServerLevel level, @NotNull Map<ResourceLocation, List<Animal>> herds,
                                @NotNull Map<ResourceLocation, Integer> keep, @NotNull BlissfulStorage storage,
                                @Nullable ServerPlayer owner, int limit) {
        int killed = 0;
        for (Map.Entry<ResourceLocation, List<Animal>> herd : herds.entrySet()) {
            int floor = keep.getOrDefault(herd.getKey(), 0);
            if (floor <= 0) continue;

            int excess = herd.getValue().size() - floor;
            for (Iterator<Animal> animals = herd.getValue().iterator(); animals.hasNext(); ) {
                if (excess <= 0 || killed >= limit) break;
                Animal animal = animals.next();
                if (animal.isBaby()) continue;
                harvest(level, animal, storage, owner);
                animals.remove();
                excess--;
                killed++;
            }
        }
        return killed;
    }

    public static int slaughterCount(@NotNull Map<ResourceLocation, List<Animal>> herds,
                                     @NotNull Map<ResourceLocation, Integer> keep, int limit) {
        int count = 0;
        for (Map.Entry<ResourceLocation, List<Animal>> herd : herds.entrySet()) {
            int floor = keep.getOrDefault(herd.getKey(), 0);
            if (floor <= 0) continue;
            int adults = (int) herd.getValue().stream().filter(animal -> !animal.isBaby()).count();
            count += Math.min(Math.max(0, herd.getValue().size() - floor), adults);
        }
        return Math.min(count, limit);
    }

    private static void harvest(ServerLevel level, Animal animal, BlissfulStorage storage,
                                @Nullable ServerPlayer owner) {
        LootTable table = level.getServer().reloadableRegistries().getLootTable(animal.getLootTable());
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, animal)
                .withParameter(LootContextParams.ORIGIN, animal.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().generic())
                .create(LootContextParamSets.ENTITY);
        table.getRandomItems(params, storage::insert);
        if (owner != null) owner.giveExperiencePoints(animal.getExperienceReward(level, owner));
        animal.discard();
    }
    //endregion

    //region Hosting [托管]
    public static @NotNull Hosted host(@NotNull ServerLevel level, @NotNull Map<ResourceLocation, List<Animal>> herds,
                                       @NotNull BlissfulLandData data, @Nullable ServerPlayer owner, int limit) {
        Map<ResourceLocation, Integer> keep = new LinkedHashMap<>();
        data.herd().keep().forEach((type, floor) -> {
            if (data.herd().isHosted(type)) keep.put(type, floor);
        });
        int killed = slaughter(level, herds, keep, data.storage(), owner, limit);
        Map<ResourceLocation, Integer> wanted = new LinkedHashMap<>();
        keep.forEach((type, floor) -> {
            int missing = floor - herds.getOrDefault(type, List.of()).size();
            if (missing > 0) wanted.put(type, missing);
        });
        return new Hosted(killed, breed(herds, data.storage(), limit - killed, wanted).animals());
    }
    //endregion
}
