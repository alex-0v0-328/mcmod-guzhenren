package net.alex.guzhenren.registry.entity;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.entity.beast.BearEntity;
import net.alex.guzhenren.entity.beast.BearSpecies;
import net.alex.guzhenren.entity.beast.CattleEntity;
import net.alex.guzhenren.entity.beast.CattleSpecies;
import net.alex.guzhenren.entity.beast.ElephantEntity;
import net.alex.guzhenren.entity.beast.TigerEntity;
import net.alex.guzhenren.entity.beast.TortoiseEntity;
import net.alex.guzhenren.entity.beast.TortoiseSpecies;
import net.alex.guzhenren.entity.beast.WildBoarEntity;
import net.alex.guzhenren.entity.gu.FlyingGuEntity;
import net.alex.guzhenren.gameplay.trade.SoulTraderEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

/**
 * 本模组全部实体的默认属性绑定与自然生成放置规则注册入口。
 * <ul>
 *   <li>属性注册中，希望蛊[Hope Gu]使用 {@link FlyingGuEntity} 基础属性，豕蛊与犀牛甲虫蛊变体在其基础上将飞行速度提升至
 *       {@link #RESTING_GU_FLYING_SPEED}（因路径导航空速随该属性缩放，基础值下逃跑和降落几乎静止，而希望蛊悬停直接写速
 *       度故无需调高）；{@link SoulTraderEntity} 注册了属性但不设自然生成规则，仅通过指令召唤。</li>
 *   <li>生成放置注册中，飞行蛊家族统一绑定地表无限制放置（{@link SpawnRules#onTheSurface}），全部野兽绑定地面放置
 *       （{@link SpawnRules#onAnimalGround}），两者均采用 {@link Heightmap.Types#MOTION_BLOCKING_NO_LEAVES} 高度
 *       图。</li>
 * </ul>
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class EntityRegistrationEvents {

    private static final double RESTING_GU_FLYING_SPEED = 0.3D;

    private EntityRegistrationEvents() {}

    @SubscribeEvent
    public static void onCreateAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntityTypes.HOPE_GU_ENTITY.get(), FlyingGuEntity.createAttributes().build());
        event.put(ModEntityTypes.WHITE_BOAR_GU_ENTITY.get(), createRestingGuAttributes().build());
        event.put(ModEntityTypes.BLACK_BOAR_GU_ENTITY.get(), createRestingGuAttributes().build());
        event.put(ModEntityTypes.FLOWER_BOAR_GU_ENTITY.get(), createRestingGuAttributes().build());
        event.put(ModEntityTypes.HORIZONTAL_CRASH_GU_ENTITY.get(), createRestingGuAttributes().build());
        event.put(ModEntityTypes.VERTICAL_CRASH_GU_ENTITY.get(), createRestingGuAttributes().build());
        event.put(ModEntityTypes.CHARGING_CRASH_GU_4_ENTITY.get(), createRestingGuAttributes().build());
        event.put(ModEntityTypes.CHARGING_CRASH_GU_5_ENTITY.get(), createRestingGuAttributes().build());
        event.put(ModEntityTypes.WILD_BOAR.get(), WildBoarEntity.createAttributes().build());
        for (BearSpecies species : BearSpecies.values()) {
            event.put(createBearType(species), BearEntity.createAttributes(species).build());
        }
        event.put(ModEntityTypes.TIGER.get(), TigerEntity.createAttributes().build());
        event.put(ModEntityTypes.WHITE_TIGER.get(), TigerEntity.createAttributes().build());
        event.put(ModEntityTypes.TORTOISE.get(), TortoiseEntity.createAttributes(TortoiseSpecies.TORTOISE).build());
        event.put(ModEntityTypes.STONE_TORTOISE.get(),
                TortoiseEntity.createAttributes(TortoiseSpecies.STONE_TORTOISE).build());
        for (CattleSpecies species : CattleSpecies.values()) {
            event.put(createCattleType(species), CattleEntity.createAttributes().build());
        }
        event.put(ModEntityTypes.ELEPHANT.get(), ElephantEntity.createAttributes().build());
        event.put(ModEntityTypes.WHITE_ELEPHANT.get(), ElephantEntity.createAttributes().build());
        event.put(ModEntityTypes.TEST_TRADE_GU_IMMORTAL.get(), SoulTraderEntity.createAttributes().build());
    }

    private static EntityType<BearEntity> createBearType(BearSpecies species) {
        return switch (species) {
            case BROWN -> ModEntityTypes.BROWN_BEAR.get();
            case ASIAN_BLACK -> ModEntityTypes.ASIAN_BLACK_BEAR.get();
            case AMERICAN_BLACK -> ModEntityTypes.AMERICAN_BLACK_BEAR.get();
            case ALBINO -> ModEntityTypes.ALBINO_BEAR.get();
        };
    }

    private static EntityType<CattleEntity> createCattleType(CattleSpecies species) {
        return switch (species) {
            case YELLOW_OX -> ModEntityTypes.YELLOW_OX.get();
            case WATER_BUFFALO -> ModEntityTypes.WATER_BUFFALO.get();
            case AZURE_OX -> ModEntityTypes.AZURE_OX.get();
        };
    }

    private static AttributeSupplier.Builder createRestingGuAttributes() {
        return FlyingGuEntity.createAttributes().add(Attributes.FLYING_SPEED, RESTING_GU_FLYING_SPEED);
    }

    @SubscribeEvent
    public static void onRegisterSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        registerSurfaceSpawn(event, ModEntityTypes.HOPE_GU_ENTITY.get());
        registerSurfaceSpawn(event, ModEntityTypes.WHITE_BOAR_GU_ENTITY.get());
        registerSurfaceSpawn(event, ModEntityTypes.BLACK_BOAR_GU_ENTITY.get());
        registerSurfaceSpawn(event, ModEntityTypes.FLOWER_BOAR_GU_ENTITY.get());
        registerSurfaceSpawn(event, ModEntityTypes.HORIZONTAL_CRASH_GU_ENTITY.get());
        registerSurfaceSpawn(event, ModEntityTypes.VERTICAL_CRASH_GU_ENTITY.get());
        registerSurfaceSpawn(event, ModEntityTypes.CHARGING_CRASH_GU_4_ENTITY.get());
        registerSurfaceSpawn(event, ModEntityTypes.CHARGING_CRASH_GU_5_ENTITY.get());
        registerAnimalSpawn(event, ModEntityTypes.WILD_BOAR.get());
        registerAnimalSpawn(event, ModEntityTypes.BROWN_BEAR.get());
        registerAnimalSpawn(event, ModEntityTypes.ASIAN_BLACK_BEAR.get());
        registerAnimalSpawn(event, ModEntityTypes.AMERICAN_BLACK_BEAR.get());
        registerAnimalSpawn(event, ModEntityTypes.ALBINO_BEAR.get());
        registerAnimalSpawn(event, ModEntityTypes.TIGER.get());
        registerAnimalSpawn(event, ModEntityTypes.WHITE_TIGER.get());
        registerAnimalSpawn(event, ModEntityTypes.TORTOISE.get());
        registerAnimalSpawn(event, ModEntityTypes.STONE_TORTOISE.get());
        for (CattleSpecies species : CattleSpecies.values()) {
            registerAnimalSpawn(event, createCattleType(species));
        }
        registerAnimalSpawn(event, ModEntityTypes.ELEPHANT.get());
        registerAnimalSpawn(event, ModEntityTypes.WHITE_ELEPHANT.get());
    }

    private static <T extends Mob> void registerAnimalSpawn(RegisterSpawnPlacementsEvent event,
                                                            EntityType<T> type) {
        event.register(type, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (spawned, level, reason, pos, random) -> SpawnRules.onAnimalGround(level, pos),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private static <T extends Mob> void registerSurfaceSpawn(RegisterSpawnPlacementsEvent event, EntityType<T> type) {
        event.register(type, SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (spawned, level, reason, pos, random) -> SpawnRules.onTheSurface(level, pos),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
