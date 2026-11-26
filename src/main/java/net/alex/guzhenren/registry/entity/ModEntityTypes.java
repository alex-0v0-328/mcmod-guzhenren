package net.alex.guzhenren.registry.entity;

import java.util.function.Supplier;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.entity.beast.BearEntity;
import net.alex.guzhenren.entity.beast.BearSpecies;
import net.alex.guzhenren.entity.beast.BeastPhantomEntity;
import net.alex.guzhenren.entity.beast.CattleEntity;
import net.alex.guzhenren.entity.beast.CattleSpecies;
import net.alex.guzhenren.entity.beast.ElephantEntity;
import net.alex.guzhenren.entity.beast.TigerEntity;
import net.alex.guzhenren.entity.beast.TortoiseEntity;
import net.alex.guzhenren.entity.beast.TortoiseSpecies;
import net.alex.guzhenren.entity.beast.WildBoarEntity;
import net.alex.guzhenren.entity.gu.BoarGuEntity;
import net.alex.guzhenren.entity.gu.CrashGuEntity;
import net.alex.guzhenren.entity.gu.HopeGuEntity;
import net.alex.guzhenren.entity.projectile.BeastChargeEntity;
import net.alex.guzhenren.entity.projectile.MoonBladeEntity;
import net.alex.guzhenren.entity.projectile.QiWaveEntity;
import net.alex.guzhenren.gameplay.blissful.land.BlissfulLand;
import net.alex.guzhenren.gameplay.blissful.land.EarthSpiritEntity;
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
 * 本模组全部 {@link EntityType} 注册表，涵盖野生蛊、野兽、虚影、魂商与投射物。
 * <ul>
 *   <li>自然生成与战斗实体包括希望蛊（自带实体光点散发）、3 种豕蛊、4 种犀牛甲虫蛊变体，以及野猪、4 种熊、2 种虎、2 种
 *       龟、3 种牛与 2 种象等野兽（徒手右键即可捕获野生蛊且不以开窍[Awakening]为门槛，野猪不可捕获）；月刃、气
 *       波与兽力[Beast Strength]冲撞为投射物，兽力虚影（{@link BeastPhantomEntity}）仅作视觉展示而不持久化存档；每个
 *       {@link SoulTrader} 对应一个免疫火焰的杂项实体，无自然生成规则，仅支持 {@code /summon} 召唤；每个福地一个地灵
 *       （{@code earth_spirit_<福地 id>}，同魂商尺寸），由福地在行宫生成，不自然生成。</li>
 *   <li>⚠ 每个活体生物实体均须在 {@code assets/ftblibrary/textures/faces/guzhenren/} 下提供一份 {@code <实体 id>.json}
 *       或 {@code .png} 作为 FTB Chunks 小地图头像（两者并存时 FTB Library 仅读取 JSON，故只保留一份；缺失会导致开发环
 *       境打印 ERROR 且小地图退化为通用圆点，{@code MISC} 杂项实体未配置则不绘制）。</li>
 * </ul>
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
    private static final float TORTOISE_WIDTH = 1.3F;
    private static final float TORTOISE_HEIGHT = 0.8F;
    private static final float CATTLE_WIDTH = 1.7F;
    private static final float CATTLE_HEIGHT = 1.75F;
    private static final float ELEPHANT_WIDTH = 2.5F;
    private static final float ELEPHANT_HEIGHT = 3.7F;
    private static final float SOUL_TRADER_WIDTH = 0.6F;
    private static final float SOUL_TRADER_HEIGHT = 1.95F;
    private static final float MOON_BLADE_SIZE = 0.8F;
    private static final float QI_WAVE_SIZE = 0.5F;
    private static final float BEAST_CHARGE_SIZE = 0.5F;
    private static final float BEAST_PHANTOM_WIDTH = 1.0F;
    private static final float BEAST_PHANTOM_HEIGHT = 1.4F;
    private static final int BEAST_PHANTOM_UPDATE_TICKS = 20;
    private static final int PROJECTILE_TRACKING_CHUNKS = 4;
    private static final int PROJECTILE_UPDATE_TICKS = 10;
    //region Gu phantoms [蛊虫幻影]
    public static final DeferredHolder<EntityType<?>, EntityType<HopeGuEntity>> HOPE_GU_ENTITY =
            registerMote("hope_gu_entity", (type, level) -> new HopeGuEntity(type, level, ModItems.HOPE_GU));
    public static final DeferredHolder<EntityType<?>, EntityType<BoarGuEntity>> WHITE_BOAR_GU_ENTITY =
            registerBoarGu("white_boar_gu_entity", ModItems.WHITE_BOAR_GU_1);
    public static final DeferredHolder<EntityType<?>, EntityType<BoarGuEntity>> BLACK_BOAR_GU_ENTITY =
            registerBoarGu("black_boar_gu_entity", ModItems.BLACK_BOAR_GU_1);
    public static final DeferredHolder<EntityType<?>, EntityType<BoarGuEntity>> FLOWER_BOAR_GU_ENTITY =
            registerBoarGu("flower_boar_gu_entity", ModItems.FLOWER_BOAR_GU_1);
    public static final DeferredHolder<EntityType<?>, EntityType<CrashGuEntity>> HORIZONTAL_CRASH_GU_ENTITY =
            registerCrashGu("horizontal_crash_gu_entity", ModItems.HORIZONTAL_CRASH_GU_3);
    public static final DeferredHolder<EntityType<?>, EntityType<CrashGuEntity>> VERTICAL_CRASH_GU_ENTITY =
            registerCrashGu("vertical_crash_gu_entity", ModItems.VERTICAL_CRASH_GU_3);
    public static final DeferredHolder<EntityType<?>, EntityType<CrashGuEntity>> CHARGING_CRASH_GU_4_ENTITY =
            registerCrashGu("charging_crash_gu_4_entity", ModItems.CHARGING_CRASH_GU_4);
    public static final DeferredHolder<EntityType<?>, EntityType<CrashGuEntity>> CHARGING_CRASH_GU_5_ENTITY =
            registerCrashGu("charging_crash_gu_5_entity", ModItems.CHARGING_CRASH_GU_5);
    //endregion

    //region Beasts [野兽]
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
    public static final DeferredHolder<EntityType<?>, EntityType<TortoiseEntity>> TORTOISE =
            registerTortoise(TortoiseSpecies.TORTOISE);
    public static final DeferredHolder<EntityType<?>, EntityType<TortoiseEntity>> STONE_TORTOISE =
            registerTortoise(TortoiseSpecies.STONE_TORTOISE);
    public static final DeferredHolder<EntityType<?>, EntityType<CattleEntity>> YELLOW_OX =
            registerCattle(CattleSpecies.YELLOW_OX);
    public static final DeferredHolder<EntityType<?>, EntityType<CattleEntity>> WATER_BUFFALO =
            registerCattle(CattleSpecies.WATER_BUFFALO);
    public static final DeferredHolder<EntityType<?>, EntityType<CattleEntity>> AZURE_OX =
            registerCattle(CattleSpecies.AZURE_OX);
    public static final DeferredHolder<EntityType<?>, EntityType<ElephantEntity>> ELEPHANT =
            register("elephant", ElephantEntity::new, MobCategory.CREATURE, ELEPHANT_WIDTH, ELEPHANT_HEIGHT);
    public static final DeferredHolder<EntityType<?>, EntityType<ElephantEntity>> WHITE_ELEPHANT =
            register("white_elephant", ElephantEntity::new, MobCategory.CREATURE, ELEPHANT_WIDTH, ELEPHANT_HEIGHT);
    //endregion

    //region Phantoms [虚影]
    public static final DeferredHolder<EntityType<?>, EntityType<BeastPhantomEntity>> BEAST_PHANTOM =
            ENTITY_TYPES.register("beast_phantom", () -> EntityType.Builder.of(BeastPhantomEntity::new,
                            MobCategory.MISC)
                    .sized(BEAST_PHANTOM_WIDTH, BEAST_PHANTOM_HEIGHT)
                    .noSave()
                    .fireImmune()
                    .clientTrackingRange(TRACKING_CHUNKS)
                    .updateInterval(BEAST_PHANTOM_UPDATE_TICKS)
                    .build("beast_phantom"));
    //endregion

    //region Soul traders [魂商]
    public static final DeferredHolder<EntityType<?>, EntityType<SoulTraderEntity>> TEST_TRADE_GU_IMMORTAL =
            registerSoulTrader(SoulTrader.TEST_TRADE_GU_IMMORTAL);
    //endregion

    //region Earth spirits [地灵]
    public static final DeferredHolder<EntityType<?>, EntityType<EarthSpiritEntity>>
            EARTH_SPIRIT_BLISSFUL_LAND_FOX_IMMORTAL = registerEarthSpirit(BlissfulLand.FOX_IMMORTAL);
    //endregion

    //region Projectiles [投射物]
    public static final DeferredHolder<EntityType<?>, EntityType<MoonBladeEntity>> MOON_BLADE =
            ENTITY_TYPES.register("moon_blade", () -> EntityType.Builder.of(MoonBladeEntity::new,
                            MobCategory.MISC)
                    .sized(MOON_BLADE_SIZE, MOON_BLADE_SIZE)
                    .clientTrackingRange(PROJECTILE_TRACKING_CHUNKS)
                    .updateInterval(PROJECTILE_UPDATE_TICKS)
                    .build("moon_blade"));
    public static final DeferredHolder<EntityType<?>, EntityType<QiWaveEntity>> QI_WAVE =
            ENTITY_TYPES.register("qi_wave", () -> EntityType.Builder.of(QiWaveEntity::new,
                            MobCategory.MISC)
                    .sized(QI_WAVE_SIZE, QI_WAVE_SIZE)
                    .clientTrackingRange(PROJECTILE_TRACKING_CHUNKS)
                    .updateInterval(PROJECTILE_UPDATE_TICKS)
                    .build("qi_wave"));
    public static final DeferredHolder<EntityType<?>, EntityType<BeastChargeEntity>> BEAST_CHARGE =
            ENTITY_TYPES.register("beast_charge", () -> EntityType.Builder.of(BeastChargeEntity::new,
                            MobCategory.MISC)
                    .sized(BEAST_CHARGE_SIZE, BEAST_CHARGE_SIZE)
                    .noSave()
                    .fireImmune()
                    .clientTrackingRange(PROJECTILE_TRACKING_CHUNKS)
                    .updateInterval(PROJECTILE_UPDATE_TICKS)
                    .build("beast_charge"));
    //endregion

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

    private static DeferredHolder<EntityType<?>, EntityType<TortoiseEntity>> registerTortoise(
            TortoiseSpecies species) {
        return register(species.getId(), TortoiseEntity::new, MobCategory.CREATURE, TORTOISE_WIDTH, TORTOISE_HEIGHT);
    }

    private static DeferredHolder<EntityType<?>, EntityType<CattleEntity>> registerCattle(CattleSpecies species) {
        return register(species.getId(), (type, level) -> new CattleEntity(type, level, species), MobCategory.CREATURE,
                CATTLE_WIDTH, CATTLE_HEIGHT);
    }

    private static DeferredHolder<EntityType<?>, EntityType<SoulTraderEntity>> registerSoulTrader(SoulTrader trader) {
        return ENTITY_TYPES.register(trader.getId(), () -> EntityType.Builder
                .<SoulTraderEntity>of((type, level) -> new SoulTraderEntity(type, level, trader), MobCategory.MISC)
                .sized(SOUL_TRADER_WIDTH, SOUL_TRADER_HEIGHT)
                .fireImmune()
                .clientTrackingRange(TRACKING_CHUNKS)
                .build(trader.getId()));
    }

    private static DeferredHolder<EntityType<?>, EntityType<EarthSpiritEntity>> registerEarthSpirit(
            BlissfulLand land) {
        String id = "earth_spirit_" + land.id();
        return ENTITY_TYPES.register(id, () -> EntityType.Builder
                .<EarthSpiritEntity>of((type, level) -> new EarthSpiritEntity(type, level, land), MobCategory.MISC)
                .sized(SOUL_TRADER_WIDTH, SOUL_TRADER_HEIGHT)
                .fireImmune()
                .clientTrackingRange(TRACKING_CHUNKS)
                .build(id));
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
