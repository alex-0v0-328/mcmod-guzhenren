package net.alex.guzhenren.gameplay.immortalaperture.herd;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.MapMark;
import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.MapMarkSource;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLandData;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandCensus;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandRules;
import net.alex.guzhenren.gameplay.immortalaperture.storage.BlissfulStorage;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 福地养殖：一个牧场只养指定的一种（Alex 定），按牧场清点、喂养繁殖、宰杀收取与托管（多出下限宰、不足下限喂）。
 * <ul>
 *   <li>动物 = 已加载区块里的原版 {@link Animal}，且是 {@link LandRules#isManaged} 认可的；按实体类型 ID 分种类，幼
 *       崽计入数量但不宰、不喂。牧场的畜群（{@link Pen#herd}）= 落在这个牧场里、种类是它指定动物的；没指定动物的牧场
 *       不喂不宰。</li>
 *   <li>{@link #breed}：取畜群里尚未发情、能发情的成年动物凑成对；每只扣 1 份该种能吃的饲料（库藏里第一种
 *       {@code Animal#isFood} 为真的物品，不够就少喂），{@code setInLove} 后由原版繁殖 AI 自己配对。</li>
 *   <li>{@link #slaughter}：宰到只剩下限，下限 0 不宰；按该动物的战利品表掷产物进库藏，经验按原版死亡经验直接加给主人
 *       （主人为空即作废），然后直接移除（不触发死亡事件，不长胆石）。{@link #slaughterCount} 用同一规则预估会宰几头。</li>
 *   <li>托管 {@link #host}：有主就跑，不设开关（Alex 定）；每个下限大于 0 的牧场先宰后喂（每对只补一只崽，按宰后的头数算
 *       缺口），所有牧场共用 {@value HerdRules#HOSTING_ANIMALS} 只的预算。</li>
 * </ul>
 */

public final class HerdWork {

    private HerdWork() {}

    public static final LandCensus.Census<Map<ResourceLocation, List<Animal>>> CENSUS =
            LandCensus.Census.ofLevel(HerdWork::census);

    public static int host(@NotNull ServerLevel level, @NotNull BlissfulLandData data, @Nullable ServerPlayer owner) {
        Hosted hosted = host(level, pens(level, data), data, owner, HerdRules.HOSTING_ANIMALS);
        if (hosted.total() > 0 || HerdState.of(data).lastSlaughtered() + HerdState.of(data).lastBred() > 0) {
            HerdState.of(data).setLastRound(hosted.slaughtered(), hosted.bred());
        }
        return hosted.total();
    }

    /** 一个牧场：下标、西北角、方格、指定动物（未指定为 {@code null}）、下限、畜群与栏内全部动物头数。 */
    public record Pen(int index, @NotNull ChunkPos corner, PastureWork.@NotNull Pasture pasture,
                      @Nullable ResourceLocation animal, int floor, @NotNull List<Animal> herd, int penned) {}

    public static @NotNull List<Pen> pens(@NotNull ServerLevel level, @NotNull BlissfulLandData data) {
        return pens(LandCensus.take(level, CENSUS), data);
    }

    public static @NotNull List<Pen> pens(@NotNull Map<ResourceLocation, List<Animal>> census,
                                          @NotNull BlissfulLandData data) {
        List<Pen> pens = new ArrayList<>();
        List<ChunkPos> corners = HerdState.of(data).pastures();
        for (int index = 0; index < corners.size(); index++) {
            ChunkPos corner = corners.get(index);
            PastureWork.Pasture pasture = PastureWork.Pasture.at(corner.x, corner.z);
            ResourceLocation animal = PastureSettings.of(data).animal(corner);
            Map<ResourceLocation, List<Animal>> inside = PastureWork.penned(census, List.of(pasture));
            List<Animal> herd = animal == null ? List.of() : new ArrayList<>(inside.getOrDefault(animal, List.of()));
            pens.add(new Pen(index, corner, pasture, animal, PastureSettings.of(data).floor(corner), herd,
                    PastureWork.count(inside)));
        }
        return pens;
    }

    public static void mapMarks(@NotNull ServerLevel level, @NotNull BlissfulLandData data,
                                @NotNull MapMarkSource.Sink sink) {
        Predicate<Entity> penned = PastureWork.penner(data);
        for (List<Animal> herd : LandCensus.take(level, CENSUS).values()) {
            for (Animal animal : herd) sink.add(animal, penned.test(animal) ? MapMark.PENNED : MapMark.LOOSE);
        }
    }

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
            if (entity instanceof Animal animal && LandRules.isManaged(animal)) {
                herds.computeIfAbsent(typeId(animal), type -> new ArrayList<>()).add(animal);
            }
        }
        return herds;
    }

    //region Breed [喂养繁殖]
    public static @NotNull Breed breed(@NotNull List<Animal> herd, @NotNull BlissfulStorage storage, int limit,
                                       int maxPairs) {
        List<Animal> ready = new ArrayList<>();
        for (Animal animal : herd) {
            if (animal.getAge() == 0 && animal.canFallInLove()) ready.add(animal);
        }
        if (ready.size() < 2 || maxPairs <= 0 || limit < 2) return new Breed(0, 0);

        Predicate<ItemStack> feed = ready.getFirst()::isFood;
        int pairs = (int) Math.min(Math.min(ready.size() / 2, maxPairs), Math.min(storage.count(feed) / 2, limit / 2));
        if (pairs <= 0) return new Breed(0, 0);
        int eaten = storage.consume(feed, pairs * 2);
        for (int i = 0; i < eaten; i++) ready.get(i).setInLove(null);
        return new Breed(eaten, eaten);
    }

    public static @NotNull Breed breedAll(@NotNull List<Pen> pens, @NotNull BlissfulStorage storage, int limit) {
        int animals = 0;
        for (Pen pen : pens) {
            if (pen.animal() != null) animals += breed(pen.herd(), storage, limit - animals, Integer.MAX_VALUE).animals();
        }
        return new Breed(animals, animals);
    }
    //endregion

    //region Slaughter [宰杀收取]
    public static int slaughter(@NotNull ServerLevel level, @NotNull List<Animal> herd, int floor,
                                @NotNull BlissfulStorage storage, @Nullable ServerPlayer owner, int limit) {
        if (floor <= 0) return 0;
        int excess = herd.size() - floor;
        int killed = 0;
        for (Iterator<Animal> animals = herd.iterator(); animals.hasNext(); ) {
            if (excess <= 0 || killed >= limit) break;
            Animal animal = animals.next();
            if (animal.isBaby()) continue;
            harvest(level, animal, storage, owner);
            animals.remove();
            excess--;
            killed++;
        }
        return killed;
    }

    public static int slaughterAll(@NotNull ServerLevel level, @NotNull List<Pen> pens,
                                   @NotNull BlissfulStorage storage, @Nullable ServerPlayer owner, int limit) {
        int killed = 0;
        for (Pen pen : pens) killed += slaughter(level, pen.herd(), pen.floor(), storage, owner, limit - killed);
        return killed;
    }

    public static int slaughterCount(@NotNull List<Animal> herd, int floor) {
        if (floor <= 0) return 0;
        int adults = (int) herd.stream().filter(animal -> !animal.isBaby()).count();
        return Math.min(Math.max(0, herd.size() - floor), adults);
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
    public static @NotNull Hosted host(@NotNull ServerLevel level, @NotNull List<Pen> pens,
                                       @NotNull BlissfulLandData data, @Nullable ServerPlayer owner, int limit) {
        BlissfulStorage storage = BlissfulStorage.of(data);
        int killed = 0;
        int bred = 0;
        for (Pen pen : pens) {
            if (pen.animal() == null || pen.floor() <= 0) continue;
            killed += slaughter(level, pen.herd(), pen.floor(), storage, owner, limit - killed - bred);
            int missing = pen.floor() - pen.herd().size();
            if (missing > 0) bred += breed(pen.herd(), storage, limit - killed - bred, missing).animals();
        }
        return new Hosted(killed, bred);
    }
    //endregion
}
