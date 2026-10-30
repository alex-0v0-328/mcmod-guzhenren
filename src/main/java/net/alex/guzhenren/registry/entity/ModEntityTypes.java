package net.alex.guzhenren.registry.entity;

import java.util.function.Supplier;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.entity.beast.BearEntity;
import net.alex.guzhenren.entity.beast.BearSpecies;
import net.alex.guzhenren.entity.beast.TigerEntity;
import net.alex.guzhenren.entity.beast.WildBoarEntity;
import net.alex.guzhenren.entity.gu.BoarGuEntity;
import net.alex.guzhenren.entity.gu.CrashGuEntity;
import net.alex.guzhenren.entity.gu.HopeGuEntity;
import net.alex.guzhenren.entity.projectile.MoonBladeEntity;
import net.alex.guzhenren.gameplay.trade.SoulTrader;
import net.alex.guzhenren.gameplay.trade.SoulTraderEntity;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The entity types this mod registers.
 *
 * <p>Hope Gu, three boar Gu and four rhinoceros beetle Gu variants are naturally spawning ambient entities;
 * the wild boar, the four bears and the two tigers are naturally spawning creatures. Hope Gu's client mote
 * is emitted by its entity class. Each {@link SoulTrader} registers one fire-immune misc entity with no spawn
 * rule at all: soul traders exist only through {@code /summon}. The Moon Blade [月刃] is the one projectile.
 * Gu capture is a bare right click and is never gated on awakening [开窍]; the wild boar has no capture path.
 *
 * @author Alex
 * @version 1.0.0
 * @see HopeGuEntity
 * @since 1.0.0
 */

public final class ModEntityTypes {

