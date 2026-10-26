package net.alex.guzhenren.entity;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.entity.beast.BearEntity;
import net.alex.guzhenren.entity.beast.BearSpecies;
import net.alex.guzhenren.entity.beast.TigerEntity;
import net.alex.guzhenren.entity.beast.WildBoarEntity;
import net.alex.guzhenren.entity.gu.FlyingGuEntity;
import net.alex.guzhenren.gameplay.trade.SoulTraderEntity;
import net.alex.guzhenren.registry.entity.ModEntityTypes;
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
 * Where this mod's entities declare their attributes and where they are allowed to spawn.
 *
 * <p>Hope Gu registers {@link net.alex.guzhenren.entity.gu.FlyingGuEntity}'s attributes; the boar Gu and
 * rhinoceros beetle Gu variants share them with the raised resting flying speed
 * ({@code RESTING_GU_FLYING_SPEED}). They flee and land through path navigation, whose airborne pace scales
 * with that attribute, and at the base flying speed they barely move; the Hope Gu's hover approach sets its
 * velocity directly, so the base speed suits it.
 *
 * <p>Flying Gu families use one surface spawn placement, the wild beasts one ground placement, both on
 * {@link net.minecraft.world.level.levelgen.Heightmap.Types#MOTION_BLOCKING_NO_LEAVES}; the checks themselves
 * live in {@link SpawnRules}. Soul traders get attributes but no placement: they are summoned only. Loader glue
 * apart from the attribute table: the events differ between the NeoForge 1.21.1 mainline and the Forge 1.20.1
 * port.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.entity.gu.FlyingGuEntity
 * @see SpawnRules
 * @since 1.0.0
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
    }

    private static <T extends Mob> void registerAnimalSpawn(RegisterSpawnPlacementsEvent event,
                                                            EntityType<T> type) {
        event.register(type, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                SpawnRules::onAnimalGround, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private static <T extends Mob> void registerSurfaceSpawn(RegisterSpawnPlacementsEvent event, EntityType<T> type) {
        event.register(type, SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                SpawnRules::onTheSurface, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
