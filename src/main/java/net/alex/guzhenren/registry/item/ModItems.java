package net.alex.guzhenren.registry.item;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.effect.timed.BruteForceLonghornBeetleGuEffect;
import net.alex.guzhenren.effect.timed.CrashGuEffect;
import net.alex.guzhenren.effect.timed.DragonpillCricketGuEffect;
import net.alex.guzhenren.effect.timed.FlowerBoarGuEffect;
import net.alex.guzhenren.effect.timed.HardshipStrengthGuEffect;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.body.Bone;
import net.alex.guzhenren.gameplay.body.Skin;
import net.alex.guzhenren.gameplay.body.Tendon;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.path.qi.QiKind;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.HumanStrength;
import net.alex.guzhenren.gameplay.path.strength.StrengthPathBranch;
import net.alex.guzhenren.gameplay.rewind.ChunkKind;
import net.alex.guzhenren.item.AdvancementIconItem;
import net.alex.guzhenren.item.gu.GuSpec;
import net.alex.guzhenren.item.gu.GuType;
import net.alex.guzhenren.item.gu.mortal.BuffGuItem;
import net.alex.guzhenren.item.gu.mortal.earth.StoneApertureGuItem;
import net.alex.guzhenren.item.gu.mortal.food.LiquorWormItem;
import net.alex.guzhenren.item.gu.mortal.heaven.LifespanGuItem;
import net.alex.guzhenren.item.gu.mortal.heaven.RelicsGuItem;
import net.alex.guzhenren.item.gu.mortal.human.HopeGuItem;
import net.alex.guzhenren.item.gu.mortal.human.SecondApertureGuItem;
import net.alex.guzhenren.item.gu.mortal.rule.AmplifyEffectGuItem;
import net.alex.guzhenren.item.gu.mortal.soul.GutsGuItem;
import net.alex.guzhenren.item.gu.mortal.space.PrimevalElderGuItem;
import net.alex.guzhenren.item.gu.mortal.strength.AllOutEffortGuItem;
import net.alex.guzhenren.item.gu.mortal.strength.BeastStrengthGuItem;
import net.alex.guzhenren.item.gu.mortal.strength.HumanStrengthGuItem;
import net.alex.guzhenren.item.gu.mortal.strength.BoneGuItem;
import net.alex.guzhenren.item.gu.mortal.strength.SelfRelianceGuItem;
import net.alex.guzhenren.item.gu.mortal.strength.SkinGuItem;
import net.alex.guzhenren.item.gu.mortal.strength.TendonGuItem;
import net.alex.guzhenren.item.gu.mortal.time.ManAsBeforeGuItem;
import net.alex.guzhenren.item.gu.mortal.time.TerrainAsBeforeGuItem;
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
import net.alex.guzhenren.registry.fluid.ModFluids;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
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
 * right. One line per item, plus the PNG and both lang keys. Past 300 lines on purpose: it is a data table, and
 * splitting it would scatter the one place the numbers live.
 *
 * <p>⚠ The registration id mirrors the Java name (and the PNG); a rank ladder is numbered
 * ({@code sword_qi_1} to {@code sword_qi_5}), but an item with its own fiction name keeps that name
 * ({@code blood_wight_gu}).
 *
 * <p>{@link #SPIRIT_SPRING} is the spring's {@code BlockItem}: same key as the block, answers to
 * the block's lang entry, and its icon is the fluid's still strip (see
 * {@code ModItemModelProvider}) -- no item PNG of its own. {@link #PURE_WATER_BUCKET} is the vanilla water
 * bucket's twin for the Pure Water [纯水]: it places a source, an empty bucket scoops one back, and a
 * dispenser pours it ({@code PureWaterEvents}). {@link #ADVANCEMENT_ICON} is no game item: it carries the
 * realm advancements' numerals ({@link AdvancementIconItem}).
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

    private static Item.Properties createTendedProperties() { return new Item.Properties().stacksTo(1); }

    private static Item.Properties createOneShotProperties() { return new Item.Properties(); }

    //region One-shot Gu [一次性] -- refining IS the use; one instant press pays, lands and spends (Hope Gu: a ritual)
    public static final DeferredItem<Item> HOPE_GU = ITEMS.register("hope_gu",
            () -> new HopeGuItem(createOneShotProperties().stacksTo(1), GuSpec.of(Rank.ONE, GuPath.HUMAN)
                    .type(GuType.CULTIVATION)));
    public static final DeferredItem<Item> VITALITY_LEAF_GU = ITEMS.register("vitality_leaf_gu",
            () -> new VitalityLeafGuItem(createOneShotProperties(), GuSpec.of(Rank.ONE, GuPath.WOOD)
                    .type(GuType.HEALING)));
    public static final DeferredItem<Item> LIFESPAN_GU = ITEMS.register("lifespan_gu",
            () -> new LifespanGuItem(createOneShotProperties(), 1, 9, GuSpec.of(Rank.ONE, GuPath.HEAVEN)));
    public static final DeferredItem<Item> TENS_LIFESPAN_GU = ITEMS.register("tens_lifespan_gu",
            () -> new LifespanGuItem(createOneShotProperties(), 10, 19, GuSpec.of(Rank.ONE, GuPath.HEAVEN)));
    public static final DeferredItem<Item> HUNDREDS_LIFESPAN_GU = ITEMS.register("hundreds_lifespan_gu",
            () -> new LifespanGuItem(createOneShotProperties(), 100, 199, GuSpec.of(Rank.ONE, GuPath.HEAVEN)));
    public static final DeferredItem<Item> THOUSANDS_LIFESPAN_GU = ITEMS.register("thousands_lifespan_gu",
            () -> new LifespanGuItem(createOneShotProperties(), 1000, 1999, GuSpec.of(Rank.ONE, GuPath.HEAVEN)));
    public static final DeferredItem<Item> COPPER_RELICS_GU = ITEMS.register("copper_relics_gu",
            () -> new RelicsGuItem(createOneShotProperties(), GuSpec.of(Rank.ONE, GuPath.HEAVEN)
                    .type(GuType.CULTIVATION)));
    public static final DeferredItem<Item> STEEL_RELICS_GU = ITEMS.register("steel_relics_gu",
            () -> new RelicsGuItem(createOneShotProperties(), GuSpec.of(Rank.TWO, GuPath.HEAVEN)
                    .type(GuType.CULTIVATION)));
    public static final DeferredItem<Item> SILVER_RELICS_GU = ITEMS.register("silver_relics_gu",
            () -> new RelicsGuItem(createOneShotProperties(), GuSpec.of(Rank.THREE, GuPath.HEAVEN)
                    .type(GuType.CULTIVATION)));
    public static final DeferredItem<Item> GOLD_RELICS_GU = ITEMS.register("gold_relics_gu",
            () -> new RelicsGuItem(createOneShotProperties(), GuSpec.of(Rank.FOUR, GuPath.HEAVEN)
                    .type(GuType.CULTIVATION)));
    public static final DeferredItem<Item> CRYSTAL_RELICS_GU = ITEMS.register("crystal_relics_gu",
            () -> new RelicsGuItem(createOneShotProperties(), GuSpec.of(Rank.FIVE, GuPath.HEAVEN)
                    .type(GuType.CULTIVATION)));
    //endregion

    //region Beast Strength Phantom Branch [兽力虚影流]
    public static final DeferredItem<Item> WHITE_BOAR_GU = ITEMS.register("white_boar_gu",
            () -> new BeastStrengthGuItem(createTendedProperties(), BeastStrength.WHITE_BOAR,
                    createBeastStrengthSpec(ModItemTags.BOAR_FEED)));
    public static final DeferredItem<Item> BLACK_BOAR_GU = ITEMS.register("black_boar_gu",
            () -> new BeastStrengthGuItem(createTendedProperties(), BeastStrength.BLACK_BOAR,
                    createBeastStrengthSpec(ModItemTags.BOAR_FEED)));
    public static final DeferredItem<Item> BEAR_STRENGTH_GU = ITEMS.register("bear_strength_gu",
            () -> new BeastStrengthGuItem(createTendedProperties(), BeastStrength.BEAR,
                    createBeastStrengthSpec(ModItemTags.BEAR_FEED)));
    public static final DeferredItem<Item> FLOWER_BOAR_GU = ITEMS.register("flower_boar_gu",
            () -> new BuffGuItem(createTendedProperties(), ModEffects.FLOWER_BOAR_GU, FlowerBoarGuEffect.DURATION_TICKS,
                    createBeastBuffSpec(ModItemTags.BOAR_FEED)));
    public static final DeferredItem<Item> DRAGONPILL_CRICKET_GU = ITEMS.register("dragonpill_cricket_gu",
            () -> new BuffGuItem(createTendedProperties(), ModEffects.DRAGONPILL_CRICKET_GU,
                    DragonpillCricketGuEffect.DURATION_TICKS, createBeastBuffSpec(ModItemTags.RABBIT_FEED)));
    public static final DeferredItem<Item> BRUTE_FORCE_LONGHORN_BEETLE_GU =
            ITEMS.register("brute_force_longhorn_beetle_gu",
                    () -> new BuffGuItem(createTendedProperties(), ModEffects.BRUTE_FORCE_LONGHORN_BEETLE_GU,
                            BruteForceLonghornBeetleGuEffect.DURATION_TICKS,
                            createBeastBuffSpec(ModItemTags.BEEF_FEED)));

    private static GuSpec createBeastStrengthSpec(TagKey<Item> feed) {
        return GuSpec.of(Rank.ONE, GuPath.STRENGTH)
                .strengthPathBranch(StrengthPathBranch.BEAST_STRENGTH_PHANTOM)
                .type(GuType.DAO_MARK_CARVING)
                .refine(800)
                .channel(3_600)
                .hungerBar(36, 1).essencePerHunger(100)
                .feed(feed, 1);
    }

    private static GuSpec createBeastBuffSpec(TagKey<Item> feed) {
        return GuSpec.of(Rank.ONE, GuPath.STRENGTH)
                .strengthPathBranch(StrengthPathBranch.BEAST_STRENGTH_PHANTOM)
                .type(GuType.AMPLIFICATION)
                .refine(800)
                .costPerUse(16)
                .hungerBar(12, 1).hungerPerUse(4)
                .feed(feed, 1)
                .cooldown(30 * Ticks.SECOND, Ticks.SECOND);
    }
    //endregion

    //region Normal [基础力道] -- Strength Path Gu outside the three specialized branches
    public static final DeferredItem<Item> HORIZONTAL_CRASH_GU = ITEMS.register("horizontal_crash_gu",
            () -> new BuffGuItem(createTendedProperties(), ModEffects.HORIZONTAL_CRASH_GU,
                    CrashGuEffect.getDuration(30),
                    GuSpec.of(Rank.THREE, GuPath.STRENGTH).type(GuType.MOVEMENT)
                            .refine(80_000).costPerUse(1_600)
                            .hungerBar(12, 1).hungerPerUse(4).feed(ModItemTags.ANVIL_FEED, 1)
                            .cooldown(30 * Ticks.SECOND, Ticks.SECOND)));
    public static final DeferredItem<Item> VERTICAL_CRASH_GU = ITEMS.register("vertical_crash_gu",
            () -> new BuffGuItem(createTendedProperties(), ModEffects.VERTICAL_CRASH_GU, CrashGuEffect.getDuration(30),
                    GuSpec.of(Rank.THREE, GuPath.STRENGTH).type(GuType.MOVEMENT)
                            .refine(80_000).costPerUse(1_600)
                            .hungerBar(12, 1).hungerPerUse(4).feed(ModItemTags.ANVIL_FEED, 1)
                            .cooldown(30 * Ticks.SECOND, Ticks.SECOND)));
    public static final DeferredItem<Item> CHARGING_CRASH_GU_4 = ITEMS.register("charging_crash_gu_4",
            () -> new BuffGuItem(createTendedProperties(), ModEffects.CHARGING_CRASH_GU,
                    CrashGuEffect.getDuration(30), 3, GuSpec.of(Rank.FOUR, GuPath.STRENGTH).type(GuType.MOVEMENT)
                    .refine(800_000).costPerUse(16_000)
                    .hungerBar(24, 1).hungerPerUse(8).hungerPerDay(2).health(24).feed(ModItemTags.ANVIL_FEED, 1)
                    .cooldown(30 * Ticks.SECOND, Ticks.SECOND)));
    public static final DeferredItem<Item> CHARGING_CRASH_GU_5 = ITEMS.register("charging_crash_gu_5",
            () -> new BuffGuItem(createTendedProperties(), ModEffects.CHARGING_CRASH_GU,
                    CrashGuEffect.getDuration(60), 4, GuSpec.of(Rank.FIVE, GuPath.STRENGTH).type(GuType.MOVEMENT)
                    .refine(8_000_000).costPerUse(160_000)
                    .hungerBar(36, 1).hungerPerUse(12).hungerPerDay(3).health(36).feed(ModItemTags.ANVIL_FEED, 1)
                    .cooldown(30 * Ticks.SECOND, Ticks.SECOND)));
    public static final DeferredItem<Item> SELF_RELIANCE_GU_2 = ITEMS.register("self_reliance_gu_2",
            () -> new SelfRelianceGuItem(createTendedProperties(), 30 * Ticks.SECOND, 1,
                    GuSpec.of(Rank.TWO, GuPath.STRENGTH).type(GuType.HEALING)
                            .refine(8_000).costPerUse(160)
                            .hungerBar(48, 1).hungerPerUse(16).hungerPerDay(4).health(48)
                            .feed(ModItemTags.COBBLESTONE_FEED, 1)
                            .cooldown(30 * Ticks.SECOND, Ticks.SECOND)));
    public static final DeferredItem<Item> SELF_RELIANCE_GU_3 = ITEMS.register("self_reliance_gu_3",
            () -> new SelfRelianceGuItem(createTendedProperties(), 60 * Ticks.SECOND, 2,
                    GuSpec.of(Rank.THREE, GuPath.STRENGTH).type(GuType.HEALING)
                            .refine(80_000).costPerUse(1_600)
                            .hungerBar(96, 1).hungerPerUse(32).hungerPerDay(8).health(96)
                            .feed(ModItemTags.COBBLESTONE_FEED, 1)
                            .cooldown(30 * Ticks.SECOND, Ticks.SECOND)));
    public static final DeferredItem<Item> SELF_RELIANCE_GU_4 = ITEMS.register("self_reliance_gu_4",
            () -> new SelfRelianceGuItem(createTendedProperties(), 120 * Ticks.SECOND, 3,
                    GuSpec.of(Rank.FOUR, GuPath.STRENGTH).type(GuType.HEALING)
                            .refine(800_000).costPerUse(16_000)
                            .hungerBar(144, 1).hungerPerUse(48).hungerPerDay(12).health(144)
                            .feed(ModItemTags.COBBLESTONE_FEED, 1)
                            .cooldown(30 * Ticks.SECOND, Ticks.SECOND)));
    public static final DeferredItem<Item> HARDSHIP_STRENGTH_GU = ITEMS.register("hardship_strength_gu",
            () -> new BuffGuItem(createTendedProperties(), ModEffects.HARDSHIP_STRENGTH_GU,
                    HardshipStrengthGuEffect.DURATION_TICKS,
                    GuSpec.of(Rank.FOUR, GuPath.STRENGTH).type(GuType.AMPLIFICATION)
                            .refine(800_000).costPerUse(16_000)
                            .hungerBar(144, 1).hungerPerUse(48).hungerPerDay(12).health(144)
                            .feed(ModItemTags.POTATO_FEED, 1)
                            .cooldown(30 * Ticks.SECOND, Ticks.SECOND)));
    //endregion

    //region Human Jun Strength Branch [人力钧力流] -- one round is one layer
    public static final DeferredItem<Item> JIN_STRENGTH_GU = ITEMS.register("jin_strength_gu",
            () -> new HumanStrengthGuItem(createTendedProperties(), HumanStrength.JIN,
                    GuSpec.of(Rank.ONE, GuPath.STRENGTH).type(GuType.DAO_MARK_CARVING)
                            .strengthPathBranch(StrengthPathBranch.HUMAN_JUN_STRENGTH)
                            .refine(800)
                            .channel(3_600)
                            .hungerBar(36, 1).essencePerHunger(300)
                            .feed(ModItemTags.JIN_FEED, 1)));
    public static final DeferredItem<Item> TENS_JIN_STRENGTH_GU = ITEMS.register("tens_jin_strength_gu",
            () -> new HumanStrengthGuItem(createTendedProperties(), HumanStrength.TEN_JIN,
                    GuSpec.of(Rank.TWO, GuPath.STRENGTH).type(GuType.DAO_MARK_CARVING)
                            .strengthPathBranch(StrengthPathBranch.HUMAN_JUN_STRENGTH)
                            .refine(8_000)
                            .channel(36_000)
                            .hungerBar(36, 2).essencePerHunger(3_000)
                            .feed(ModItemTags.JIN_FEED, 1)));
    public static final DeferredItem<Item> JUN_STRENGTH_GU = ITEMS.register("jun_strength_gu",
            () -> new HumanStrengthGuItem(createTendedProperties(), HumanStrength.JUN,
                    GuSpec.of(Rank.THREE, GuPath.STRENGTH).type(GuType.DAO_MARK_CARVING)
                            .strengthPathBranch(StrengthPathBranch.HUMAN_JUN_STRENGTH)
                            .refine(80_000)
                            .channel(36_000)
                            .hungerBar(36, 4).essencePerHunger(6_000)
                            .feed(ModItemTags.JIN_FEED_SMELTED, 1)));
    public static final DeferredItem<Item> TENS_JUN_STRENGTH_GU = ITEMS.register("tens_jun_strength_gu",
            () -> new HumanStrengthGuItem(createTendedProperties(), HumanStrength.TEN_JUN,
                    GuSpec.of(Rank.FOUR, GuPath.STRENGTH).type(GuType.DAO_MARK_CARVING)
                            .strengthPathBranch(StrengthPathBranch.HUMAN_JUN_STRENGTH)
                            .refine(800_000)
                            .channel(360_000)
                            .hungerBar(36, 8).essencePerHunger(60_000)
                            .feed(ModItemTags.JIN_FEED_SMELTED, 1)));
    //endregion

    //region Normal [基础力道] -- Jade Skin Gu [玉皮蛊] and White Jade Gu [白玉蛊]: a defensive skin for a while
    public static final DeferredItem<Item> JADE_SKIN_GU = ITEMS.register("jade_skin_gu",
            () -> new SkinGuItem(createTendedProperties(), Skin.JADE_SKIN, 60 * Ticks.SECOND,
                    createTemperingSpec(Rank.ONE, 800, 16, 1, ModItemTags.JADE_SKIN_FEED)));
    public static final DeferredItem<Item> WHITE_JADE_GU = ITEMS.register("white_jade_gu",
            () -> new SkinGuItem(createTendedProperties(), Skin.WHITE_JADE_SKIN, 90 * Ticks.SECOND,
                    createTemperingSpec(Rank.TWO, 8_000, 160, 4, ModItemTags.WHITE_JADE_FEED)));
    //endregion

    //region Normal [基础力道] -- body tempering [炼体]: bronze and iron skins, steel tendons and iron bones for good
    public static final DeferredItem<Item> BRONZE_SKIN_GU_1 = ITEMS.register("bronze_skin_gu_1",
            () -> new SkinGuItem(createTendedProperties(), Skin.BRONZE_SKIN_1, 0,
                    createTemperingSpec(Rank.ONE, 800, 16, 1, ModItemTags.BRONZE_SKIN_FEED)));
    public static final DeferredItem<Item> BRONZE_SKIN_GU_2 = ITEMS.register("bronze_skin_gu_2",
            () -> new SkinGuItem(createTendedProperties(), Skin.BRONZE_SKIN_2, 0,
                    createTemperingSpec(Rank.TWO, 8_000, 160, 4, ModItemTags.BRONZE_SKIN_FEED)));
    public static final DeferredItem<Item> BRONZE_SKIN_GU_3 = ITEMS.register("bronze_skin_gu_3",
            () -> new SkinGuItem(createTendedProperties(), Skin.BRONZE_SKIN_3, 0,
                    createTemperingSpec(Rank.THREE, 80_000, 1_600, 8, ModItemTags.BRONZE_SKIN_FEED)));
    public static final DeferredItem<Item> ANCIENT_BRONZE_SKIN_GU = ITEMS.register("ancient_bronze_skin_gu",
            () -> new SkinGuItem(createTendedProperties(), Skin.ANCIENT_BRONZE_SKIN, 0,
                    createTemperingSpec(Rank.FOUR, 800_000, 16_000, 12, ModItemTags.ANCIENT_BRONZE_SKIN_FEED)));
    public static final DeferredItem<Item> IRON_SKIN_GU_1 = ITEMS.register("iron_skin_gu_1",
            () -> new SkinGuItem(createTendedProperties(), Skin.IRON_SKIN_1, 0,
                    createTemperingSpec(Rank.ONE, 800, 16, 1, ModItemTags.IRON_SKIN_FEED)));
    public static final DeferredItem<Item> IRON_SKIN_GU_2 = ITEMS.register("iron_skin_gu_2",
            () -> new SkinGuItem(createTendedProperties(), Skin.IRON_SKIN_2, 0,
                    createTemperingSpec(Rank.TWO, 8_000, 160, 4, ModItemTags.IRON_SKIN_FEED)));
    public static final DeferredItem<Item> IRON_SKIN_GU_3 = ITEMS.register("iron_skin_gu_3",
            () -> new SkinGuItem(createTendedProperties(), Skin.IRON_SKIN_3, 0,
                    createTemperingSpec(Rank.THREE, 80_000, 1_600, 8, ModItemTags.IRON_SKIN_FEED)));
    public static final DeferredItem<Item> ESSENCE_IRON_SKIN_GU = ITEMS.register("essence_iron_skin_gu",
            () -> new SkinGuItem(createTendedProperties(), Skin.ESSENCE_IRON_SKIN, 0,
                    createTemperingSpec(Rank.FOUR, 800_000, 16_000, 12, ModItemTags.ESSENCE_IRON_SKIN_FEED)));
    public static final DeferredItem<Item> STEEL_TENDON_GU_3 = ITEMS.register("steel_tendon_gu_3",
            () -> new TendonGuItem(createTendedProperties(), Tendon.STEEL_TENDON_3,
                    createTemperingSpec(Rank.THREE, 80_000, 1_600, 8, ModItemTags.STEEL_TENDON_FEED)));
    public static final DeferredItem<Item> STEEL_TENDON_GU_4 = ITEMS.register("steel_tendon_gu_4",
            () -> new TendonGuItem(createTendedProperties(), Tendon.STEEL_TENDON_4,
                    createTemperingSpec(Rank.FOUR, 800_000, 16_000, 12, ModItemTags.STEEL_TENDON_FEED)));
    public static final DeferredItem<Item> IRON_BONE_GU = ITEMS.register("iron_bone_gu",
            () -> new BoneGuItem(createTendedProperties(), Bone.IRON_BONE,
                    createTemperingSpec(Rank.THREE, 80_000, 1_600, 8, ModItemTags.IRON_BONE_FEED)));
    public static final DeferredItem<Item> ESSENCE_IRON_BONE_GU = ITEMS.register("essence_iron_bone_gu",
            () -> new BoneGuItem(createTendedProperties(), Bone.ESSENCE_IRON_BONE,
                    createTemperingSpec(Rank.FOUR, 800_000, 16_000, 12, ModItemTags.ESSENCE_IRON_BONE_FEED)));

    /**
     * The tempering Gu's shared row shape: a food is one point, and {@code scale} sizes a bar of 12 days at 4 per
     * use and a health of 12, both times {@code scale}; 30 s / 1 s cooldown.
     */
    private static GuSpec createTemperingSpec(Rank rank, int refine, int costPerUse, int scale, TagKey<Item> feed) {
        return GuSpec.of(rank, GuPath.STRENGTH).type(GuType.DEFENSIVE)
                .refine(refine).costPerUse(costPerUse)
                .hungerBar(12 * scale, 1).hungerPerUse(4 * scale).hungerPerDay(scale).health(12 * scale)
                .feed(feed, 1)
                .cooldown(30 * Ticks.SECOND, Ticks.SECOND);
    }
    //endregion

    //region Normal [基础力道] -- All-Out Effort Gu unlocks a stockpiled 9999 jin
    public static final DeferredItem<Item> ALL_OUT_EFFORT_GU_3 = ITEMS.register("all_out_effort_gu_3",
            () -> new AllOutEffortGuItem(createTendedProperties(), 60, GuSpec.of(Rank.THREE, GuPath.STRENGTH)
                    .type(GuType.AMPLIFICATION)
                    .refine(80_000)
                    .costPerUse(1_600)
                    .hungerBar(48, 1).hungerPerUse(16).hungerPerDay(4).health(48)
                    .feed(ModItemTags.ALL_OUT_FEED, 4)
                    .cooldown(80 * Ticks.SECOND)));
    public static final DeferredItem<Item> ALL_OUT_EFFORT_GU_4 = ITEMS.register("all_out_effort_gu_4",
            () -> new AllOutEffortGuItem(createTendedProperties(), 90, GuSpec.of(Rank.FOUR, GuPath.STRENGTH)
                    .type(GuType.AMPLIFICATION)
                    .refine(800_000)
                    .costPerUse(16_000)
                    .hungerBar(96, 1).hungerPerUse(32).hungerPerDay(8).health(96)
                    .feed(ModItemTags.ALL_OUT_FEED, 4)
                    .cooldown(100 * Ticks.SECOND)));
    public static final DeferredItem<Item> ALL_OUT_EFFORT_GU_5 = ITEMS.register("all_out_effort_gu_5",
            () -> new AllOutEffortGuItem(createTendedProperties(), 120, GuSpec.of(Rank.FIVE, GuPath.STRENGTH)
                    .type(GuType.AMPLIFICATION)
                    .refine(8_000_000)
                    .costPerUse(160_000)
                    .hungerBar(192, 1).hungerPerUse(64).hungerPerDay(16).health(192)
                    .feed(ModItemTags.ALL_OUT_FEED, 4)
                    .cooldown(120 * Ticks.SECOND)));
    //endregion

    //region Liquor Worm [酒虫] -- a hunger bar of 8 days with 3 days a use, and only its own rank can drive it
    public static final DeferredItem<Item> LIQUOR_WORM = ITEMS.register("liquor_worm",
            () -> new LiquorWormItem(createTendedProperties(), GuSpec.of(Rank.ONE, GuPath.FOOD).type(GuType.CULTIVATION)
                    .refine(1_600).costPerUse(16)
                    .hungerBar(8, 1).hungerPerUse(3).feed(ModItemTags.LIQUOR_FEED, 1)));
    public static final DeferredItem<Item> FOUR_FLAVORS_LIQUOR_WORM = ITEMS.register("four_flavors_liquor_worm",
            () -> new LiquorWormItem(createTendedProperties(), GuSpec.of(Rank.TWO, GuPath.FOOD).type(GuType.CULTIVATION)
                    .refine(16_000).costPerUse(160)
                    .hungerBar(16, 1).hungerPerUse(6).hungerPerDay(2).health(24).feed(ModItemTags.LIQUOR_FEED, 1)));
    public static final DeferredItem<Item> SEVEN_FRAGRANCES_LIQUOR_WORM = ITEMS.register(
            "seven_fragrances_liquor_worm",
            () -> new LiquorWormItem(createTendedProperties(), GuSpec.of(Rank.THREE, GuPath.FOOD)
                    .type(GuType.CULTIVATION)
                    .refine(160_000).costPerUse(1_600)
                    .hungerBar(32, 1).hungerPerUse(12).hungerPerDay(4).health(48).feed(ModItemTags.LIQUOR_FEED, 1)));
    public static final DeferredItem<Item> NINE_EYES_LIQUOR_WORM = ITEMS.register("nine_eyes_liquor_worm",
            () -> new LiquorWormItem(createTendedProperties(), GuSpec.of(Rank.FOUR, GuPath.FOOD)
                    .type(GuType.CULTIVATION)
                    .refine(1_600_000).costPerUse(16_000)
                    .hungerBar(64, 1).hungerPerUse(24).hungerPerDay(8).health(96).feed(ModItemTags.LIQUOR_FEED, 1)));
    //endregion

    //region Primeval Elder Gu [元老蛊] -- a vault for Primeval Stones [元石] that never needs feeding at all
    public static final DeferredItem<Item> PRIMEVAL_ELDER_GU_1 = ITEMS.register("primeval_elder_gu_1",
            () -> new PrimevalElderGuItem(createTendedProperties(), 1_000L, GuSpec.of(Rank.ONE, GuPath.SPACE)
                    .type(GuType.STORAGE)
                    .refine(16).costPerUse(0)));
    public static final DeferredItem<Item> PRIMEVAL_ELDER_GU_2 = ITEMS.register("primeval_elder_gu_2",
            () -> new PrimevalElderGuItem(createTendedProperties(), 10_000L, GuSpec.of(Rank.TWO, GuPath.SPACE)
                    .type(GuType.STORAGE)
                    .refine(160).costPerUse(0)));
    public static final DeferredItem<Item> PRIMEVAL_ELDER_GU_3 = ITEMS.register("primeval_elder_gu_3",
            () -> new PrimevalElderGuItem(createTendedProperties(), 100_000L, GuSpec.of(Rank.THREE, GuPath.SPACE)
                    .type(GuType.STORAGE)
                    .refine(1_600).costPerUse(0)));
    public static final DeferredItem<Item> PRIMEVAL_ELDER_GU_4 = ITEMS.register("primeval_elder_gu_4",
            () -> new PrimevalElderGuItem(createTendedProperties(), 1_000_000L, GuSpec.of(Rank.FOUR, GuPath.SPACE)
                    .type(GuType.STORAGE)
                    .refine(16_000).costPerUse(0)));
    public static final DeferredItem<Item> PRIMEVAL_ELDER_GU_5 = ITEMS.register("primeval_elder_gu_5",
            () -> new PrimevalElderGuItem(createTendedProperties(), 100_000_000L, GuSpec.of(Rank.FIVE, GuPath.SPACE)
                    .type(GuType.STORAGE)
                    .refine(160_000).costPerUse(0)));
    //endregion

    //region Heavenly Essence Treasure Lotus Gu [天元宝莲] -- wood path; 5% essence per second and minted stones
    public static final DeferredItem<Item> HEAVENLY_ESSENCE_TREASURE_LOTUS_GU = ITEMS.register(
            "heavenly_essence_treasure_lotus_gu",
            () -> new TreasureLotusGuItem(createTendedProperties(), 1, 100, GuSpec.of(Rank.THREE, GuPath.WOOD)
                    .type(GuType.CULTIVATION)
                    .refine(80_000).costPerUse(0)));
    public static final DeferredItem<Item> HEAVENLY_ESSENCE_TREASURE_MONARCH_LOTUS_GU = ITEMS.register(
            "heavenly_essence_treasure_monarch_lotus_gu",
            () -> new TreasureLotusGuItem(createTendedProperties(), 10, 1_000, GuSpec.of(Rank.FOUR, GuPath.WOOD)
                    .type(GuType.CULTIVATION)
                    .refine(800_000).costPerUse(0)));
    public static final DeferredItem<Item> HEAVENLY_ESSENCE_TREASURE_KING_LOTUS_GU = ITEMS.register(
            "heavenly_essence_treasure_king_lotus_gu",
            () -> new TreasureLotusGuItem(createTendedProperties(), 100, 10_000, GuSpec.of(Rank.FIVE, GuPath.WOOD)
                    .type(GuType.CULTIVATION)
                    .refine(8_000_000).costPerUse(0)));
    //endregion

    //region Nine Leaf Vitality Grass [九叶生机草] -- wood path; nine Vitality Leaf Gu, one regrown every 3 minutes
    // or grown at once for 80 essence
    public static final DeferredItem<Item> NINE_LEAF_VITALITY_GRASS = ITEMS.register("nine_leaf_vitality_grass",
            () -> new NineLeafVitalityGrassItem(createTendedProperties(), 3 * Ticks.MINUTE, 80,
                    GuSpec.of(Rank.THREE, GuPath.WOOD).type(GuType.HEALING).refine(80_000).costPerUse(0)));
    //endregion

    //region Zombie Gu [僵尸蛊] -- Transformation Path [变化道]; a timed Half-Zombie [半生半僵], and a 5-minute
    // window that makes it permanent
    public static final DeferredItem<Item> ROAMING_ZOMBIE_GU = ITEMS.register("roaming_zombie_gu",
            () -> new ZombieGuItem(createTendedProperties(), 2 * Ticks.MINUTE,
                    GuSpec.of(Rank.TWO, GuPath.TRANSFORMATION)
                            .refine(8_000).costPerUse(160)
                            .hungerBar(24, 1).hungerPerUse(8).hungerPerDay(2).health(24)
                            .feed(ModItemTags.ZOMBIE_FEED, 1)
                            .cooldown(Ticks.SECOND)));
    public static final DeferredItem<Item> HAIRY_ZOMBIE_GU = ITEMS.register("hairy_zombie_gu",
            () -> new ZombieGuItem(createTendedProperties(), 4 * Ticks.MINUTE,
                    GuSpec.of(Rank.THREE, GuPath.TRANSFORMATION)
                            .refine(80_000).costPerUse(1_600)
                            .hungerBar(48, 1).hungerPerUse(16).hungerPerDay(4).health(48)
                            .feed(ModItemTags.ZOMBIE_FEED, 1)
                            .cooldown(Ticks.SECOND)));
    public static final DeferredItem<Item> HOPPING_ZOMBIE_GU = ITEMS.register("hopping_zombie_gu",
            () -> new ZombieGuItem(createTendedProperties(), 6 * Ticks.MINUTE,
                    GuSpec.of(Rank.FOUR, GuPath.TRANSFORMATION)
                            .refine(800_000).costPerUse(16_000)
                            .hungerBar(96, 1).hungerPerUse(32).hungerPerDay(8).health(96)
                            .feed(ModItemTags.ZOMBIE_FEED, 1)
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
        return new ZombieGuItem(createTendedProperties(), 8 * Ticks.MINUTE, GuSpec.of(Rank.FIVE, GuPath.TRANSFORMATION)
                .refine(8_000_000).costPerUse(160_000)
                .hungerBar(192, 1).hungerPerUse(64).hungerPerDay(16).health(192).feed(ModItemTags.ZOMBIE_FEED, 1)
                .cooldown(Ticks.SECOND));
    }
    //endregion

    //region Watch Gu [更蛊] -- Time Path [宙道]; tended like any other, and taken by the one use it is kept for
    public static final DeferredItem<Item> SECOND_WATCH_GU = ITEMS.register("second_watch_gu",
            () -> new WatchGuItem(createTendedProperties(), ModEffects.SECOND_WATCH_GU, 5 * Ticks.MINUTE,
                    GuSpec.of(Rank.FOUR, GuPath.TIME).type(GuType.AMPLIFICATION)
                            .refine(100_000).costPerUse(0)
                            .cooldown(Ticks.SECOND)));
    public static final DeferredItem<Item> THIRD_WATCH_GU = ITEMS.register("third_watch_gu",
            () -> new WatchGuItem(createTendedProperties(), ModEffects.THIRD_WATCH_GU, 5 * Ticks.MINUTE,
                    GuSpec.of(Rank.FIVE, GuPath.TIME).type(GuType.AMPLIFICATION)
                            .refine(1_000_000).costPerUse(0)
                            .cooldown(Ticks.SECOND)));
    //endregion

    //region As Before Gu [如故蛊] -- Time Path [宙道]; never fed, four points and four health back one per half day
    public static final DeferredItem<Item> MAN_AS_BEFORE_GU = ITEMS.register("man_as_before_gu",
            () -> new ManAsBeforeGuItem(createTendedProperties(), GuSpec.of(Rank.FIVE, GuPath.TIME).type(GuType.HEALING)
                    .refine(1_000_000).costPerUse(320_000)
                    .recoverBar(4, Ticks.HALF_DAY).hungerPerUse(1).health(4)
                    .cooldown(Ticks.SECOND)));
    public static final DeferredItem<Item> MOUNTAIN_AS_BEFORE_GU = ITEMS.register("mountain_as_before_gu",
            () -> new TerrainAsBeforeGuItem(createTendedProperties(), ChunkKind.LAND, GuSpec.of(Rank.FIVE, GuPath.TIME)
                    .refine(1_000_000).costPerUse(640_000)
                    .recoverBar(4, Ticks.HALF_DAY).hungerPerUse(1).health(4)
                    .cooldown(Ticks.SECOND)));
    public static final DeferredItem<Item> RIVER_AS_BEFORE_GU = ITEMS.register("river_as_before_gu",
            () -> new TerrainAsBeforeGuItem(createTendedProperties(), ChunkKind.WATER, GuSpec.of(Rank.FIVE, GuPath.TIME)
                    .refine(1_000_000).costPerUse(640_000)
                    .recoverBar(4, Ticks.HALF_DAY).hungerPerUse(1).health(4)
                    .cooldown(Ticks.SECOND)));
    //endregion

    //region Malicious Thought Gu [恶念蛊] -- Wisdom Path [智道]; a one-use flood of evil thoughts, taken by its use
    public static final DeferredItem<Item> MALICIOUS_THOUGHT_GU_2 = ITEMS.register("malicious_thought_gu_2",
            () -> new MaliciousThoughtGuItem(createTendedProperties(), ModEffects.MALICIOUS_THOUGHT_GU, 64L,
                    GuSpec.of(Rank.TWO, GuPath.WISDOM).type(GuType.CULTIVATION)
                            .refine(1_000).costPerUse(0)
                            .hungerBar(16, 1).hungerPerUse(0).hungerPerDay(2).health(24)
                            .feed(ModItemTags.MALICIOUS_THOUGHT_FEED, 1)
                            .cooldown(Ticks.SECOND)));
    public static final DeferredItem<Item> MALICIOUS_THOUGHT_GU_3 = ITEMS.register("malicious_thought_gu_3",
            () -> new MaliciousThoughtGuItem(createTendedProperties(), ModEffects.MALICIOUS_THOUGHT_GU, 640L,
                    GuSpec.of(Rank.THREE, GuPath.WISDOM).type(GuType.CULTIVATION)
                            .refine(10_000).costPerUse(0)
                            .hungerBar(32, 1).hungerPerUse(0).hungerPerDay(4).health(48)
                            .feed(ModItemTags.MALICIOUS_THOUGHT_FEED, 1)
                            .cooldown(Ticks.SECOND)));
    public static final DeferredItem<Item> MALICIOUS_THOUGHT_GU_4 = ITEMS.register("malicious_thought_gu_4",
            () -> new MaliciousThoughtGuItem(createTendedProperties(), ModEffects.MALICIOUS_THOUGHT_GU, 6_400L,
                    GuSpec.of(Rank.FOUR, GuPath.WISDOM).type(GuType.CULTIVATION)
                            .refine(100_000).costPerUse(0)
                            .hungerBar(64, 1).hungerPerUse(0).hungerPerDay(8).health(96)
                            .feed(ModItemTags.MALICIOUS_THOUGHT_FEED, 1)
                            .cooldown(Ticks.SECOND)));
    public static final DeferredItem<Item> MALICIOUS_THOUGHT_GU_5 = ITEMS.register("malicious_thought_gu_5",
            () -> new MaliciousThoughtGuItem(createTendedProperties(), ModEffects.MALICIOUS_THOUGHT_GU, 64_000L,
                    GuSpec.of(Rank.FIVE, GuPath.WISDOM).type(GuType.CULTIVATION)
                            .refine(1_000_000).costPerUse(0)
                            .hungerBar(128, 1).hungerPerUse(0).hungerPerDay(16).health(192)
                            .feed(ModItemTags.MALICIOUS_THOUGHT_FEED, 1)
                            .cooldown(Ticks.SECOND)));
    //endregion

    //region Guts Gu [胆识蛊] -- Soul Path [魂道]; a one-shot Gu that raises the soul cap
    public static final DeferredItem<Item> GUTS_GU = ITEMS.register("guts_gu",
            () -> new GutsGuItem(createOneShotProperties(), GuSpec.of(Rank.ONE, GuPath.SOUL).type(GuType.CULTIVATION)));
    //endregion

    //region Casual Gu [随意蛊] -- Wisdom Path [智道]; ten seconds of random thoughts, taken by its use
    public static final DeferredItem<Item> CASUAL_GU_1 = ITEMS.register("casual_gu_1",
            () -> new CasualGuItem(createTendedProperties(), ModEffects.CASUAL_GU, GuSpec.of(Rank.ONE, GuPath.WISDOM)
                    .type(GuType.CULTIVATION)
                    .refine(100)
                    .costPerUse(0)
                    .hungerBar(8, 1).hungerPerUse(0)
                    .feed(ModItemTags.CASUAL_FEED, 1)
                    .cooldown(Ticks.SECOND)));
    public static final DeferredItem<Item> CASUAL_GU_2 = ITEMS.register("casual_gu_2",
            () -> new CasualGuItem(createTendedProperties(), ModEffects.CASUAL_GU, GuSpec.of(Rank.TWO, GuPath.WISDOM)
                    .type(GuType.CULTIVATION)
                    .refine(1_000)
                    .costPerUse(0)
                    .hungerBar(16, 1).hungerPerUse(0).hungerPerDay(2).health(24)
                    .feed(ModItemTags.CASUAL_FEED, 1)
                    .cooldown(Ticks.SECOND)));
    //endregion

    //region Stone Aperture Gu [石窍蛊] -- earth path; never feeds, taken by its use, and the aperture
    // it leaves stands on this rank's peak, petrified
    public static final DeferredItem<Item> STONE_APERTURE_GU_3 = ITEMS.register("stone_aperture_gu_3",
            () -> new StoneApertureGuItem(createTendedProperties(), GuSpec.of(Rank.THREE, GuPath.EARTH)
                    .type(GuType.AMPLIFICATION)
                    .refine(10_000).costPerUse(0)
                    .cooldown(Ticks.SECOND)));
    public static final DeferredItem<Item> STONE_APERTURE_GU_4 = ITEMS.register("stone_aperture_gu_4",
            () -> new StoneApertureGuItem(createTendedProperties(), GuSpec.of(Rank.FOUR, GuPath.EARTH)
                    .type(GuType.AMPLIFICATION)
                    .refine(100_000).costPerUse(0)
                    .cooldown(Ticks.SECOND)));
    public static final DeferredItem<Item> STONE_APERTURE_GU_5 = ITEMS.register("stone_aperture_gu_5",
            () -> new StoneApertureGuItem(createTendedProperties(), GuSpec.of(Rank.FIVE, GuPath.EARTH)
                    .type(GuType.AMPLIFICATION)
                    .refine(1_000_000).costPerUse(0)
                    .cooldown(Ticks.SECOND)));
    //endregion

    //region Amplify Effect Gu [功倍蛊] -- Rule Path [律道]; held in the offhand, it multiplies another Gu's effect
    // by its rank number; a bar of 16 at 4 a use
    public static final DeferredItem<Item> AMPLIFY_EFFECT_GU_2 = ITEMS.register("amplify_effect_gu_2",
            () -> createAmplifyEffectGu(Rank.TWO, 8_000, 160));
    public static final DeferredItem<Item> AMPLIFY_EFFECT_GU_3 = ITEMS.register("amplify_effect_gu_3",
            () -> createAmplifyEffectGu(Rank.THREE, 80_000, 1_600));
    public static final DeferredItem<Item> AMPLIFY_EFFECT_GU_4 = ITEMS.register("amplify_effect_gu_4",
            () -> createAmplifyEffectGu(Rank.FOUR, 800_000, 16_000));
    public static final DeferredItem<Item> AMPLIFY_EFFECT_GU_5 = ITEMS.register("amplify_effect_gu_5",
            () -> createAmplifyEffectGu(Rank.FIVE, 8_000_000, 160_000));

    private static AmplifyEffectGuItem createAmplifyEffectGu(Rank rank, int refine, int costPerUse) {
        return new AmplifyEffectGuItem(createTendedProperties(), GuSpec.of(rank, GuPath.RULE)
                .type(GuType.AMPLIFICATION)
                .refine(refine).costPerUse(costPerUse)
                .hungerBar(16, 1).hungerPerUse(4)
                .feed(ModItemTags.AMPLIFY_EFFECT_FEED, 1));
    }
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
        return new SecondApertureGuItem(createOneShotProperties(), GuSpec.of(rank, GuPath.HUMAN)
                .type(GuType.CULTIVATION));
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
    public static final DeferredItem<Item> PURE_WATER_BUCKET = ITEMS.register("pure_water_bucket",
            () -> new BucketItem(ModFluids.PURE_WATER.get(),
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
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

    //region Human Aperture [人窍] -- pure Gu material, ranks I to V; a wiped death drops one per aperture
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

    //region Qi Path [气道] materials -- 21, ranks I to V
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
            () -> new LifeQiItem(createQiProperties(), Rank.ONE, getQiEssenceCost(Rank.ONE)));
    public static final DeferredItem<Item> LIFE_QI_2 = ITEMS.register("life_qi_2",
            () -> new LifeQiItem(createQiProperties(), Rank.TWO, getQiEssenceCost(Rank.TWO)));
    public static final DeferredItem<Item> LIFE_QI_3 = ITEMS.register("life_qi_3",
            () -> new LifeQiItem(createQiProperties(), Rank.THREE, getQiEssenceCost(Rank.THREE)));
    public static final DeferredItem<Item> LIFE_QI_4 = ITEMS.register("life_qi_4",
            () -> new LifeQiItem(createQiProperties(), Rank.FOUR, getQiEssenceCost(Rank.FOUR)));
    public static final DeferredItem<Item> LIFE_QI_5 = ITEMS.register("life_qi_5",
            () -> new LifeQiItem(createQiProperties(), Rank.FIVE, getQiEssenceCost(Rank.FIVE)));
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
            () -> new DeathQiItem(createQiProperties(), Rank.FIVE, getQiEssenceCost(Rank.FIVE)));

    private static DeferredItem<Item> registerQiMaterial(String id, Rank rank, QiKind kind) {
        return ITEMS.register(id, () -> new QiMaterialItem(createQiProperties(), rank, kind, getQiEssenceCost(rank)));
    }

    private static long getQiEssenceCost(Rank rank) { return QI_ESSENCE_COST[rank.ordinal() - Rank.ONE.ordinal()]; }

    private static Item.Properties createQiProperties() { return new Item.Properties().stacksTo(64); }
    //endregion

    //region Advancement Icon [进度图标] -- no tab, no recipe viewer; only advancements show it
    public static final DeferredItem<AdvancementIconItem> ADVANCEMENT_ICON = ITEMS.register("advancement_icon",
            () -> new AdvancementIconItem(new Item.Properties().stacksTo(1)));
    //endregion

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