    private ModEntityTypes() {}

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Guzhenren.MOD_ID);
    private static final float MOTE_WIDTH = 0.4F;
    private static final float MOTE_HEIGHT = 0.4F;
    private static final int TRACKING_CHUNKS = 8;
    private static final float WILD_BOAR_WIDTH = 1.1F;
    private static final float WILD_BOAR_HEIGHT = 1.2F;
    private static final float BEAR_WIDTH = 1.2F;
    private static final float BEAR_HEIGHT = 1.4F;
    private static final float TIGER_WIDTH = 1.3F;
    private static final float TIGER_HEIGHT = 1.4F;
    private static final float SOUL_TRADER_WIDTH = 0.6F;
    private static final float SOUL_TRADER_HEIGHT = 1.95F;
    private static final float MOON_BLADE_SIZE = 0.8F;
    private static final int PROJECTILE_TRACKING_CHUNKS = 4;
    private static final int PROJECTILE_UPDATE_TICKS = 10;
    public static final DeferredHolder<EntityType<?>, EntityType<HopeGuEntity>> HOPE_GU_ENTITY =
            registerMote("hope_gu_entity", (type, level) -> new HopeGuEntity(type, level, ModItems.HOPE_GU));
    public static final DeferredHolder<EntityType<?>, EntityType<BoarGuEntity>> WHITE_BOAR_GU_ENTITY =
            registerBoarGu("white_boar_gu_entity", ModItems.WHITE_BOAR_GU);
    public static final DeferredHolder<EntityType<?>, EntityType<BoarGuEntity>> BLACK_BOAR_GU_ENTITY =
            registerBoarGu("black_boar_gu_entity", ModItems.BLACK_BOAR_GU);
    public static final DeferredHolder<EntityType<?>, EntityType<BoarGuEntity>> FLOWER_BOAR_GU_ENTITY =
            registerBoarGu("flower_boar_gu_entity", ModItems.FLOWER_BOAR_GU);
    public static final DeferredHolder<EntityType<?>, EntityType<CrashGuEntity>> HORIZONTAL_CRASH_GU_ENTITY =
            registerCrashGu("horizontal_crash_gu_entity", ModItems.HORIZONTAL_CRASH_GU);
    public static final DeferredHolder<EntityType<?>, EntityType<CrashGuEntity>> VERTICAL_CRASH_GU_ENTITY =
            registerCrashGu("vertical_crash_gu_entity", ModItems.VERTICAL_CRASH_GU);
    public static final DeferredHolder<EntityType<?>, EntityType<CrashGuEntity>> CHARGING_CRASH_GU_4_ENTITY =
            registerCrashGu("charging_crash_gu_4_entity", ModItems.CHARGING_CRASH_GU_4);
    public static final DeferredHolder<EntityType<?>, EntityType<CrashGuEntity>> CHARGING_CRASH_GU_5_ENTITY =
            registerCrashGu("charging_crash_gu_5_entity", ModItems.CHARGING_CRASH_GU_5);
    public static final DeferredHolder<EntityType<?>, EntityType<WildBoarEntity>> WILD_BOAR =
            register("wild_boar", WildBoarEntity::new, MobCategory.CREATURE, WILD_BOAR_WIDTH, WILD_BOAR_HEIGHT);
    public static final DeferredHolder<EntityType<?>, EntityType<BearEntity>> BROWN_BEAR =
            registerBear(BearSpecies.BROWN);
    public static final DeferredHolder<EntityType<?>, EntityType<BearEntity>> ASIAN_BLACK_BEAR =
            registerBear(BearSpecies.ASIAN_BLACK);
    public static final DeferredHolder<EntityType<?>, EntityType<BearEntity>> AMERICAN_BLACK_BEAR =
            registerBear(BearSpecies.AMERICAN_BLACK);
    public static final DeferredHolder<EntityType<?>, EntityType<BearEntity>> ALBINO_BEAR =
            registerBear(BearSpecies.ALBINO);
    public static final DeferredHolder<EntityType<?>, EntityType<TigerEntity>> TIGER =
            register("tiger", TigerEntity::new, MobCategory.CREATURE, TIGER_WIDTH, TIGER_HEIGHT);
    public static final DeferredHolder<EntityType<?>, EntityType<TigerEntity>> WHITE_TIGER =
            register("white_tiger", TigerEntity::new, MobCategory.CREATURE, TIGER_WIDTH, TIGER_HEIGHT);
    public static final DeferredHolder<EntityType<?>, EntityType<SoulTraderEntity>> TEST_TRADE_GU_IMMORTAL =
            registerSoulTrader(SoulTrader.TEST_TRADE_GU_IMMORTAL);

    public static final DeferredHolder<EntityType<?>, EntityType<MoonBladeEntity>> MOON_BLADE =
            ENTITY_TYPES.register("moon_blade", () -> EntityType.Builder.<MoonBladeEntity>of(MoonBladeEntity::new,
                            MobCategory.MISC)
                    .sized(MOON_BLADE_SIZE, MOON_BLADE_SIZE)
                    .clientTrackingRange(PROJECTILE_TRACKING_CHUNKS)
                    .updateInterval(PROJECTILE_UPDATE_TICKS)
                    .build("moon_blade"));

    private static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> register(
            String id, EntityType.EntityFactory<T> factory, MobCategory category, float width, float height) {
        return ENTITY_TYPES.register(id, () -> EntityType.Builder.of(factory, category)
                .sized(width, height)
                .clientTrackingRange(TRACKING_CHUNKS)
                .build(id));
    }

    private static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> registerMote(
            String id, EntityType.EntityFactory<T> factory) {
        return register(id, factory, MobCategory.AMBIENT, MOTE_WIDTH, MOTE_HEIGHT);
    }

    private static DeferredHolder<EntityType<?>, EntityType<BearEntity>> registerBear(BearSpecies species) {
        return register(species.getId(), (type, level) -> new BearEntity(type, level, species), MobCategory.CREATURE,
                BEAR_WIDTH, BEAR_HEIGHT);
    }

    private static DeferredHolder<EntityType<?>, EntityType<SoulTraderEntity>> registerSoulTrader(SoulTrader trader) {
        return ENTITY_TYPES.register(trader.getId(), () -> EntityType.Builder
                .<SoulTraderEntity>of((type, level) -> new SoulTraderEntity(type, level, trader), MobCategory.MISC)
                .sized(SOUL_TRADER_WIDTH, SOUL_TRADER_HEIGHT)
                .fireImmune()
                .clientTrackingRange(TRACKING_CHUNKS)
                .build(trader.getId()));
    }

    private static DeferredHolder<EntityType<?>, EntityType<BoarGuEntity>> registerBoarGu(
            String id, Supplier<Item> caughtGu) {
        return registerMote(id, (type, level) -> new BoarGuEntity(type, level, caughtGu));
    }

    private static DeferredHolder<EntityType<?>, EntityType<CrashGuEntity>> registerCrashGu(
            String id, Supplier<Item> caughtGu) {
        return registerMote(id, (type, level) -> new CrashGuEntity(type, level, caughtGu));
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}
