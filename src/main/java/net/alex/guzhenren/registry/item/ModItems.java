package net.alex.guzhenren.registry.item;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.effect.timed.BruteForceLonghornBeetleGuEffect;
import net.alex.guzhenren.effect.timed.CrashGuEffect;
import net.alex.guzhenren.effect.timed.DragonpillCricketGuEffect;
import net.alex.guzhenren.effect.timed.FlowerBoarGuEffect;
import net.alex.guzhenren.effect.timed.HardshipStrengthGuEffect;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.path.qi.QiKind;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.HumanStrength;
import net.alex.guzhenren.gameplay.path.strength.StrengthPathBranch;
import net.alex.guzhenren.item.gu.GuSpec;
import net.alex.guzhenren.item.gu.mortal.BuffGuItem;
import net.alex.guzhenren.item.gu.mortal.earth.StoneApertureGuItem;
import net.alex.guzhenren.item.gu.mortal.food.LiquorWormItem;
import net.alex.guzhenren.item.gu.mortal.heaven.LifespanGuItem;
import net.alex.guzhenren.item.gu.mortal.heaven.RelicsGuItem;
import net.alex.guzhenren.item.gu.mortal.human.HopeGuItem;
import net.alex.guzhenren.item.gu.mortal.human.SecondApertureGuItem;
import net.alex.guzhenren.item.gu.mortal.soul.GutsGuItem;
import net.alex.guzhenren.item.gu.mortal.space.PrimevalElderGuItem;
import net.alex.guzhenren.item.gu.mortal.strength.AllOutEffortGuItem;
import net.alex.guzhenren.item.gu.mortal.strength.BeastStrengthGuItem;
import net.alex.guzhenren.item.gu.mortal.strength.HumanStrengthGuItem;
import net.alex.guzhenren.item.gu.mortal.strength.SelfRelianceGuItem;
import net.alex.guzhenren.item.gu.mortal.time.WatchGuItem;
import net.alex.guzhenren.item.gu.mortal.transformation.ZombieGuItem;
import net.alex.guzhenren.item.gu.mortal.wisdom.CasualGuItem;
import net.alex.guzhenren.item.gu.mortal.wisdom.MaliciousThoughtGuItem;
import net.alex.guzhenren.item.gu.mortal.wood.NineLeafVitalityGrassItem;
import net.alex.guzhenren.item.gu.mortal.wood.TreasureLotusGuItem;
import net.alex.guzhenren.item.gu.mortal.wood.VitalityLeafGuItem;
import net.alex.guzhenren.item.material.GuMaterialItem;
import net.alex.guzhenren.item.material.LiquorItem;
import net.alex.guzhenren.item.material.PrimevalStoneItem;
import net.alex.guzhenren.item.material.qi.DeathQiItem;
import net.alex.guzhenren.item.material.qi.LifeQiItem;
import net.alex.guzhenren.item.material.qi.QiMaterialItem;
import net.alex.guzhenren.registry.block.ModBlocks;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

/**
 * Every item and the only place a Gu's numbers actually live.
 *
 * <p>DeferredRegister holder: the {@link GuSpec} chain on each registration, or the spec helper its family
 * shares, IS the truth. A figure
 * written down anywhere else is a copy of it, and when the two disagree this file is the one that is
 * right. One line per item, plus the PNG and both lang keys.
 *
 * <p>⚠ The registration id mirrors the Java name (and the PNG); a rank ladder is numbered
 * ({@code sword_qi_1..5}), but an item with its own fiction name keeps that name ({@code blood_wight_gu}).
 *
 * <p>{@link #SPIRIT_SPRING} is the spring's {@code BlockItem}: same key as the block, answers to
 * the block's lang entry, and its icon is the fluid's still strip (see
 * {@code ModItemModelProvider}) -- no item PNG of its own.
 *
 * @author Alex
 * @version 1.0.0
 * @see GuSpec
 * @since 1.0.0
 */

public final class ModItems {

