package net.alex.guzhenren.registry.entity;

import java.util.function.Supplier;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.entity.BearEntity;
import net.alex.guzhenren.entity.BearSpecies;
import net.alex.guzhenren.entity.BoarGuEntity;
import net.alex.guzhenren.entity.HopeGuEntity;
import net.alex.guzhenren.entity.CrashGuEntity;
import net.alex.guzhenren.entity.TigerEntity;
import net.alex.guzhenren.entity.WildBoarEntity;
import net.alex.guzhenren.gameplay.trade.SoulTrader;
import net.alex.guzhenren.gameplay.trade.SoulTraderEntity;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.registries.Registries;
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
 * rule at all: soul traders exist only through {@code /summon}.
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
    public static final DeferredHolder<EntityType<?>, EntityType<HopeGuEntity>> HOPE_GU_ENTITY =
            ENTITY_TYPES.register("hope_gu_entity", () -> EntityType.Builder
                    .<HopeGuEntity>of((type, level) ->
                                    new HopeGuEntity(type, level, ModItems.HOPE_GU),
                            MobCategory.AMBIENT)
                    .sized(MOTE_WIDTH, MOTE_HEIGHT)
                    .clientTrackingRange(TRACKING_CHUNKS)
                    .build("hope_gu_entity"));
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
            ENTITY_TYPES.register("wild_boar", () -> EntityType.Builder
                    .of(WildBoarEntity::new, MobCategory.CREATURE)
                    .sized(WILD_BOAR_WIDTH, WILD_BOAR_HEIGHT)
                    .clientTrackingRange(TRACKING_CHUNKS)
                    .build("wild_boar"));
    public static final DeferredHolder<EntityType<?>, EntityType<BearEntity>> BROWN_BEAR =
            registerBear(BearSpecies.BROWN);
    public static final DeferredHolder<EntityType<?>, EntityType<BearEntity>> ASIAN_BLACK_BEAR =
            registerBear(BearSpecies.ASIAN_BLACK);
    public static final DeferredHolder<EntityType<?>, EntityType<BearEntity>> AMERICAN_BLACK_BEAR =
            registerBear(BearSpecies.AMERICAN_BLACK);
    public static final DeferredHolder<EntityType<?>, EntityType<BearEntity>> ALBINO_BEAR =
            registerBear(BearSpecies.ALBINO);
    public static final DeferredHolder<EntityType<?>, EntityType<TigerEntity>> TIGER =
            ENTITY_TYPES.register("tiger", () -> EntityType.Builder
                    .of(TigerEntity::new, MobCategory.CREATURE)
                    .sized(TIGER_WIDTH, TIGER_HEIGHT)
                    .clientTrackingRange(TRACKING_CHUNKS)
                    .build("tiger"));
    public static final DeferredHolder<EntityType<?>, EntityType<TigerEntity>> WHITE_TIGER =
            ENTITY_TYPES.register("white_tiger", () -> EntityType.Builder
                    .of(TigerEntity::new, MobCategory.CREATURE)
                    .sized(TIGER_WIDTH, TIGER_HEIGHT)
                    .clientTrackingRange(TRACKING_CHUNKS)
                    .build("white_tiger"));
    public static final DeferredHolder<EntityType<?>, EntityType<SoulTraderEntity>> TEST_TRADE_GU_IMMORTAL =
            registerSoulTrader(SoulTrader.TEST_TRADE_GU_IMMORTAL);

    private static DeferredHolder<EntityType<?>, EntityType<BearEntity>> registerBear(BearSpecies species) {
        return ENTITY_TYPES.register(species.id(), () -> EntityType.Builder
                .<BearEntity>of((type, level) -> new BearEntity(type, level, species), MobCategory.CREATURE)
                .sized(BEAR_WIDTH, BEAR_HEIGHT)
                .clientTrackingRange(TRACKING_CHUNKS)
                .build(species.id()));
    }

    private static DeferredHolder<EntityType<?>, EntityType<SoulTraderEntity>> registerSoulTrader(SoulTrader trader) {
        return ENTITY_TYPES.register(trader.id(), () -> EntityType.Builder
                .<SoulTraderEntity>of((type, level) -> new SoulTraderEntity(type, level, trader), MobCategory.MISC)
                .sized(SOUL_TRADER_WIDTH, SOUL_TRADER_HEIGHT)
                .fireImmune()
                .clientTrackingRange(TRACKING_CHUNKS)
                .build(trader.id()));
    }

    private static DeferredHolder<EntityType<?>, EntityType<BoarGuEntity>> registerBoarGu(
            String name, Supplier<Item> caughtGu) {
        return ENTITY_TYPES.register(name, () -> EntityType.Builder
                .<BoarGuEntity>of((type, level) -> new BoarGuEntity(type, level, caughtGu), MobCategory.AMBIENT)
                .sized(MOTE_WIDTH, MOTE_HEIGHT)
                .clientTrackingRange(TRACKING_CHUNKS)
                .build(name));
    }

    private static DeferredHolder<EntityType<?>, EntityType<CrashGuEntity>> registerCrashGu(
            String name, Supplier<Item> caughtGu) {
        return ENTITY_TYPES.register(name, () -> EntityType.Builder
                .<CrashGuEntity>of((type, level) -> new CrashGuEntity(type, level, caughtGu),
                        MobCategory.AMBIENT)
                .sized(MOTE_WIDTH, MOTE_HEIGHT)
                .clientTrackingRange(TRACKING_CHUNKS)
                .build(name));
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}