    private ModItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guzhenren.MOD_ID);
    private static final long PRIMEVAL_STONE_ESSENCE = 20L;
    private static final long[] QI_ESSENCE_COST = { 50L, 500L, 5_000L, 50_000L, 500_000L };

    private static Item.Properties tendedProperties() { return new Item.Properties().stacksTo(1); }

    private static Item.Properties oneShotProperties() { return new Item.Properties(); }

    //region One-shot Gu [一次性] -- refining IS the use; one charged press pays, lands and spends
    public static final DeferredItem<Item> HOPE_GU = ITEMS.register("hope_gu",
            () -> new HopeGuItem(oneShotProperties().stacksTo(1), GuSpec.of(Rank.ONE, GuPath.HUMAN)));
    public static final DeferredItem<Item> VITALITY_LEAF_GU = ITEMS.register("vitality_leaf_gu",
            () -> new VitalityLeafGuItem(oneShotProperties(), GuSpec.of(Rank.ONE, GuPath.WOOD)));
    public static final DeferredItem<Item> LIFESPAN_GU = ITEMS.register("lifespan_gu",
            () -> new LifespanGuItem(oneShotProperties(), 1, 9, GuSpec.of(Rank.ONE, GuPath.HEAVEN)));
    public static final DeferredItem<Item> TENS_LIFESPAN_GU = ITEMS.register("tens_lifespan_gu",
            () -> new LifespanGuItem(oneShotProperties(), 10, 19, GuSpec.of(Rank.ONE, GuPath.HEAVEN)));
    public static final DeferredItem<Item> HUNDREDS_LIFESPAN_GU = ITEMS.register("hundreds_lifespan_gu",
            () -> new LifespanGuItem(oneShotProperties(), 100, 199, GuSpec.of(Rank.ONE, GuPath.HEAVEN)));
    public static final DeferredItem<Item> THOUSANDS_LIFESPAN_GU = ITEMS.register("thousands_lifespan_gu",
            () -> new LifespanGuItem(oneShotProperties(), 1000, 1999, GuSpec.of(Rank.ONE, GuPath.HEAVEN)));
    public static final DeferredItem<Item> COPPER_RELICS_GU = ITEMS.register("copper_relics_gu",
            () -> new RelicsGuItem(oneShotProperties(), GuSpec.of(Rank.ONE, GuPath.HEAVEN)));
    public static final DeferredItem<Item> STEEL_RELICS_GU = ITEMS.register("steel_relics_gu",
            () -> new RelicsGuItem(oneShotProperties(), GuSpec.of(Rank.TWO, GuPath.HEAVEN)));
    public static final DeferredItem<Item> SILVER_RELICS_GU = ITEMS.register("silver_relics_gu",
            () -> new RelicsGuItem(oneShotProperties(), GuSpec.of(Rank.THREE, GuPath.HEAVEN)));
    public static final DeferredItem<Item> GOLD_RELICS_GU = ITEMS.register("gold_relics_gu",
            () -> new RelicsGuItem(oneShotProperties(), GuSpec.of(Rank.FOUR, GuPath.HEAVEN)));
    public static final DeferredItem<Item> CRYSTAL_RELICS_GU = ITEMS.register("crystal_relics_gu",
            () -> new RelicsGuItem(oneShotProperties(), GuSpec.of(Rank.FIVE, GuPath.HEAVEN)));
    //endregion

    //region Beast Strength Phantom Branch [兽力虚影流]
    public static final DeferredItem<Item> WHITE_BOAR_GU = ITEMS.register("white_boar_gu",
            () -> new BeastStrengthGuItem(tendedProperties(), BeastStrength.WHITE_BOAR,
                    createBeastStrengthSpec(ModItemTags.BOAR_FEED)));
    public static final DeferredItem<Item> BLACK_BOAR_GU = ITEMS.register("black_boar_gu",
            () -> new BeastStrengthGuItem(tendedProperties(), BeastStrength.BLACK_BOAR,
                    createBeastStrengthSpec(ModItemTags.BOAR_FEED)));
    public static final DeferredItem<Item> BEAR_STRENGTH_GU = ITEMS.register("bear_strength_gu",
            () -> new BeastStrengthGuItem(tendedProperties(), BeastStrength.BEAR,
                    createBeastStrengthSpec(ModItemTags.BEAR_FEED)));
    public static final DeferredItem<Item> FLOWER_BOAR_GU = ITEMS.register("flower_boar_gu",
            () -> new BuffGuItem(tendedProperties(), ModEffects.FLOWER_BOAR_GU, FlowerBoarGuEffect.DURATION_TICKS,
                    createBeastBuffSpec(ModItemTags.BOAR_FEED)));
    public static final DeferredItem<Item> DRAGONPILL_CRICKET_GU = ITEMS.register("dragonpill_cricket_gu",
            () -> new BuffGuItem(tendedProperties(), ModEffects.DRAGONPILL_CRICKET_GU,
                    DragonpillCricketGuEffect.DURATION_TICKS, createBeastBuffSpec(ModItemTags.RABBIT_FEED)));
    public static final DeferredItem<Item> BRUTE_FORCE_LONGHORN_BEETLE_GU =
            ITEMS.register("brute_force_longhorn_beetle_gu",
                    () -> new BuffGuItem(tendedProperties(), ModEffects.BRUTE_FORCE_LONGHORN_BEETLE_GU,
                            BruteForceLonghornBeetleGuEffect.DURATION_TICKS,
                            createBeastBuffSpec(ModItemTags.BEEF_FEED)));

    private static GuSpec createBeastStrengthSpec(TagKey<Item> feed) {
        return GuSpec.of(Rank.ONE, GuPath.STRENGTH)
                .strengthPathBranch(StrengthPathBranch.BEAST_STRENGTH_PHANTOM)
                .refine(800)
                .channel(3_600)
                .hungerBar(36, 1).essencePerHunger(100)
                .feed(feed, 1);
    }

    private static GuSpec createBeastBuffSpec(TagKey<Item> feed) {
        return GuSpec.of(Rank.ONE, GuPath.STRENGTH)
                .strengthPathBranch(StrengthPathBranch.BEAST_STRENGTH_PHANTOM)
                .refine(800)
                .costPerUse(16)
                .hungerBar(12, 1).hungerPerUse(4)
                .feed(feed, 1)
                .cooldown(30 * Ticks.SECOND, Ticks.SECOND);
    }
    //endregion

    //region Normal [基础力道] -- Strength Path Gu outside the three specialized branches
    public static final DeferredItem<Item> HORIZONTAL_CRASH_GU = ITEMS.register("horizontal_crash_gu",
            () -> new BuffGuItem(tendedProperties(), ModEffects.HORIZONTAL_CRASH_GU, CrashGuEffect.duration(30),
                    GuSpec.of(Rank.THREE, GuPath.STRENGTH)
                            .refine(80_000).costPerUse(1_600)
                            .hungerBar(12, 1).hungerPerUse(4).feed(ModItemTags.ANVIL_FEED, 1)
                            .cooldown(30 * Ticks.SECOND, Ticks.SECOND)));
    public static final DeferredItem<Item> VERTICAL_CRASH_GU = ITEMS.register("vertical_crash_gu",
            () -> new BuffGuItem(tendedProperties(), ModEffects.VERTICAL_CRASH_GU, CrashGuEffect.duration(30),
                    GuSpec.of(Rank.THREE, GuPath.STRENGTH)
                            .refine(80_000).costPerUse(1_600)
                            .hungerBar(12, 1).hungerPerUse(4).feed(ModItemTags.ANVIL_FEED, 1)
                            .cooldown(30 * Ticks.SECOND, Ticks.SECOND)));
    public static final DeferredItem<Item> CHARGING_CRASH_GU_4 = ITEMS.register("charging_crash_gu_4",
            () -> new BuffGuItem(tendedProperties(), ModEffects.CHARGING_CRASH_GU,
                    CrashGuEffect.duration(30), 3, GuSpec.of(Rank.FOUR, GuPath.STRENGTH)
                    .refine(800_000).costPerUse(16_000)
                    .hungerBar(12, 2).hungerPerUse(4).feed(ModItemTags.ANVIL_FEED, 1)
                    .cooldown(30 * Ticks.SECOND, Ticks.SECOND)));
    public static final DeferredItem<Item> CHARGING_CRASH_GU_5 = ITEMS.register("charging_crash_gu_5",
            () -> new BuffGuItem(tendedProperties(), ModEffects.CHARGING_CRASH_GU,
                    CrashGuEffect.duration(60), 4, GuSpec.of(Rank.FIVE, GuPath.STRENGTH)
                    .refine(8_000_000).costPerUse(160_000)
                    .hungerBar(12, 3).hungerPerUse(4).feed(ModItemTags.ANVIL_FEED, 1)
                    .cooldown(30 * Ticks.SECOND, Ticks.SECOND)));
    public static final DeferredItem<Item> SELF_RELIANCE_GU_2 = ITEMS.register("self_reliance_gu_2",
            () -> new SelfRelianceGuItem(tendedProperties(), 30 * Ticks.SECOND, 1,
                    GuSpec.of(Rank.TWO, GuPath.STRENGTH)
                            .refine(8_000).costPerUse(160)
                            .hungerBar(12, 4).hungerPerUse(4).feed(ModItemTags.COBBLESTONE_FEED, 1)
                            .cooldown(30 * Ticks.SECOND, Ticks.SECOND)));
    public static final DeferredItem<Item> SELF_RELIANCE_GU_3 = ITEMS.register("self_reliance_gu_3",
            () -> new SelfRelianceGuItem(tendedProperties(), 60 * Ticks.SECOND, 2,
                    GuSpec.of(Rank.THREE, GuPath.STRENGTH)
                            .refine(80_000).costPerUse(1_600)
                            .hungerBar(12, 8).hungerPerUse(4).feed(ModItemTags.COBBLESTONE_FEED, 1)
                            .cooldown(30 * Ticks.SECOND, Ticks.SECOND)));
    public static final DeferredItem<Item> SELF_RELIANCE_GU_4 = ITEMS.register("self_reliance_gu_4",
            () -> new SelfRelianceGuItem(tendedProperties(), 120 * Ticks.SECOND, 3,
                    GuSpec.of(Rank.FOUR, GuPath.STRENGTH)
                            .refine(800_000).costPerUse(16_000)
                            .hungerBar(12, 12).hungerPerUse(4).feed(ModItemTags.COBBLESTONE_FEED, 1)
                            .cooldown(30 * Ticks.SECOND, Ticks.SECOND)));
    public static final DeferredItem<Item> HARDSHIP_STRENGTH_GU = ITEMS.register("hardship_strength_gu",
            () -> new BuffGuItem(tendedProperties(), ModEffects.HARDSHIP_STRENGTH_GU,
                    HardshipStrengthGuEffect.DURATION_TICKS,
                    GuSpec.of(Rank.FOUR, GuPath.STRENGTH)
                            .refine(800_000).costPerUse(16_000)
                            .hungerBar(12, 12).hungerPerUse(4).feed(ModItemTags.POTATO_FEED, 1)
                            .cooldown(30 * Ticks.SECOND, Ticks.SECOND)));
    //endregion

    //region Human Jun Strength Branch [人力钧力流] -- one round is one layer
    public static final DeferredItem<Item> JIN_STRENGTH_GU = ITEMS.register("jin_strength_gu",
            () -> new HumanStrengthGuItem(tendedProperties(), HumanStrength.JIN,
                    GuSpec.of(Rank.ONE, GuPath.STRENGTH)
                            .strengthPathBranch(StrengthPathBranch.HUMAN_JUN_STRENGTH)
                            .refine(800)
                            .channel(3_600)
                            .hungerBar(36, 1).essencePerHunger(300)
                            .feed(ModItemTags.JIN_FEED, 1)));
    public static final DeferredItem<Item> TENS_JIN_STRENGTH_GU = ITEMS.register("tens_jin_strength_gu",
            () -> new HumanStrengthGuItem(tendedProperties(), HumanStrength.TEN_JIN,
                    GuSpec.of(Rank.TWO, GuPath.STRENGTH)
                            .strengthPathBranch(StrengthPathBranch.HUMAN_JUN_STRENGTH)
                            .refine(8_000)
                            .channel(36_000)
                            .hungerBar(36, 2).essencePerHunger(3_000)
                            .feed(ModItemTags.JIN_FEED, 1)));
    public static final DeferredItem<Item> JUN_STRENGTH_GU = ITEMS.register("jun_strength_gu",
            () -> new HumanStrengthGuItem(tendedProperties(), HumanStrength.JUN,
                    GuSpec.of(Rank.THREE, GuPath.STRENGTH)
                            .strengthPathBranch(StrengthPathBranch.HUMAN_JUN_STRENGTH)
                            .refine(80_000)
                            .channel(36_000)
                            .hungerBar(36, 4).essencePerHunger(6_000)
                            .feed(ModItemTags.JIN_FEED_SMELTED, 1)));
    public static final DeferredItem<Item> TENS_JUN_STRENGTH_GU = ITEMS.register("tens_jun_strength_gu",
            () -> new HumanStrengthGuItem(tendedProperties(), HumanStrength.TEN_JUN,
                    GuSpec.of(Rank.FOUR, GuPath.STRENGTH)
                            .strengthPathBranch(StrengthPathBranch.HUMAN_JUN_STRENGTH)
                            .refine(800_000)
                            .channel(360_000)
                            .hungerBar(36, 8).essencePerHunger(60_000)
                            .feed(ModItemTags.JIN_FEED_SMELTED, 1)));
    //endregion

    //region Normal [基础力道] -- All-Out Effort Gu unlocks a stockpiled 9999 jin
    public static final DeferredItem<Item> ALL_OUT_EFFORT_GU_3 = ITEMS.register("all_out_effort_gu_3",
            () -> new AllOutEffortGuItem(tendedProperties(), 60, GuSpec.of(Rank.THREE, GuPath.STRENGTH)
                    .refine(80_000)
                    .costPerUse(1_600)
                    .hungerBar(12, 4).hungerPerUse(4)
                    .feed(ModItemTags.ALL_OUT_FEED, 5)
                    .cooldown(80 * Ticks.SECOND)));
    public static final DeferredItem<Item> ALL_OUT_EFFORT_GU_4 = ITEMS.register("all_out_effort_gu_4",
            () -> new AllOutEffortGuItem(tendedProperties(), 90, GuSpec.of(Rank.FOUR, GuPath.STRENGTH)
                    .refine(800_000)
                    .costPerUse(16_000)
                    .hungerBar(12, 8).hungerPerUse(4)
                    .feed(ModItemTags.ALL_OUT_FEED, 5)
                    .cooldown(100 * Ticks.SECOND)));
    public static final DeferredItem<Item> ALL_OUT_EFFORT_GU_5 = ITEMS.register("all_out_effort_gu_5",
            () -> new AllOutEffortGuItem(tendedProperties(), 120, GuSpec.of(Rank.FIVE, GuPath.STRENGTH)
                    .refine(8_000_000)
                    .costPerUse(160_000)
                    .hungerBar(12, 16).hungerPerUse(4)
                    .feed(ModItemTags.ALL_OUT_FEED, 5)
                    .cooldown(120 * Ticks.SECOND)));
    //endregion

    //region Liquor Worm [酒虫] -- hunger bar 8 with 3 per use, and only its own rank can drive it
    public static final DeferredItem<Item> LIQUOR_WORM = ITEMS.register("liquor_worm",
            () -> new LiquorWormItem(tendedProperties(), GuSpec.of(Rank.ONE, GuPath.FOOD)
                    .refine(1_600).costPerUse(16)
                    .hungerBar(8, 1).hungerPerUse(3).feed(ModItemTags.LIQUOR_FEED, 1)));
    public static final DeferredItem<Item> FOUR_FLAVORS_LIQUOR_WORM = ITEMS.register("four_flavors_liquor_worm",
            () -> new LiquorWormItem(tendedProperties(), GuSpec.of(Rank.TWO, GuPath.FOOD)
                    .refine(16_000).costPerUse(160)
                    .hungerBar(8, 2).hungerPerUse(3).feed(ModItemTags.LIQUOR_FEED, 1)));
    public static final DeferredItem<Item> SEVEN_FRAGRANCES_LIQUOR_WORM = ITEMS.register(
            "seven_fragrances_liquor_worm",
            () -> new LiquorWormItem(tendedProperties(), GuSpec.of(Rank.THREE, GuPath.FOOD)
                    .refine(160_000).costPerUse(1_600)
                    .hungerBar(8, 4).hungerPerUse(3).feed(ModItemTags.LIQUOR_FEED, 1)));
    public static final DeferredItem<Item> NINE_EYES_LIQUOR_WORM = ITEMS.register("nine_eyes_liquor_worm",
            () -> new LiquorWormItem(tendedProperties(), GuSpec.of(Rank.FOUR, GuPath.FOOD)
                    .refine(1_600_000).costPerUse(16_000)
                    .hungerBar(8, 8).hungerPerUse(3).feed(ModItemTags.LIQUOR_FEED, 1)));
    //endregion

    //region Primeval Elder Gu [元老蛊] -- a vault for Primeval Stones [元石] that never needs feeding at all
    public static final DeferredItem<Item> PRIMEVAL_ELDER_GU_1 = ITEMS.register("primeval_elder_gu_1",
            () -> new PrimevalElderGuItem(tendedProperties(), 1_000L, GuSpec.of(Rank.ONE, GuPath.SPACE)
                    .refine(16).costPerUse(0)));
    public static final DeferredItem<Item> PRIMEVAL_ELDER_GU_2 = ITEMS.register("primeval_elder_gu_2",
            () -> new PrimevalElderGuItem(tendedProperties(), 10_000L, GuSpec.of(Rank.TWO, GuPath.SPACE)
                    .refine(160).costPerUse(0)));
    public static final DeferredItem<Item> PRIMEVAL_ELDER_GU_3 = ITEMS.register("primeval_elder_gu_3",
            () -> new PrimevalElderGuItem(tendedProperties(), 100_000L, GuSpec.of(Rank.THREE, GuPath.SPACE)
                    .refine(1_600).costPerUse(0)));
    public static final DeferredItem<Item> PRIMEVAL_ELDER_GU_4 = ITEMS.register("primeval_elder_gu_4",
            () -> new PrimevalElderGuItem(tendedProperties(), 1_000_000L, GuSpec.of(Rank.FOUR, GuPath.SPACE)
                    .refine(16_000).costPerUse(0)));
    public static final DeferredItem<Item> PRIMEVAL_ELDER_GU_5 = ITEMS.register("primeval_elder_gu_5",
            () -> new PrimevalElderGuItem(tendedProperties(), 100_000_000L, GuSpec.of(Rank.FIVE, GuPath.SPACE)
                    .refine(160_000).costPerUse(0)));
    //endregion

    //region Heavenly Essence Treasure Lotus Gu [天元宝莲] -- wood path; 5% essence per second and minted stones
    public static final DeferredItem<Item> HEAVENLY_ESSENCE_TREASURE_LOTUS_GU = ITEMS.register(
            "heavenly_essence_treasure_lotus_gu",
            () -> new TreasureLotusGuItem(tendedProperties(), 1, 100, GuSpec.of(Rank.THREE, GuPath.WOOD)
                    .refine(80_000).costPerUse(0)));
    public static final DeferredItem<Item> HEAVENLY_ESSENCE_TREASURE_MONARCH_LOTUS_GU = ITEMS.register(
            "heavenly_essence_treasure_monarch_lotus_gu",
            () -> new TreasureLotusGuItem(tendedProperties(), 10, 1_000, GuSpec.of(Rank.FOUR, GuPath.WOOD)
                    .refine(800_000).costPerUse(0)));
    public static final DeferredItem<Item> HEAVENLY_ESSENCE_TREASURE_KING_LOTUS_GU = ITEMS.register(
            "heavenly_essence_treasure_king_lotus_gu",
            () -> new TreasureLotusGuItem(tendedProperties(), 100, 10_000, GuSpec.of(Rank.FIVE, GuPath.WOOD)
                    .refine(8_000_000).costPerUse(0)));
    //endregion

    //region Nine Leaf Vitality Grass [九叶生机草] -- wood path; nine Vitality Leaf Gu, one regrown every 3 minutes
    // or grown at once for 80 essence
    public static final DeferredItem<Item> NINE_LEAF_VITALITY_GRASS = ITEMS.register("nine_leaf_vitality_grass",
            () -> new NineLeafVitalityGrassItem(tendedProperties(), 3 * Ticks.MINUTE, 80,
                    GuSpec.of(Rank.THREE, GuPath.WOOD).refine(80_000).costPerUse(0)));
    //endregion

    //region Zombie Gu [僵尸蛊] -- Transformation Path [变化道]; a timed Half-Zombie [半生半僵], and a 5-minute
    // window that makes it permanent
    public static final DeferredItem<Item> ROAMING_ZOMBIE_GU = ITEMS.register("roaming_zombie_gu",
            () -> new ZombieGuItem(tendedProperties(), 2 * Ticks.MINUTE, GuSpec.of(Rank.TWO, GuPath.TRANSFORMATION)
                    .refine(8_000).costPerUse(160)
                    .hungerBar(12, 2).hungerPerUse(4).feed(ModItemTags.ZOMBIE_FEED, 1)
                    .cooldown(Ticks.SECOND)));
    public static final DeferredItem<Item> HAIRY_ZOMBIE_GU = ITEMS.register("hairy_zombie_gu",
            () -> new ZombieGuItem(tendedProperties(), 4 * Ticks.MINUTE, GuSpec.of(Rank.THREE, GuPath.TRANSFORMATION)
                    .refine(80_000).costPerUse(1_600)
                    .hungerBar(12, 4).hungerPerUse(4).feed(ModItemTags.ZOMBIE_FEED, 1)
                    .cooldown(Ticks.SECOND)));
    public static final DeferredItem<Item> HOPPING_ZOMBIE_GU = ITEMS.register("hopping_zombie_gu",
            () -> new ZombieGuItem(tendedProperties(), 6 * Ticks.MINUTE, GuSpec.of(Rank.FOUR, GuPath.TRANSFORMATION)
                    .refine(800_000).costPerUse(16_000)
                    .hungerBar(12, 8).hungerPerUse(4).feed(ModItemTags.ZOMBIE_FEED, 1)
                    .cooldown(Ticks.SECOND)));
    public static final DeferredItem<Item> HEAVENLY_DEMON_ZOMBIE_GU = ITEMS.register("heavenly_demon_zombie_gu",
            ModItems::createFifthRankZombieGu);
    public static final DeferredItem<Item> NIGHTMARE_ZOMBIE_GU = ITEMS.register("nightmare_zombie_gu",
            ModItems::createFifthRankZombieGu);
    public static final DeferredItem<Item> ASURA_ZOMBIE_GU = ITEMS.register("asura_zombie_gu",
            ModItems::createFifthRankZombieGu);
    public static final DeferredItem<Item> EARTH_CHIEF_ZOMBIE_GU = ITEMS.register("earth_chief_zombie_gu",
            ModItems::createFifthRankZombieGu);
    public static final DeferredItem<Item> PLAGUE_ZOMBIE_GU = ITEMS.register("plague_zombie_gu",
            ModItems::createFifthRankZombieGu);
    public static final DeferredItem<Item> BLOOD_WIGHT_GU = ITEMS.register("blood_wight_gu",
            ModItems::createFifthRankZombieGu);

    private static ZombieGuItem createFifthRankZombieGu() {
        return new ZombieGuItem(tendedProperties(), 8 * Ticks.MINUTE, GuSpec.of(Rank.FIVE, GuPath.TRANSFORMATION)
                .refine(8_000_000).costPerUse(160_000)
                .hungerBar(12, 16).hungerPerUse(4).feed(ModItemTags.ZOMBIE_FEED, 1)
                .cooldown(Ticks.SECOND));
    }
    //endregion

    //region Watch Gu [更蛊] -- Time Path [宙道]; tended like any other, and taken by the one use it is kept for
    public static final DeferredItem<Item> SECOND_WATCH_GU = ITEMS.register("second_watch_gu",
            () -> new WatchGuItem(tendedProperties(), ModEffects.SECOND_WATCH_GU, 5 * Ticks.MINUTE,
                    GuSpec.of(Rank.FOUR, GuPath.TIME)
                            .refine(100_000).costPerUse(0)
                            .cooldown(Ticks.SECOND)));
    public static final DeferredItem<Item> THIRD_WATCH_GU = ITEMS.register("third_watch_gu",
            () -> new WatchGuItem(tendedProperties(), ModEffects.THIRD_WATCH_GU, 5 * Ticks.MINUTE,
                    GuSpec.of(Rank.FIVE, GuPath.TIME)
                            .refine(1_000_000).costPerUse(0)
                            .cooldown(Ticks.SECOND)));
    //endregion

    //region Malicious Thought Gu [恶念蛊] -- Wisdom Path [智道]; a one-use flood of evil thoughts, taken by its use
    public static final DeferredItem<Item> MALICIOUS_THOUGHT_GU_2 = ITEMS.register("malicious_thought_gu_2",
            () -> new MaliciousThoughtGuItem(tendedProperties(), ModEffects.MALICIOUS_THOUGHT_GU, 64L,
                    GuSpec.of(Rank.TWO, GuPath.WISDOM)
                            .refine(1_000).costPerUse(0)
                            .hungerBar(8, 2).hungerPerUse(0)
                            .feed(ModItemTags.MALICIOUS_THOUGHT_FEED, 1)
                            .cooldown(Ticks.SECOND)));
    public static final DeferredItem<Item> MALICIOUS_THOUGHT_GU_3 = ITEMS.register("malicious_thought_gu_3",
            () -> new MaliciousThoughtGuItem(tendedProperties(), ModEffects.MALICIOUS_THOUGHT_GU, 640L,
                    GuSpec.of(Rank.THREE, GuPath.WISDOM)
                            .refine(10_000).costPerUse(0)
                            .hungerBar(8, 4).hungerPerUse(0)
                            .feed(ModItemTags.MALICIOUS_THOUGHT_FEED, 1)
                            .cooldown(Ticks.SECOND)));
    public static final DeferredItem<Item> MALICIOUS_THOUGHT_GU_4 = ITEMS.register("malicious_thought_gu_4",
            () -> new MaliciousThoughtGuItem(tendedProperties(), ModEffects.MALICIOUS_THOUGHT_GU, 6_400L,
                    GuSpec.of(Rank.FOUR, GuPath.WISDOM)
                            .refine(100_000).costPerUse(0)
                            .hungerBar(8, 8).hungerPerUse(0)
                            .feed(ModItemTags.MALICIOUS_THOUGHT_FEED, 1)
                            .cooldown(Ticks.SECOND)));
    public static final DeferredItem<Item> MALICIOUS_THOUGHT_GU_5 = ITEMS.register("malicious_thought_gu_5",
            () -> new MaliciousThoughtGuItem(tendedProperties(), ModEffects.MALICIOUS_THOUGHT_GU, 64_000L,
                    GuSpec.of(Rank.FIVE, GuPath.WISDOM)
                            .refine(1_000_000).costPerUse(0)
                            .hungerBar(8, 16).hungerPerUse(0)
                            .feed(ModItemTags.MALICIOUS_THOUGHT_FEED, 1)
                            .cooldown(Ticks.SECOND)));
    //endregion

    //region Guts Gu [胆识蛊] -- Soul Path [魂道]; a one-shot Gu that raises the soul cap
    public static final DeferredItem<Item> GUTS_GU = ITEMS.register("guts_gu",
            () -> new GutsGuItem(oneShotProperties(), GuSpec.of(Rank.ONE, GuPath.SOUL)));
    //endregion

    //region Casual Gu [随意蛊] -- Wisdom Path [智道]; ten seconds of random thoughts, taken by its use
    public static final DeferredItem<Item> CASUAL_GU_1 = ITEMS.register("casual_gu_1",
            () -> new CasualGuItem(tendedProperties(), ModEffects.CASUAL_GU, GuSpec.of(Rank.ONE, GuPath.WISDOM)
                    .refine(100)
                    .costPerUse(0)
                    .hungerBar(8, 1).hungerPerUse(0)
                    .feed(ModItemTags.CASUAL_FEED, 1)
                    .cooldown(Ticks.SECOND)));
    public static final DeferredItem<Item> CASUAL_GU_2 = ITEMS.register("casual_gu_2",
            () -> new CasualGuItem(tendedProperties(), ModEffects.CASUAL_GU, GuSpec.of(Rank.TWO, GuPath.WISDOM)
                    .refine(1_000)
                    .costPerUse(0)
                    .hungerBar(8, 2).hungerPerUse(0)
                    .feed(ModItemTags.CASUAL_FEED, 1)
                    .cooldown(Ticks.SECOND)));
    //endregion

    //region Stone Aperture Gu [石窍蛊] -- earth path; never feeds, taken by its use, and the aperture
    // it leaves stands on this rank's peak, petrified
    public static final DeferredItem<Item> STONE_APERTURE_GU_3 = ITEMS.register("stone_aperture_gu_3",
            () -> new StoneApertureGuItem(tendedProperties(), GuSpec.of(Rank.THREE, GuPath.EARTH)
                    .refine(10_000).costPerUse(0)
                    .cooldown(Ticks.SECOND)));
    public static final DeferredItem<Item> STONE_APERTURE_GU_4 = ITEMS.register("stone_aperture_gu_4",
            () -> new StoneApertureGuItem(tendedProperties(), GuSpec.of(Rank.FOUR, GuPath.EARTH)
                    .refine(100_000).costPerUse(0)
                    .cooldown(Ticks.SECOND)));
    public static final DeferredItem<Item> STONE_APERTURE_GU_5 = ITEMS.register("stone_aperture_gu_5",
            () -> new StoneApertureGuItem(tendedProperties(), GuSpec.of(Rank.FIVE, GuPath.EARTH)
                    .refine(1_000_000).costPerUse(0)
                    .cooldown(Ticks.SECOND)));
    //endregion

    //region Second Aperture Gu [第二空窍蛊] -- human path; opens or upgrades the second aperture,
    // a free one-shot taken by its use; Grade-A at 8/10, this rank's first stage, never a physique
    public static final DeferredItem<Item> SECOND_APERTURE_GU_1 = ITEMS.register("second_aperture_gu_1",
            () -> createSecondApertureGu(Rank.ONE));
    public static final DeferredItem<Item> SECOND_APERTURE_GU_2 = ITEMS.register("second_aperture_gu_2",
            () -> createSecondApertureGu(Rank.TWO));
    public static final DeferredItem<Item> SECOND_APERTURE_GU_3 = ITEMS.register("second_aperture_gu_3",
            () -> createSecondApertureGu(Rank.THREE));
    public static final DeferredItem<Item> SECOND_APERTURE_GU_4 = ITEMS.register("second_aperture_gu_4",
            () -> createSecondApertureGu(Rank.FOUR));
    public static final DeferredItem<Item> SECOND_APERTURE_GU_5 = ITEMS.register("second_aperture_gu_5",
            () -> createSecondApertureGu(Rank.FIVE));

    private static SecondApertureGuItem createSecondApertureGu(Rank rank) {
        return new SecondApertureGuItem(oneShotProperties(), GuSpec.of(rank, GuPath.HUMAN));
    }
    //endregion

    //region Gu materials [蛊材]
    public static final DeferredItem<Item> PRIMEVAL_STONE = ITEMS.register("primeval_stone",
            () -> new PrimevalStoneItem(new Item.Properties(), PRIMEVAL_STONE_ESSENCE));
    public static final DeferredItem<Item> LIQUOR = ITEMS.register("liquor",
            () -> new LiquorItem(new Item.Properties()));
    public static final DeferredItem<Item> SOUR_LIQUOR = ITEMS.register("sour_liquor",
            () -> new LiquorItem(new Item.Properties()));
    public static final DeferredItem<Item> SWEET_LIQUOR = ITEMS.register("sweet_liquor",
            () -> new LiquorItem(new Item.Properties()));
    public static final DeferredItem<Item> BITTER_LIQUOR = ITEMS.register("bitter_liquor",
            () -> new LiquorItem(new Item.Properties()));
    public static final DeferredItem<Item> SPICY_LIQUOR = ITEMS.register("spicy_liquor",
            () -> new LiquorItem(new Item.Properties()));
    public static final DeferredItem<Item> SPIRIT_SPRING = ITEMS.register("spirit_spring",
            () -> new BlockItem(ModBlocks.SPIRIT_SPRING.get(), new Item.Properties()));
    //endregion

    //region Herb materials [草木蛊材] -- pure Gu material, wood path rank I; only a hoe takes them from the plant
    public static final DeferredItem<Item> INTIMATE_GRASS_BUNDLE = ITEMS.register("intimate_grass_bundle",
            () -> new GuMaterialItem(new Item.Properties(), Rank.ONE, GuPath.WOOD));
    public static final DeferredItem<Item> MOON_ORCHID_PETALS = ITEMS.register("moon_orchid_petals",
            () -> new GuMaterialItem(new Item.Properties(), Rank.ONE, GuPath.WOOD));
    //endregion

    //region Earth materials [土道蛊材] -- pure Gu material, earth path rank I; no natural source yet
    public static final DeferredItem<Item> ROTTEN_FROZEN_MUD = ITEMS.register("rotten_frozen_mud",
            () -> new GuMaterialItem(new Item.Properties(), Rank.ONE, GuPath.EARTH));
    //endregion

    //region Human Aperture [人窍] -- pure Gu material, ranks I..V; a wiped death drops one per aperture
    public static final DeferredItem<Item> HUMAN_APERTURE_1 = registerHumanAperture("human_aperture_1", Rank.ONE);
    public static final DeferredItem<Item> HUMAN_APERTURE_2 = registerHumanAperture("human_aperture_2", Rank.TWO);
    public static final DeferredItem<Item> HUMAN_APERTURE_3 = registerHumanAperture("human_aperture_3", Rank.THREE);
    public static final DeferredItem<Item> HUMAN_APERTURE_4 = registerHumanAperture("human_aperture_4", Rank.FOUR);
    public static final DeferredItem<Item> HUMAN_APERTURE_5 = registerHumanAperture("human_aperture_5", Rank.FIVE);

    private static DeferredItem<Item> registerHumanAperture(String id, Rank rank) {
        return ITEMS.register(id, () -> new GuMaterialItem(new Item.Properties().stacksTo(64), rank, GuPath.HUMAN));
    }

    public static @Nullable Item getHumanAperture(Rank rank) {
        return switch (rank) {
            case ONE -> HUMAN_APERTURE_1.get();
            case TWO -> HUMAN_APERTURE_2.get();
            case THREE -> HUMAN_APERTURE_3.get();
            case FOUR -> HUMAN_APERTURE_4.get();
            case FIVE -> HUMAN_APERTURE_5.get();
            default -> null;
        };
    }
    //endregion

    //region Qi Path [气道] materials -- 21, ranks I..V
    public static final DeferredItem<Item> SWORD_QI_1 =
            registerQiMaterial("sword_qi_1", Rank.ONE, QiKind.SWORD);
    public static final DeferredItem<Item> SWORD_QI_2 =
            registerQiMaterial("sword_qi_2", Rank.TWO, QiKind.SWORD);
    public static final DeferredItem<Item> SWORD_QI_3 =
            registerQiMaterial("sword_qi_3", Rank.THREE, QiKind.SWORD);
    public static final DeferredItem<Item> SWORD_QI_4 =
            registerQiMaterial("sword_qi_4", Rank.FOUR, QiKind.SWORD);
    public static final DeferredItem<Item> SWORD_QI_5 =
            registerQiMaterial("sword_qi_5", Rank.FIVE, QiKind.SWORD);
    public static final DeferredItem<Item> STRENGTH_QI_1 =
            registerQiMaterial("strength_qi_1", Rank.ONE, QiKind.STRENGTH);
    public static final DeferredItem<Item> STRENGTH_QI_2 =
            registerQiMaterial("strength_qi_2", Rank.TWO, QiKind.STRENGTH);
    public static final DeferredItem<Item> STRENGTH_QI_3 =
            registerQiMaterial("strength_qi_3", Rank.THREE, QiKind.STRENGTH);
    public static final DeferredItem<Item> STRENGTH_QI_4 =
            registerQiMaterial("strength_qi_4", Rank.FOUR, QiKind.STRENGTH);
    public static final DeferredItem<Item> STRENGTH_QI_5 =
            registerQiMaterial("strength_qi_5", Rank.FIVE, QiKind.STRENGTH);
    public static final DeferredItem<Item> LIFE_QI_1 = ITEMS.register("life_qi_1",
            () -> new LifeQiItem(qiProperties(), Rank.ONE, qiEssenceCost(Rank.ONE)));
    public static final DeferredItem<Item> LIFE_QI_2 = ITEMS.register("life_qi_2",
            () -> new LifeQiItem(qiProperties(), Rank.TWO, qiEssenceCost(Rank.TWO)));
    public static final DeferredItem<Item> LIFE_QI_3 = ITEMS.register("life_qi_3",
            () -> new LifeQiItem(qiProperties(), Rank.THREE, qiEssenceCost(Rank.THREE)));
    public static final DeferredItem<Item> LIFE_QI_4 = ITEMS.register("life_qi_4",
            () -> new LifeQiItem(qiProperties(), Rank.FOUR, qiEssenceCost(Rank.FOUR)));
    public static final DeferredItem<Item> LIFE_QI_5 = ITEMS.register("life_qi_5",
            () -> new LifeQiItem(qiProperties(), Rank.FIVE, qiEssenceCost(Rank.FIVE)));
    public static final DeferredItem<Item> ESSENCE_QI_1 =
            registerQiMaterial("essence_qi_1", Rank.ONE, QiKind.ESSENCE);
    public static final DeferredItem<Item> ESSENCE_QI_2 =
            registerQiMaterial("essence_qi_2", Rank.TWO, QiKind.ESSENCE);
    public static final DeferredItem<Item> ESSENCE_QI_3 =
            registerQiMaterial("essence_qi_3", Rank.THREE, QiKind.ESSENCE);
    public static final DeferredItem<Item> ESSENCE_QI_4 =
            registerQiMaterial("essence_qi_4", Rank.FOUR, QiKind.ESSENCE);
    public static final DeferredItem<Item> ESSENCE_QI_5 =
            registerQiMaterial("essence_qi_5", Rank.FIVE, QiKind.ESSENCE);
    public static final DeferredItem<Item> DEATH_QI_5 = ITEMS.register("death_qi_5",
            () -> new DeathQiItem(qiProperties(), Rank.FIVE, qiEssenceCost(Rank.FIVE)));

    private static DeferredItem<Item> registerQiMaterial(String id, Rank rank, QiKind kind) {
        return ITEMS.register(id, () -> new QiMaterialItem(qiProperties(), rank, kind, qiEssenceCost(rank)));
    }

    private static long qiEssenceCost(Rank rank) { return QI_ESSENCE_COST[rank.ordinal() - Rank.ONE.ordinal()]; }

    private static Item.Properties qiProperties() { return new Item.Properties().stacksTo(64); }
    //endregion

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
