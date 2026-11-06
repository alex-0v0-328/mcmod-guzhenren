package net.alex.guzhenren.registry.item;

import java.util.function.Predicate;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.entity.projectile.MoonBladeEntity;
import net.alex.guzhenren.effect.timed.BruteForceLonghornBeetleGuEffect;
import net.alex.guzhenren.effect.timed.CasualGuEffect;
import net.alex.guzhenren.effect.timed.FlowerBoarGuEffect;
import net.alex.guzhenren.effect.timed.HardshipStrengthGuEffect;
import net.alex.guzhenren.effect.timed.MaliciousThoughtGuEffect;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.body.Bone;
import net.alex.guzhenren.gameplay.body.ShieldType;
import net.alex.guzhenren.gameplay.body.Skin;
import net.alex.guzhenren.gameplay.body.Tendon;
import net.alex.guzhenren.gameplay.mind.ThoughtTag;
import net.alex.guzhenren.gameplay.path.EffectCarrier;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.HumanStrength;
import net.alex.guzhenren.gameplay.path.strength.StrengthPathBranch;
import net.alex.guzhenren.gameplay.rewind.ChunkKind;
import net.alex.guzhenren.item.AdvancementIconItem;
import net.alex.guzhenren.item.KillingMoveBookItem;
import net.alex.guzhenren.item.gu.tending.ConsumedGuItem;
import net.alex.guzhenren.item.gu.GuBehavior;
import net.alex.guzhenren.item.gu.GuProperties;
import net.alex.guzhenren.item.gu.GuType;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.OneShotGuItem;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.alex.guzhenren.item.gu.killingmove.KillingMove;
import net.alex.guzhenren.item.gu.mortal.behavior.RecastBehavior;
import net.alex.guzhenren.item.gu.mortal.behavior.SkinBehavior;
import net.alex.guzhenren.item.gu.mortal.behavior.TimedBuffBehavior;
import net.alex.guzhenren.item.gu.mortal.path.earth.PetrifyBehavior;
import net.alex.guzhenren.item.gu.mortal.path.food.DistillBehavior;
import net.alex.guzhenren.item.gu.mortal.path.heaven.LifespanBehavior;
import net.alex.guzhenren.item.gu.mortal.path.heaven.StageUpBehavior;
import net.alex.guzhenren.item.gu.mortal.path.human.HopeGuItem;
import net.alex.guzhenren.item.gu.mortal.path.human.SecondApertureBehavior;
import net.alex.guzhenren.item.gu.mortal.path.light.LittleLightGuItem;
import net.alex.guzhenren.item.gu.mortal.path.metal.BoneBehavior;
import net.alex.guzhenren.item.gu.mortal.path.metal.TendonBehavior;
import net.alex.guzhenren.item.gu.mortal.path.moon.MoonBladeBehavior;
import net.alex.guzhenren.item.gu.mortal.path.rule.AmplifyEffectGuItem;
import net.alex.guzhenren.item.gu.mortal.path.soul.SoulCapBehavior;
import net.alex.guzhenren.item.gu.mortal.path.space.PrimevalElderGuItem;
import net.alex.guzhenren.item.gu.mortal.path.strength.AllOutEffortBehavior;
import net.alex.guzhenren.item.gu.mortal.path.strength.AutoUseBehavior;
import net.alex.guzhenren.item.gu.mortal.path.strength.BeastStrengthBehavior;
import net.alex.guzhenren.item.gu.mortal.path.strength.HumanStrengthBehavior;
import net.alex.guzhenren.item.gu.mortal.path.strength.SelfRelianceBehavior;
import net.alex.guzhenren.item.gu.mortal.path.time.ManAsBeforeGuItem;
import net.alex.guzhenren.item.gu.mortal.path.time.TerrainAsBeforeGuItem;
import net.alex.guzhenren.item.gu.mortal.path.time.WatchBehavior;
import net.alex.guzhenren.item.gu.mortal.path.transformation.ZombieBehavior;
import net.alex.guzhenren.item.gu.mortal.path.water.ShieldBehavior;
import net.alex.guzhenren.item.gu.mortal.path.wisdom.ThoughtBurstBehavior;
import net.alex.guzhenren.item.gu.mortal.path.wisdom.ThoughtBehavior;
import net.alex.guzhenren.item.gu.mortal.path.wood.NineLeafVitalityGrassItem;
import net.alex.guzhenren.item.gu.mortal.path.wood.TreasureLotusGuItem;
import net.alex.guzhenren.item.gu.mortal.path.wood.VitalityLeafBehavior;
import net.alex.guzhenren.item.material.GuMaterialItem;
import net.alex.guzhenren.item.material.LiquorItem;
import net.alex.guzhenren.item.material.PrimevalStoneItem;
import net.alex.guzhenren.registry.block.ModBlocks;
import net.alex.guzhenren.registry.builder.ItemModel;
import net.alex.guzhenren.registry.builder.ItemRegistrar;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.alex.guzhenren.registry.effect.ModPathEffectTypes;
import net.alex.guzhenren.registry.fluid.ModFluids;
import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

/**
 * Every item and the only place a Gu's numbers actually live.
 *
 * <p>DeferredRegister holder: the {@link GuProperties} chain on each registration, or the family helper right
 * under its ladder, IS the truth. A figure written down anywhere else is a copy of it, and when the two disagree
 * this file is the one that is right. The two shared helpers give every family its bar: {@link
 * #createRankedProperties}, the bar, use, day and health of every Gu paid per use by its rank, and {@link
 * #createChannelProperties}, whose channeled round [灌注] bills by progress. A family helper takes the figures
 * that differ along the ladder as arguments, so the ladder reads as table rows; a chain spells its facts in
 * one order -- type, branch, effect, refine, essence, bar, feed, cooldown -- one call a line. {@link #findGuWith}
 * finds the Gu a body state was made by, through the behavior that made it, for the path effect it carries. One
 * {@link ItemRegistrar} chain per item, carrying its factory, both names, its model shape, its tags and any tab,
 * plus the PNG. Past 300 lines on purpose: it is a data table, and splitting it would scatter the one place the
 * numbers live.
 *
 * <p>⚠ The Gu regions follow {@link GuPath}'s order, one path a region, a branch or family a region inside it;
 * materials, books and the icon close the file. Registration order is the creative tabs' order
 * ({@code ModCreativeTabs} walks {@link #ITEMS}), so a Gu moves tab position with its row. The registration id
 * mirrors the Java name (and the PNG); a rank ladder is numbered ({@code primeval_elder_gu_1} to
 * {@code primeval_elder_gu_5}), but an item with its own fiction name keeps that name ({@code blood_wight_gu}).
 *
 * <p>{@link #SPIRIT_SPRING} is the spring's {@code BlockItem}: same key as the block, so the names on its chain
 * name the block and the fluid too, and its icon is the fluid's still strip ({@code ItemModel.FLUID}) -- no item
 * PNG of its own. {@link #PURE_WATER_BUCKET} is the vanilla water
 * bucket's twin for the Pure Water [纯水]: it places a source, an empty bucket scoops one back, and a
 * dispenser pours it ({@code PureWaterEvents}). {@link #ADVANCEMENT_ICON} is no game item: it carries the
 * realm advancements' numerals ({@link AdvancementIconItem}).
 *
 * @author Alex
 * @version 1.0.0
 * @see GuProperties
 * @since 1.0.0
 */

public final class ModItems {

    private ModItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guzhenren.MOD_ID);
    private static final ItemRegistrar REGISTRAR = ItemRegistrar.of(ITEMS);
    private static final long PRIMEVAL_STONE_ESSENCE = 20L;
    private static final float MOONLIGHT_DAMAGE = 6.0F;
    /** A moon blade Gu fires again after 0.1 s: the blade itself is the pacing, not a cooldown sweep. */
    private static final int MOON_BLADE_COOLDOWN_TICKS = 2;
    private static final int HUNGER_PER_USE = 3;
    private static final int HUNGER_PER_DAY = 1;
    private static final int HUNGER_PER_ROUND = 10;

    public static <T extends Item> @Nullable T findGu(Class<T> type, Predicate<T> test) {
        for (DeferredHolder<Item, ? extends Item> entry : ITEMS.getEntries()) {
            Item item = entry.get();
            if (type.isInstance(item) && test.test(type.cast(item))) return type.cast(item);
        }
        return null;
    }

    public static <M extends GuBehavior> @Nullable MortalGuItem findGuWith(Class<M> behavior, Predicate<M> test) {
        for (DeferredHolder<Item, ? extends Item> entry : ITEMS.getEntries()) {
            if (!(entry.get() instanceof MortalGuItem gu)) continue;

            M found = gu.getBehavior(behavior);
            if (found != null && test.test(found)) return gu;
        }
        return null;
    }

    //region Shared bars [共用饱食条] -- the two bars every tended Gu family starts from
    /**
     * A tended Gu paid per use: a bar of 2^(rank+1) uses of {@value #HUNGER_PER_USE} plus a buffer of two days'
     * drain of {@value #HUNGER_PER_DAY} (the buffer is below one use), so 14, 26, 50, 98, 194 from rank I to V.
     */
    private static GuProperties createRankedProperties(Rank rank, GuPath path) {
        return GuProperties.of(rank, path)
                .hungerBar(3 * (2 << rank.ordinal()) + 2 * HUNGER_PER_DAY, 1)
                .hungerPerUse(HUNGER_PER_USE)
                .hungerPerDay(HUNGER_PER_DAY);
    }

    /**
     * A channeled Gu [灌注]: a round bills {@value #HUNGER_PER_ROUND} by its progress, never by its essence; the bar
     * holds one round at ranks I and II, three from rank III, plus the same two days' buffer, so 12, 12, 32, 32.
     */
    private static GuProperties createChannelProperties(Rank rank, GuPath path) {
        int rounds = rank.compareTo(Rank.THREE) < 0 ? 1 : 3;
        return GuProperties.of(rank, path)
                .hungerBar(rounds * HUNGER_PER_ROUND + 2 * HUNGER_PER_DAY, 1)
                .hungerPerRound(HUNGER_PER_ROUND)
                .hungerPerDay(HUNGER_PER_DAY);
    }
    //endregion

    //region Heaven Path [天道] -- one-shot Gu: Lifespan Gu [寿蛊] add years, Relics Gu [舍利蛊] step the stage up
    public static final DeferredItem<Item> LIFESPAN_GU = REGISTRAR.item("lifespan_gu",
            () -> createLifespanGu(1, 9))
            .lang("Lifespan Gu", "寿蛊").register();
    public static final DeferredItem<Item> TENS_LIFESPAN_GU = REGISTRAR.item("tens_lifespan_gu",
            () -> createLifespanGu(10, 19))
            .lang("Ten-Year Lifespan Gu", "十年寿蛊").register();
    public static final DeferredItem<Item> HUNDREDS_LIFESPAN_GU = REGISTRAR.item("hundreds_lifespan_gu",
            () -> createLifespanGu(100, 199))
            .lang("Hundred-Year Lifespan Gu", "百年寿蛊").register();
    public static final DeferredItem<Item> THOUSANDS_LIFESPAN_GU = REGISTRAR.item("thousands_lifespan_gu",
            () -> createLifespanGu(1000, 1999))
            .lang("Thousand-Year Lifespan Gu", "千年寿蛊").register();
    public static final DeferredItem<Item> COPPER_RELICS_GU = REGISTRAR.item("copper_relics_gu",
            () -> createRelicsGu(Rank.ONE))
            .lang("Green Copper Relics Gu", "青铜舍利蛊").register();
    public static final DeferredItem<Item> STEEL_RELICS_GU = REGISTRAR.item("steel_relics_gu",
            () -> createRelicsGu(Rank.TWO))
            .lang("Red Steel Relics Gu", "赤铁舍利蛊").register();
    public static final DeferredItem<Item> SILVER_RELICS_GU = REGISTRAR.item("silver_relics_gu",
            () -> createRelicsGu(Rank.THREE))
            .lang("White Silver Relics Gu", "白银舍利蛊").register();
    public static final DeferredItem<Item> GOLD_RELICS_GU = REGISTRAR.item("gold_relics_gu",
            () -> createRelicsGu(Rank.FOUR))
            .lang("Yellow Gold Relics Gu", "黄金舍利蛊").register();
    public static final DeferredItem<Item> CRYSTAL_RELICS_GU = REGISTRAR.item("crystal_relics_gu",
            () -> createRelicsGu(Rank.FIVE))
            .lang("Purple Crystal Relics Gu", "紫晶舍利蛊").register();

    private static OneShotGuItem createLifespanGu(int minYears, int maxYears) {
        return new OneShotGuItem(GuProperties.of(Rank.ONE, GuPath.HEAVEN),
                new LifespanBehavior(minYears, maxYears));
    }

    private static OneShotGuItem createRelicsGu(Rank rank) {
        return new OneShotGuItem(GuProperties.of(rank, GuPath.HEAVEN)
                .type(GuType.CULTIVATION),
                new StageUpBehavior());
    }
    //endregion

    //region Rule Path [律道] -- Amplify Effect Gu [功倍蛊]: held in the offhand, it multiplies another Gu's effect
    // by its rank number; the ranked bar, paid per amplified use
    public static final DeferredItem<Item> AMPLIFY_EFFECT_GU_2 = REGISTRAR.item("amplify_effect_gu_2",
            () -> createAmplifyEffectGu(Rank.TWO, 1_600, 36))
            .lang("Amplify Effect Gu II", "二转功倍蛊").register();
    public static final DeferredItem<Item> AMPLIFY_EFFECT_GU_3 = REGISTRAR.item("amplify_effect_gu_3",
            () -> createAmplifyEffectGu(Rank.THREE, 3_200, 108))
            .lang("Amplify Effect Gu III", "三转功倍蛊").register();
    public static final DeferredItem<Item> AMPLIFY_EFFECT_GU_4 = REGISTRAR.item("amplify_effect_gu_4",
            () -> createAmplifyEffectGu(Rank.FOUR, 6_400, 324))
            .lang("Amplify Effect Gu IV", "四转功倍蛊").register();
    public static final DeferredItem<Item> AMPLIFY_EFFECT_GU_5 = REGISTRAR.item("amplify_effect_gu_5",
            () -> createAmplifyEffectGu(Rank.FIVE, 12_800, 972))
            .lang("Amplify Effect Gu V", "五转功倍蛊").register();

    private static AmplifyEffectGuItem createAmplifyEffectGu(Rank rank, int refine, int costPerUse) {
        return new AmplifyEffectGuItem(createRankedProperties(rank, GuPath.RULE)
                .type(GuType.AMPLIFICATION)
                .effect(EffectCarrier.BODY)
                .refine(refine)
                .costPerUse(costPerUse)
                .feed(ModItemTags.AMPLIFY_EFFECT_FEED, 1));
    }
    //endregion

    //region Space Path [空道] -- Primeval Elder Gu [元老蛊]: a vault for Primeval Stones [元石] that never needs
    // feeding at all
    public static final DeferredItem<Item> PRIMEVAL_ELDER_GU_1 = REGISTRAR.item("primeval_elder_gu_1",
            () -> createPrimevalElderGu(Rank.ONE, 1_000L, 800))
            .lang("Primeval Elder Gu I", "一转元老蛊").register();
    public static final DeferredItem<Item> PRIMEVAL_ELDER_GU_2 = REGISTRAR.item("primeval_elder_gu_2",
            () -> createPrimevalElderGu(Rank.TWO, 10_000L, 1_600))
            .lang("Primeval Elder Gu II", "二转元老蛊").register();
    public static final DeferredItem<Item> PRIMEVAL_ELDER_GU_3 = REGISTRAR.item("primeval_elder_gu_3",
            () -> createPrimevalElderGu(Rank.THREE, 100_000L, 3_200))
            .lang("Primeval Elder Gu III", "三转元老蛊").register();
    public static final DeferredItem<Item> PRIMEVAL_ELDER_GU_4 = REGISTRAR.item("primeval_elder_gu_4",
            () -> createPrimevalElderGu(Rank.FOUR, 1_000_000L, 6_400))
            .lang("Primeval Elder Gu IV", "四转元老蛊").register();
    public static final DeferredItem<Item> PRIMEVAL_ELDER_GU_5 = REGISTRAR.item("primeval_elder_gu_5",
            () -> createPrimevalElderGu(Rank.FIVE, 100_000_000L, 12_800))
            .lang("Primeval Elder Gu V", "五转元老蛊").register();

    private static PrimevalElderGuItem createPrimevalElderGu(Rank rank, long capacity, int refine) {
        return new PrimevalElderGuItem(capacity, GuProperties.of(rank, GuPath.SPACE)
                .type(GuType.STORAGE)
                .refine(refine)
                .costPerUse(0));
    }
    //endregion

    //region Time Path [宙道] -- Watch Gu [更蛊] tended like any other and taken by the one use it is kept for;
    // As Before Gu [如故蛊] never fed, nine points and 24 health back one per third of a day
    public static final DeferredItem<Item> SECOND_WATCH_GU = REGISTRAR.item("second_watch_gu",
            () -> createWatchGu(Rank.FOUR, 6_400, ModEffects.SECOND_WATCH_GU))
            .lang("Second Watch Gu", "两更蛊").register();
    public static final DeferredItem<Item> THIRD_WATCH_GU = REGISTRAR.item("third_watch_gu",
            () -> createWatchGu(Rank.FIVE, 12_800, ModEffects.THIRD_WATCH_GU))
            .lang("Third Watch Gu", "三更蛊").register();
    public static final DeferredItem<Item> MAN_AS_BEFORE_GU = REGISTRAR.item("man_as_before_gu",
            () -> new ManAsBeforeGuItem(createAsBeforeProperties().type(GuType.HEALING)))
            .lang("Man As Before Gu", "人如故").register();
    public static final DeferredItem<Item> MOUNTAIN_AS_BEFORE_GU = REGISTRAR.item("mountain_as_before_gu",
            () -> new TerrainAsBeforeGuItem(ChunkKind.LAND, createAsBeforeProperties()))
            .lang("Mountain As Before Gu", "山如故").register();
    public static final DeferredItem<Item> RIVER_AS_BEFORE_GU = REGISTRAR.item("river_as_before_gu",
            () -> new TerrainAsBeforeGuItem(ChunkKind.WATER, createAsBeforeProperties()))
            .lang("River As Before Gu", "江如故").register();

    private static ConsumedGuItem createWatchGu(Rank rank, int refine, Holder<MobEffect> form) {
        return new ConsumedGuItem(GuProperties.of(rank, GuPath.TIME)
                .type(GuType.AMPLIFICATION)
                .effect(EffectCarrier.BODY)
                .refine(refine)
                .costPerUse(0)
                .cooldown(Ticks.SECOND),
                new WatchBehavior(form, 5 * Ticks.MINUTE));
    }

    private static GuProperties createAsBeforeProperties() {
        return GuProperties.of(Rank.FIVE, GuPath.TIME)
                .refine(12_800)
                .costPerUse(972)
                .recoverBar(9, Ticks.DAY / 3)
                .hungerPerUse(2)
                .cooldown(Ticks.SECOND);
    }
    //endregion

    //region Human Path [人道] -- Hope Gu [希望蛊] opens the aperture in a ritual; Second Aperture Gu [第二空窍蛊]
    // opens or upgrades the second aperture, a free one-shot taken by its use; Grade-A at 8/10, this rank's first
    // stage, never a physique
    public static final DeferredItem<Item> HOPE_GU = REGISTRAR.item("hope_gu",
            () -> new HopeGuItem(GuProperties.of(Rank.ONE, GuPath.HUMAN)
                    .type(GuType.CULTIVATION)))
            .lang("Hope Gu", "希望蛊").register();
    public static final DeferredItem<Item> SECOND_APERTURE_GU_1 = REGISTRAR.item("second_aperture_gu_1",
            () -> createSecondApertureGu(Rank.ONE))
            .lang("Second Aperture Gu I", "一转第二空窍蛊").register();
    public static final DeferredItem<Item> SECOND_APERTURE_GU_2 = REGISTRAR.item("second_aperture_gu_2",
            () -> createSecondApertureGu(Rank.TWO))
            .lang("Second Aperture Gu II", "二转第二空窍蛊").register();
    public static final DeferredItem<Item> SECOND_APERTURE_GU_3 = REGISTRAR.item("second_aperture_gu_3",
            () -> createSecondApertureGu(Rank.THREE))
            .lang("Second Aperture Gu III", "三转第二空窍蛊").register();
    public static final DeferredItem<Item> SECOND_APERTURE_GU_4 = REGISTRAR.item("second_aperture_gu_4",
            () -> createSecondApertureGu(Rank.FOUR))
            .lang("Second Aperture Gu IV", "四转第二空窍蛊").register();
    public static final DeferredItem<Item> SECOND_APERTURE_GU_5 = REGISTRAR.item("second_aperture_gu_5",
            () -> createSecondApertureGu(Rank.FIVE))
            .lang("Second Aperture Gu V", "五转第二空窍蛊").register();

    private static OneShotGuItem createSecondApertureGu(Rank rank) {
        return new OneShotGuItem(GuProperties.of(rank, GuPath.HUMAN)
                .type(GuType.CULTIVATION),
                new SecondApertureBehavior());
    }
    //endregion

    //region Metal Path [金道] -- body tempering [炼体]: bronze and iron skins, steel tendons and iron bones for good;
    // one channeled round eats the whole bar and tempers the part
    public static final DeferredItem<Item> BRONZE_SKIN_GU_1 = REGISTRAR.item("bronze_skin_gu_1",
            () -> createTemperingGu(Rank.ONE, 800, 1_200, ModItemTags.BRONZE_SKIN_FEED,
                    new SkinBehavior(Skin.BRONZE_SKIN_1, 0)))
            .lang("Bronze Skin Gu I", "一转铜皮蛊").register();
    public static final DeferredItem<Item> BRONZE_SKIN_GU_2 = REGISTRAR.item("bronze_skin_gu_2",
            () -> createTemperingGu(Rank.TWO, 1_600, 2_400, ModItemTags.BRONZE_SKIN_FEED,
                    new SkinBehavior(Skin.BRONZE_SKIN_2, 0)))
            .lang("Bronze Skin Gu II", "二转铜皮蛊").register();
    public static final DeferredItem<Item> BRONZE_SKIN_GU_3 = REGISTRAR.item("bronze_skin_gu_3",
            () -> createTemperingGu(Rank.THREE, 3_200, 4_800, ModItemTags.BRONZE_SKIN_FEED,
                    new SkinBehavior(Skin.BRONZE_SKIN_3, 0)))
            .lang("Bronze Skin Gu III", "三转铜皮蛊").register();
    public static final DeferredItem<Item> BRONZE_SKIN_GU_4 = REGISTRAR.item("bronze_skin_gu_4",
            () -> createTemperingGu(Rank.FOUR, 6_400, 9_600, ModItemTags.BRONZE_SKIN_FEED,
                    new SkinBehavior(Skin.BRONZE_SKIN_4, 0)))
            .lang("Bronze Skin Gu IV", "四转铜皮蛊").register();
    public static final DeferredItem<Item> IRON_SKIN_GU_1 = REGISTRAR.item("iron_skin_gu_1",
            () -> createTemperingGu(Rank.ONE, 800, 1_200, ModItemTags.IRON_SKIN_FEED,
                    new SkinBehavior(Skin.IRON_SKIN_1, 0)))
            .lang("Iron Skin Gu I", "一转铁皮蛊").register();
    public static final DeferredItem<Item> IRON_SKIN_GU_2 = REGISTRAR.item("iron_skin_gu_2",
            () -> createTemperingGu(Rank.TWO, 1_600, 2_400, ModItemTags.IRON_SKIN_FEED,
                    new SkinBehavior(Skin.IRON_SKIN_2, 0)))
            .lang("Iron Skin Gu II", "二转铁皮蛊").register();
    public static final DeferredItem<Item> IRON_SKIN_GU_3 = REGISTRAR.item("iron_skin_gu_3",
            () -> createTemperingGu(Rank.THREE, 3_200, 4_800, ModItemTags.IRON_SKIN_FEED,
                    new SkinBehavior(Skin.IRON_SKIN_3, 0)))
            .lang("Iron Skin Gu III", "三转铁皮蛊").register();
    public static final DeferredItem<Item> IRON_SKIN_GU_4 = REGISTRAR.item("iron_skin_gu_4",
            () -> createTemperingGu(Rank.FOUR, 6_400, 9_600, ModItemTags.IRON_SKIN_FEED,
                    new SkinBehavior(Skin.IRON_SKIN_4, 0)))
            .lang("Iron Skin Gu IV", "四转铁皮蛊").register();
    public static final DeferredItem<Item> STEEL_TENDON_GU_3 = REGISTRAR.item("steel_tendon_gu_3",
            () -> createTemperingGu(Rank.THREE, 3_200, 4_800, ModItemTags.STEEL_TENDON_FEED,
                    new TendonBehavior(Tendon.STEEL_TENDON_3)))
            .lang("Steel Tendon Gu III", "三转钢筋蛊").register();
    public static final DeferredItem<Item> STEEL_TENDON_GU_4 = REGISTRAR.item("steel_tendon_gu_4",
            () -> createTemperingGu(Rank.FOUR, 6_400, 9_600, ModItemTags.STEEL_TENDON_FEED,
                    new TendonBehavior(Tendon.STEEL_TENDON_4)))
            .lang("Steel Tendon Gu IV", "四转钢筋蛊").register();
    public static final DeferredItem<Item> IRON_BONE_GU_3 = REGISTRAR.item("iron_bone_gu_3",
            () -> createTemperingGu(Rank.THREE, 3_200, 4_800, ModItemTags.IRON_BONE_FEED,
                    new BoneBehavior(Bone.IRON_BONE_3)))
            .lang("Iron Bone Gu III", "三转铁骨蛊").register();
    public static final DeferredItem<Item> IRON_BONE_GU_4 = REGISTRAR.item("iron_bone_gu_4",
            () -> createTemperingGu(Rank.FOUR, 6_400, 9_600, ModItemTags.IRON_BONE_FEED,
                    new BoneBehavior(Bone.IRON_BONE_4)))
            .lang("Iron Bone Gu IV", "四转铁骨蛊").register();

    private static TendedGuItem createTemperingGu(Rank rank, int refine, int essencePerRound, TagKey<Item> feed,
                                                  GuBehavior tempering) {
        return new TendedGuItem(createChannelProperties(rank, GuPath.METAL)
                .type(GuType.PATH_MARK_CARVING)
                .effect(EffectCarrier.BODY)
                .refine(refine)
                .channel(essencePerRound)
                .feed(feed, 1),
                tempering);
    }
    //endregion

    //region Wood Path [木道] -- Vitality Leaf Gu [生机叶蛊] heals once; Heavenly Essence Treasure Lotus [天元宝莲]
    // mints 5% essence per second into stones; Nine Leaf Vitality Grass [九叶生机草] regrows nine Vitality Leaf Gu, one
    // every 3 minutes or all at once for 80 essence
    public static final DeferredItem<Item> VITALITY_LEAF_GU = REGISTRAR.item("vitality_leaf_gu",
            () -> new OneShotGuItem(GuProperties.of(Rank.ONE, GuPath.WOOD)
                    .type(GuType.HEALING)
                    .effect(EffectCarrier.BODY),
                    new VitalityLeafBehavior()))
            .lang("Vitality Leaf Gu", "生机叶蛊").register();
    public static final DeferredItem<Item> HEAVENLY_ESSENCE_TREASURE_LOTUS_GU = REGISTRAR.item(
            "heavenly_essence_treasure_lotus_gu",
            () -> createTreasureLotusGu(Rank.THREE, 1, 100, 3_200))
            .lang("Heavenly Essence Treasure Lotus Gu", "天元宝莲").register();
    public static final DeferredItem<Item> HEAVENLY_ESSENCE_TREASURE_MONARCH_LOTUS_GU = REGISTRAR.item(
            "heavenly_essence_treasure_monarch_lotus_gu",
            () -> createTreasureLotusGu(Rank.FOUR, 10, 1_000, 6_400))
            .lang("Heavenly Essence Treasure Monarch Lotus Gu", "天元宝君莲").register();
    public static final DeferredItem<Item> HEAVENLY_ESSENCE_TREASURE_KING_LOTUS_GU = REGISTRAR.item(
            "heavenly_essence_treasure_king_lotus_gu",
            () -> createTreasureLotusGu(Rank.FIVE, 100, 10_000, 12_800))
            .lang("Heavenly Essence Treasure King Lotus Gu", "天元宝王莲").register();
    public static final DeferredItem<Item> NINE_LEAF_VITALITY_GRASS = REGISTRAR.item("nine_leaf_vitality_grass",
            () -> new NineLeafVitalityGrassItem(3 * Ticks.MINUTE, 80, GuProperties.of(Rank.THREE, GuPath.WOOD)
                    .type(GuType.HEALING)
                    .refine(3_200)
                    .costPerUse(0)))
            .lang("Nine Leaf Vitality Grass", "九叶生机草").model(ItemModel.LEAF_STAGES).register();

    private static TreasureLotusGuItem createTreasureLotusGu(Rank rank, int stonesPerSecond, int stonesPerHealth,
                                                             int refine) {
        return new TreasureLotusGuItem(stonesPerSecond, stonesPerHealth, GuProperties.of(rank, GuPath.WOOD)
                .type(GuType.CULTIVATION)
                .refine(refine)
                .costPerUse(0));
    }
    //endregion

    //region Water Path [水道] -- Heavenly Mugwort Gu [天蓬蛊]: a shield for a while
    public static final DeferredItem<Item> HEAVENLY_MUGWORT_GU = REGISTRAR.item("heavenly_mugwort_gu",
            () -> new TendedGuItem(createRankedProperties(Rank.THREE, GuPath.WATER)
                    .type(GuType.DEFENSIVE)
                    .effect(EffectCarrier.BODY)
                    .refine(3_200)
                    .costPerUse(108)
                    .feed(ModItemTags.SMOOTH_STONE_FEED, 1)
                    .cooldown(30 * Ticks.SECOND, Ticks.SECOND),
                    new ShieldBehavior(ShieldType.HEAVENLY_MUGWORT, 180.0F, 30 * Ticks.SECOND),
                    new RecastBehavior()))
            .lang("Heavenly Mugwort Gu", "天蓬蛊").register();
    //endregion

    //region Earth Path [土道] -- Jade Skin Gu [玉皮蛊] and White Jade Gu [白玉蛊]: a defensive skin for a while;
    // Stone Aperture Gu [石窍蛊] never feeds, is taken by its use, and the aperture it leaves stands on this rank's
    // peak, petrified
    public static final DeferredItem<Item> JADE_SKIN_GU = REGISTRAR.item("jade_skin_gu",
            () -> createJadeSkinGu(Rank.ONE, 800, 12, ModItemTags.JADE_SKIN_FEED, Skin.JADE_SKIN, 60 * Ticks.SECOND))
            .lang("Jade Skin Gu", "玉皮蛊").register();
    public static final DeferredItem<Item> WHITE_JADE_GU = REGISTRAR.item("white_jade_gu",
            () -> createJadeSkinGu(Rank.TWO, 1_600, 36, ModItemTags.WHITE_JADE_FEED, Skin.WHITE_JADE_SKIN,
                    90 * Ticks.SECOND))
            .lang("White Jade Gu", "白玉蛊").register();
    public static final DeferredItem<Item> STONE_APERTURE_GU_3 = REGISTRAR.item("stone_aperture_gu_3",
            () -> createStoneApertureGu(Rank.THREE, 3_200))
            .lang("Stone Aperture Gu III", "三转石窍蛊").register();
    public static final DeferredItem<Item> STONE_APERTURE_GU_4 = REGISTRAR.item("stone_aperture_gu_4",
            () -> createStoneApertureGu(Rank.FOUR, 6_400))
            .lang("Stone Aperture Gu IV", "四转石窍蛊").register();
    public static final DeferredItem<Item> STONE_APERTURE_GU_5 = REGISTRAR.item("stone_aperture_gu_5",
            () -> createStoneApertureGu(Rank.FIVE, 12_800))
            .lang("Stone Aperture Gu V", "五转石窍蛊").register();

    private static TendedGuItem createJadeSkinGu(Rank rank, int refine, int costPerUse, TagKey<Item> feed, Skin skin,
                                                 int durationTicks) {
        return new TendedGuItem(createRankedProperties(rank, GuPath.EARTH)
                .type(GuType.DEFENSIVE)
                .effect(EffectCarrier.BODY)
                .refine(refine)
                .costPerUse(costPerUse)
                .feed(feed, 1)
                .cooldown(30 * Ticks.SECOND, Ticks.SECOND),
                new SkinBehavior(skin, durationTicks));
    }

    private static ConsumedGuItem createStoneApertureGu(Rank rank, int refine) {
        return new ConsumedGuItem(GuProperties.of(rank, GuPath.EARTH)
                .type(GuType.AMPLIFICATION)
                .effect(EffectCarrier.APERTURE)
                .refine(refine)
                .costPerUse(0)
                .cooldown(Ticks.SECOND),
                new PetrifyBehavior());
    }
    //endregion

    //region Light Path [光道] -- Little Light Gu [小光蛊]: glows in the hand, shines bright on a use and eats while
    // it does; a killing move's partner, its use cost the one it pays there
    public static final DeferredItem<Item> LITTLE_LIGHT_GU = REGISTRAR.item("little_light_gu",
            () -> new LittleLightGuItem(createRankedProperties(Rank.ONE, GuPath.LIGHT)
                    .refine(800)
                    .costPerUse(12)
                    .feed(ModItemTags.LITTLE_LIGHT_FEED, 1)))
            .lang("Little Light Gu", "小光蛊").register();
    //endregion

    //region Moon Path [月道] -- Moonlight Gu [月光蛊] and Moonglow Gu [月芒蛊]: one moon blade a use, a killing
    // move's main Gu with a Little Light Gu [小光蛊]; createMoonBladeGu also builds Blood Path's Blood Moon Gu
    public static final DeferredItem<Item> MOONLIGHT_GU = REGISTRAR.item("moonlight_gu",
            () -> createMoonBladeGu(Rank.ONE, GuPath.MOON, 800, 12, ModItemTags.MOONLIGHT_FEED,
                    MoonBladeEntity.Variant.MOON, MOONLIGHT_DAMAGE))
            .lang("Moonlight Gu", "月光蛊").register();
    public static final DeferredItem<Item> MOONGLOW_GU = REGISTRAR.item("moonglow_gu",
            () -> createMoonBladeGu(Rank.TWO, GuPath.MOON, 1_600, 36, ModItemTags.MOONLIGHT_FEED,
                    MoonBladeEntity.Variant.MOON, 3 * MOONLIGHT_DAMAGE))
            .lang("Moonglow Gu", "月芒蛊").register();

    private static TendedGuItem createMoonBladeGu(Rank rank, GuPath path, int refine, int costPerUse,
                                                  TagKey<Item> feed, MoonBladeEntity.Variant variant, float damage) {
        return new TendedGuItem(createRankedProperties(rank, path)
                .type(GuType.OFFENSIVE)
                .refine(refine)
                .costPerUse(costPerUse)
                .feed(feed, 1)
                .cooldown(MOON_BLADE_COOLDOWN_TICKS),
                new MoonBladeBehavior(variant, damage));
    }
    //endregion

    //region Strength Path [力道] -- two branches and the normal Gu outside them
    //region Beast Strength Phantom Branch [兽力虚影流] -- carving Gu lay a beast's strength on the body for good; the
    // buff Gu lend it for a while
    public static final DeferredItem<Item> WHITE_BOAR_GU = REGISTRAR.item("white_boar_gu",
            () -> new TendedGuItem(createBeastStrengthProperties(ModItemTags.BOAR_FEED),
                    new BeastStrengthBehavior(BeastStrength.WHITE_BOAR)))
            .lang("White Boar Gu", "白豕蛊").register();
    public static final DeferredItem<Item> BLACK_BOAR_GU = REGISTRAR.item("black_boar_gu",
            () -> new TendedGuItem(createBeastStrengthProperties(ModItemTags.BOAR_FEED),
                    new BeastStrengthBehavior(BeastStrength.BLACK_BOAR)))
            .lang("Black Boar Gu", "黑豕蛊").register();
    public static final DeferredItem<Item> BEAR_STRENGTH_GU = REGISTRAR.item("bear_strength_gu",
            () -> new TendedGuItem(createBeastStrengthProperties(ModItemTags.BEAR_FEED),
                    new BeastStrengthBehavior(BeastStrength.BEAR)))
            .lang("Bear Strength Gu", "熊力蛊").register();
    public static final DeferredItem<Item> FLOWER_BOAR_GU = REGISTRAR.item("flower_boar_gu",
            () -> new TendedGuItem(createBeastBuffProperties(ModItemTags.BOAR_FEED),
                    new TimedBuffBehavior(ModEffects.FLOWER_BOAR_GU, FlowerBoarGuEffect.DURATION_TICKS),
                    new RecastBehavior()))
            .lang("Flower Boar Gu", "花豕蛊").register();
    public static final DeferredItem<Item> BRUTE_FORCE_LONGHORN_BEETLE_GU = REGISTRAR.item(
            "brute_force_longhorn_beetle_gu",
            () -> new TendedGuItem(createBeastBuffProperties(ModItemTags.BEEF_FEED),
                    new TimedBuffBehavior(ModEffects.BRUTE_FORCE_LONGHORN_BEETLE_GU,
                            BruteForceLonghornBeetleGuEffect.DURATION_TICKS),
                    new RecastBehavior()))
            .lang("Brute Force Longhorn Beetle Gu", "蛮力天牛蛊").register();

    private static GuProperties createBeastStrengthProperties(TagKey<Item> feed) {
        return createChannelProperties(Rank.ONE, GuPath.STRENGTH)
                .type(GuType.PATH_MARK_CARVING)
                .strengthPathBranch(StrengthPathBranch.BEAST_STRENGTH_PHANTOM)
                .effect(EffectCarrier.BODY)
                .refine(800)
                .channel(1_200)
                .feed(feed, 1);
    }

    private static GuProperties createBeastBuffProperties(TagKey<Item> feed) {
        return createRankedProperties(Rank.ONE, GuPath.STRENGTH)
                .type(GuType.AMPLIFICATION)
                .strengthPathBranch(StrengthPathBranch.BEAST_STRENGTH_PHANTOM)
                .effect(EffectCarrier.BODY)
                .refine(800)
                .costPerUse(12)
                .feed(feed, 1)
                .cooldown(30 * Ticks.SECOND, Ticks.SECOND);
    }
    //endregion

    //region Human Jun Strength Branch [人力钧力流] -- one channeled round is one layer; the strands grow fourfold a rank
    public static final DeferredItem<Item> JIN_STRENGTH_GU = REGISTRAR.item("jin_strength_gu",
            () -> createHumanStrengthGu(Rank.ONE, 2, 800, 1_200, ModItemTags.IRON_FEED, HumanStrength.JIN))
            .lang("Jin Strength Gu", "斤力蛊").register();
    public static final DeferredItem<Item> TENS_JIN_STRENGTH_GU = REGISTRAR.item("tens_jin_strength_gu",
            () -> createHumanStrengthGu(Rank.TWO, 8, 1_600, 2_400, ModItemTags.IRON_FEED, HumanStrength.TEN_JIN))
            .lang("Ten Jin Strength Gu", "十斤力蛊").register();
    public static final DeferredItem<Item> JUN_STRENGTH_GU = REGISTRAR.item("jun_strength_gu",
            () -> createHumanStrengthGu(Rank.THREE, 32, 3_200, 4_800, ModItemTags.IRON_FEED_SMELTED,
                    HumanStrength.JUN))
            .lang("Jun Strength Gu", "钧力蛊").register();
    public static final DeferredItem<Item> TENS_JUN_STRENGTH_GU = REGISTRAR.item("tens_jun_strength_gu",
            () -> createHumanStrengthGu(Rank.FOUR, 128, 6_400, 9_600, ModItemTags.IRON_FEED_SMELTED,
                    HumanStrength.TEN_JUN))
            .lang("Ten Jun Strength Gu", "十钧力蛊").register();
    public static final DeferredItem<Item> HUNDREDS_JUN_STRENGTH_GU = REGISTRAR.item("hundreds_jun_strength_gu",
            () -> createHumanStrengthGu(Rank.FIVE, 512, 12_800, 19_200, ModItemTags.IRON_FEED_SMELTED,
                    HumanStrength.HUNDRED_JUN))
            .lang("Hundred Jun Strength Gu", "百钧之力蛊").register();

    private static TendedGuItem createHumanStrengthGu(Rank rank, long strands, int refine, int essencePerRound,
                                                      TagKey<Item> feed, HumanStrength kind) {
        return new TendedGuItem(createChannelProperties(rank, GuPath.STRENGTH)
                .type(GuType.PATH_MARK_CARVING)
                .strengthPathBranch(StrengthPathBranch.HUMAN_JUN_STRENGTH)
                .effect(EffectCarrier.BODY, strands)
                .refine(refine)
                .channel(essencePerRound)
                .feed(feed, 1),
                new HumanStrengthBehavior(kind));
    }
    //endregion

    //region Normal [基础力道] -- Strength Path Gu outside the two branches: the crashes [横冲直撞], Self Reliance
    // [自力更生], Hardship Strength [苦力] and All-Out Effort [全力以赴], which unlocks a stockpiled 99,999 jin
    public static final DeferredItem<Item> HORIZONTAL_CRASH_GU = REGISTRAR.item("horizontal_crash_gu",
            () -> createCrashGu(Rank.THREE, 3_200, 108,
                    new TimedBuffBehavior(ModEffects.HORIZONTAL_CRASH_GU, 30 * Ticks.SECOND)))
            .lang("Horizontal Crash Gu", "横冲蛊").register();
    public static final DeferredItem<Item> VERTICAL_CRASH_GU = REGISTRAR.item("vertical_crash_gu",
            () -> createCrashGu(Rank.THREE, 3_200, 108,
                    new TimedBuffBehavior(ModEffects.VERTICAL_CRASH_GU, 30 * Ticks.SECOND)))
            .lang("Vertical Crash Gu", "直撞蛊").register();
    public static final DeferredItem<Item> CHARGING_CRASH_GU_4 = REGISTRAR.item("charging_crash_gu_4",
            () -> createCrashGu(Rank.FOUR, 6_400, 324,
                    new TimedBuffBehavior(ModEffects.CHARGING_CRASH_GU, 30 * Ticks.SECOND, 3)))
            .lang("Charging Crash Gu IV", "四转横冲直撞蛊").register();
    public static final DeferredItem<Item> CHARGING_CRASH_GU_5 = REGISTRAR.item("charging_crash_gu_5",
            () -> createCrashGu(Rank.FIVE, 12_800, 972,
                    new TimedBuffBehavior(ModEffects.CHARGING_CRASH_GU, 60 * Ticks.SECOND, 4)))
            .lang("Charging Crash Gu V", "五转横冲直撞蛊").register();
    public static final DeferredItem<Item> SELF_RELIANCE_GU_2 = REGISTRAR.item("self_reliance_gu_2",
            () -> createSelfRelianceGu(Rank.TWO, 1_600, 36, 1, 0.03F))
            .lang("Self Reliance Gu II", "二转自力更生蛊").register();
    public static final DeferredItem<Item> SELF_RELIANCE_GU_3 = REGISTRAR.item("self_reliance_gu_3",
            () -> createSelfRelianceGu(Rank.THREE, 3_200, 108, 2, 0.04F))
            .lang("Self Reliance Gu III", "三转自力更生蛊").register();
    public static final DeferredItem<Item> SELF_RELIANCE_GU_4 = REGISTRAR.item("self_reliance_gu_4",
            () -> createSelfRelianceGu(Rank.FOUR, 6_400, 324, 3, 0.05F))
            .lang("Self Reliance Gu IV", "四转自力更生蛊").register();
    public static final DeferredItem<Item> HARDSHIP_STRENGTH_GU = REGISTRAR.item("hardship_strength_gu",
            () -> new TendedGuItem(createRankedProperties(Rank.FOUR, GuPath.STRENGTH)
                    .type(GuType.AMPLIFICATION)
                    .effect(EffectCarrier.BODY)
                    .refine(6_400)
                    .costPerUse(324)
                    .feed(ModItemTags.POTATO_FEED, 1)
                    .cooldown(30 * Ticks.SECOND, Ticks.SECOND),
                    new TimedBuffBehavior(ModEffects.HARDSHIP_STRENGTH_GU, HardshipStrengthGuEffect.DURATION_TICKS),
                    new RecastBehavior()))
            .lang("Hardship Strength Gu", "苦力蛊").register();
    public static final DeferredItem<Item> ALL_OUT_EFFORT_GU_3 = REGISTRAR.item("all_out_effort_gu_3",
            () -> createAllOutEffortGu(Rank.THREE, 3_200, 108, 80, 60))
            .lang("All-Out Effort Gu III", "三转全力以赴蛊").register();
    public static final DeferredItem<Item> ALL_OUT_EFFORT_GU_4 = REGISTRAR.item("all_out_effort_gu_4",
            () -> createAllOutEffortGu(Rank.FOUR, 6_400, 324, 100, 90))
            .lang("All-Out Effort Gu IV", "四转全力以赴蛊").register();
    public static final DeferredItem<Item> ALL_OUT_EFFORT_GU_5 = REGISTRAR.item("all_out_effort_gu_5",
            () -> createAllOutEffortGu(Rank.FIVE, 12_800, 972, 120, 120))
            .lang("All-Out Effort Gu V", "五转全力以赴蛊").register();

    private static TendedGuItem createCrashGu(Rank rank, int refine, int costPerUse, TimedBuffBehavior crash) {
        return new TendedGuItem(createRankedProperties(rank, GuPath.STRENGTH)
                .type(GuType.MOVEMENT)
                .effect(EffectCarrier.BODY)
                .refine(refine)
                .costPerUse(costPerUse)
                .feed(ModItemTags.ANVIL_FEED, 1)
                .cooldown(30 * Ticks.SECOND, Ticks.SECOND),
                crash,
                new RecastBehavior());
    }

    private static TendedGuItem createSelfRelianceGu(Rank rank, int refine, int costPerUse, int grade,
                                                     float pulseHeal) {
        return new TendedGuItem(createRankedProperties(rank, GuPath.STRENGTH)
                .type(GuType.HEALING)
                .effect(EffectCarrier.BODY)
                .refine(refine)
                .costPerUse(costPerUse)
                .feed(ModItemTags.COBBLESTONE_FEED, 1)
                .cooldown(15 * Ticks.SECOND, Ticks.SECOND),
                new SelfRelianceBehavior(grade, pulseHeal),
                new AutoUseBehavior(0.25F));
    }

    private static TendedGuItem createAllOutEffortGu(Rank rank, int refine, int costPerUse, int cooldownSeconds,
                                                     int effectSeconds) {
        return new TendedGuItem(createRankedProperties(rank, GuPath.STRENGTH)
                .type(GuType.AMPLIFICATION)
                .effect(EffectCarrier.BODY)
                .refine(refine)
                .costPerUse(costPerUse)
                .feed(ModItemTags.ALL_OUT_FEED, 1)
                .cooldown(cooldownSeconds * Ticks.SECOND),
                new AllOutEffortBehavior(effectSeconds));
    }
    //endregion
    //endregion

    //region Wisdom Path [智道] -- Casual Gu [随意蛊]: ten seconds of random thoughts; Malicious Thought Gu [恶念蛊]: a
    // one-use flood of evil thoughts; both taken by their use
    public static final DeferredItem<Item> CASUAL_GU_1 = REGISTRAR.item("casual_gu_1",
            () -> createCasualGu(Rank.ONE, 800, 8))
            .lang("Casual Gu I", "一转随意蛊").register();
    public static final DeferredItem<Item> CASUAL_GU_2 = REGISTRAR.item("casual_gu_2",
            () -> createCasualGu(Rank.TWO, 1_600, 10))
            .lang("Casual Gu II", "二转随意蛊").register();
    public static final DeferredItem<Item> MALICIOUS_THOUGHT_GU_2 = REGISTRAR.item("malicious_thought_gu_2",
            () -> createMaliciousThoughtGu(Rank.TWO, 1_600, 10, 64L))
            .lang("Malicious Thought Gu II", "二转恶念蛊").register();
    public static final DeferredItem<Item> MALICIOUS_THOUGHT_GU_3 = REGISTRAR.item("malicious_thought_gu_3",
            () -> createMaliciousThoughtGu(Rank.THREE, 3_200, 12, 640L))
            .lang("Malicious Thought Gu III", "三转恶念蛊").register();
    public static final DeferredItem<Item> MALICIOUS_THOUGHT_GU_4 = REGISTRAR.item("malicious_thought_gu_4",
            () -> createMaliciousThoughtGu(Rank.FOUR, 6_400, 14, 6_400L))
            .lang("Malicious Thought Gu IV", "四转恶念蛊").register();
    public static final DeferredItem<Item> MALICIOUS_THOUGHT_GU_5 = REGISTRAR.item("malicious_thought_gu_5",
            () -> createMaliciousThoughtGu(Rank.FIVE, 12_800, 16, 64_000L))
            .lang("Malicious Thought Gu V", "五转恶念蛊").register();

    private static ConsumedGuItem createCasualGu(Rank rank, int refine, int hungerBar) {
        return new ConsumedGuItem(GuProperties.of(rank, GuPath.WISDOM)
                .type(GuType.CULTIVATION)
                .effect(EffectCarrier.MIND)
                .refine(refine)
                .costPerUse(0)
                .hungerBar(hungerBar, 1)
                .hungerPerUse(0)
                .feed(ModItemTags.CASUAL_FEED, 1)
                .cooldown(Ticks.SECOND),
                new ThoughtBehavior(ModPathEffectTypes.CASUAL_THOUGHT, ModEffects.CASUAL_GU, 0,
                        CasualGuEffect.DURATION_TICKS));
    }

    private static ConsumedGuItem createMaliciousThoughtGu(Rank rank, int refine, int hungerBar, long thoughts) {
        return new ConsumedGuItem(GuProperties.of(rank, GuPath.WISDOM)
                .type(GuType.CULTIVATION)
                .effect(EffectCarrier.MIND)
                .refine(refine)
                .costPerUse(0)
                .hungerBar(hungerBar, 1)
                .hungerPerUse(0)
                .feed(ModItemTags.MALICIOUS_THOUGHT_FEED, 1)
                .cooldown(Ticks.SECOND),
                new ThoughtBurstBehavior(thoughts, ThoughtTag.EVIL),
                new ThoughtBehavior(ModPathEffectTypes.MALICIOUS_THOUGHT, ModEffects.MALICIOUS_THOUGHT_GU, -1,
                        MaliciousThoughtGuEffect.DURATION_TICKS));
    }
    //endregion

    //region Blood Path [血道] -- Blood Moon Gu [血月蛊]: a blood moon blade that leaves Bleeding [流血]
    public static final DeferredItem<Item> BLOOD_MOON_GU = REGISTRAR.item("blood_moon_gu",
            () -> createMoonBladeGu(Rank.THREE, GuPath.BLOOD, 3_200, 108, ModItemTags.BLOOD_MOON_FEED,
                    MoonBladeEntity.Variant.BLOOD, 9 * MOONLIGHT_DAMAGE))
            .lang("Blood Moon Gu", "血月蛊").register();
    //endregion

    //region Soul Path [魂道] -- Guts Gu [胆识蛊]: a one-shot Gu that raises the soul cap
    public static final DeferredItem<Item> GUTS_GU = REGISTRAR.item("guts_gu",
            () -> new OneShotGuItem(GuProperties.of(Rank.ONE, GuPath.SOUL)
                    .type(GuType.CULTIVATION),
                    new SoulCapBehavior(10)))
            .lang("Guts Gu", "一转胆识蛊").register();
    //endregion

    //region Food Path [食道] -- Liquor Worm [酒虫]: a hunger bar of 8 days with 3 days a use, and only its own rank
    // can drive it
    public static final DeferredItem<Item> LIQUOR_WORM = REGISTRAR.item("liquor_worm",
            () -> createLiquorWorm(Rank.ONE, 800, 12, 8, 3, 1))
            .lang("Liquor Worm", "酒虫").register();
    public static final DeferredItem<Item> FOUR_FLAVORS_LIQUOR_WORM = REGISTRAR.item("four_flavors_liquor_worm",
            () -> createLiquorWorm(Rank.TWO, 1_600, 36, 16, 6, 2))
            .lang("Four Flavors Liquor Worm", "四味酒虫").register();
    public static final DeferredItem<Item> SEVEN_FRAGRANCES_LIQUOR_WORM = REGISTRAR.item(
            "seven_fragrances_liquor_worm",
            () -> createLiquorWorm(Rank.THREE, 3_200, 108, 32, 12, 4))
            .lang("Seven Fragrances Liquor Worm", "七香酒虫").register();
    public static final DeferredItem<Item> NINE_EYES_LIQUOR_WORM = REGISTRAR.item("nine_eyes_liquor_worm",
            () -> createLiquorWorm(Rank.FOUR, 6_400, 324, 64, 24, 8))
            .lang("Nine Eyes Liquor Worm", "九眼酒虫").register();

    private static TendedGuItem createLiquorWorm(Rank rank, int refine, int costPerUse, int hungerBar,
                                                 int hungerPerUse, int hungerPerDay) {
        return new TendedGuItem(GuProperties.of(rank, GuPath.FOOD)
                .type(GuType.CULTIVATION)
                .effect(EffectCarrier.BODY)
                .refine(refine)
                .costPerUse(costPerUse)
                .hungerBar(hungerBar, 1)
                .hungerPerUse(hungerPerUse)
                .hungerPerDay(hungerPerDay)
                .feed(ModItemTags.LIQUOR_FEED, 1),
                new DistillBehavior());
    }
    //endregion

    //region Transformation Path [变化道] -- Zombie Gu [僵尸蛊]: a timed Temp Zombie [临时僵尸化], and a 5-minute
    // window that makes it permanent; the six rank V Gu share one row
    public static final DeferredItem<Item> ROAMING_ZOMBIE_GU = REGISTRAR.item("roaming_zombie_gu",
            () -> createZombieGu(Rank.TWO, 1_600, 36, 16, 6, 2, 2 * Ticks.MINUTE))
            .lang("Roaming Zombie Gu", "游僵蛊").register();
    public static final DeferredItem<Item> HAIRY_ZOMBIE_GU = REGISTRAR.item("hairy_zombie_gu",
            () -> createZombieGu(Rank.THREE, 3_200, 108, 32, 12, 4, 4 * Ticks.MINUTE))
            .lang("Hairy Zombie Gu", "毛僵蛊").register();
    public static final DeferredItem<Item> HOPPING_ZOMBIE_GU = REGISTRAR.item("hopping_zombie_gu",
            () -> createZombieGu(Rank.FOUR, 6_400, 324, 64, 24, 8, 6 * Ticks.MINUTE))
            .lang("Hopping Zombie Gu", "跳僵蛊").register();
    public static final DeferredItem<Item> HEAVENLY_DEMON_ZOMBIE_GU = REGISTRAR.item("heavenly_demon_zombie_gu",
            ModItems::createFifthRankZombieGu)
            .lang("Heavenly Demon Zombie Gu", "天魔尸蛊").register();
    public static final DeferredItem<Item> NIGHTMARE_ZOMBIE_GU = REGISTRAR.item("nightmare_zombie_gu",
            ModItems::createFifthRankZombieGu)
            .lang("Nightmare Zombie Gu", "梦魇尸蛊").register();
    public static final DeferredItem<Item> ASURA_ZOMBIE_GU = REGISTRAR.item("asura_zombie_gu",
            ModItems::createFifthRankZombieGu)
            .lang("Asura Zombie Gu", "修罗尸蛊").register();
    public static final DeferredItem<Item> EARTH_CHIEF_ZOMBIE_GU = REGISTRAR.item("earth_chief_zombie_gu",
            ModItems::createFifthRankZombieGu)
            .lang("Earth Chief Zombie Gu", "地魁尸蛊").register();
    public static final DeferredItem<Item> PLAGUE_ZOMBIE_GU = REGISTRAR.item("plague_zombie_gu",
            ModItems::createFifthRankZombieGu)
            .lang("Plague Zombie Gu", "病瘟尸蛊").register();
    public static final DeferredItem<Item> BLOOD_WIGHT_GU = REGISTRAR.item("blood_wight_gu",
            ModItems::createFifthRankZombieGu)
            .lang("Blood Wight Gu", "血鬼尸蛊").register();

    private static TendedGuItem createZombieGu(Rank rank, int refine, int costPerUse, int hungerBar, int hungerPerUse,
                                               int hungerPerDay, int tempZombieTicks) {
        return new TendedGuItem(GuProperties.of(rank, GuPath.TRANSFORMATION)
                .effect(EffectCarrier.BODY)
                .refine(refine)
                .costPerUse(costPerUse)
                .hungerBar(hungerBar, 1)
                .hungerPerUse(hungerPerUse)
                .hungerPerDay(hungerPerDay)
                .feed(ModItemTags.ZOMBIE_FEED, 1)
                .cooldown(Ticks.SECOND),
                new ZombieBehavior(tempZombieTicks));
    }

    private static TendedGuItem createFifthRankZombieGu() {
        return createZombieGu(Rank.FIVE, 12_800, 972, 128, 48, 16, 8 * Ticks.MINUTE);
    }
    //endregion

    //region Killing move books [杀招书] -- one per KillingMove row, same id; a read teaches it. Creative or command
    // only for now
    public static final DeferredItem<Item> KM_MORTAL_LITTLE_LIGHT_AMPLIFY = REGISTRAR.item(
            "km_mortal_little_light_amplify",
            () -> new KillingMoveBookItem(new Item.Properties(), KillingMove.LITTLE_LIGHT_AMPLIFY))
            .lang("Mortal Killing Move: Little Light Amplification", "凡道杀招：小光蛊增幅").register();
    //endregion

    //region Gu materials [蛊材] -- stones, liquors and the spring's water
    public static final DeferredItem<Item> PRIMEVAL_STONE = REGISTRAR.item("primeval_stone",
            () -> new PrimevalStoneItem(new Item.Properties(), PRIMEVAL_STONE_ESSENCE))
            .lang("Primeval Stone", "元石").register();
    public static final DeferredItem<Item> LIQUOR = REGISTRAR.item("liquor",
            () -> new LiquorItem(new Item.Properties()))
            .lang("Liquor", "酒").tag(ModItemTags.LIQUOR_FEED).register();
    public static final DeferredItem<Item> SOUR_LIQUOR = REGISTRAR.item("sour_liquor",
            () -> new LiquorItem(new Item.Properties()))
            .lang("Sour Liquor", "酸酒").tag(ModItemTags.LIQUOR_FEED).register();
    public static final DeferredItem<Item> SWEET_LIQUOR = REGISTRAR.item("sweet_liquor",
            () -> new LiquorItem(new Item.Properties()))
            .lang("Sweet Liquor", "甜酒").tag(ModItemTags.LIQUOR_FEED).register();
    public static final DeferredItem<Item> BITTER_LIQUOR = REGISTRAR.item("bitter_liquor",
            () -> new LiquorItem(new Item.Properties()))
            .lang("Bitter Liquor", "苦酒").tag(ModItemTags.LIQUOR_FEED).register();
    public static final DeferredItem<Item> SPICY_LIQUOR = REGISTRAR.item("spicy_liquor",
            () -> new LiquorItem(new Item.Properties()))
            .lang("Spicy Liquor", "辣酒").tag(ModItemTags.LIQUOR_FEED).register();
    public static final DeferredItem<Item> SPIRIT_SPRING = REGISTRAR.item("spirit_spring",
            () -> new BlockItem(ModBlocks.SPIRIT_SPRING.get(), new Item.Properties()))
            .lang("Spirit Spring", "元泉").model(ItemModel.FLUID).tab(ModCreativeTabs.GU_MATERIAL).register();
    public static final DeferredItem<Item> PURE_WATER_BUCKET = REGISTRAR.item("pure_water_bucket",
            () -> new BucketItem(ModFluids.PURE_WATER.get(),
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)))
            .lang("Pure Water Bucket", "纯水桶").tab(ModCreativeTabs.GU_MATERIAL).register();
    //endregion

    //region Herb materials [草木蛊材] -- pure Gu material, wood path rank I; only a hoe takes them from the plant
    public static final DeferredItem<Item> INTIMATE_GRASS_BUNDLE = REGISTRAR.item("intimate_grass_bundle",
            () -> new GuMaterialItem(new Item.Properties(), Rank.ONE, GuPath.WOOD))
            .lang("Intimate Grass Bundle", "知心草束").register();
    public static final DeferredItem<Item> MOON_ORCHID_PETALS = REGISTRAR.item("moon_orchid_petals",
            () -> new GuMaterialItem(new Item.Properties(), Rank.ONE, GuPath.WOOD))
            .lang("Moon Orchid Petals", "月兰花瓣").register();
    //endregion

    //region Earth materials [土道蛊材] -- pure Gu material, earth path rank I; no natural source yet
    // Rough Jade [玉原石] and Rough Jadeite [翡翠原石] smelt into Jade [玉] and Jadeite [翡翠], both jade skin Gu's food.
    public static final DeferredItem<Item> ROTTEN_FROZEN_MUD = REGISTRAR.item("rotten_frozen_mud",
            () -> new GuMaterialItem(new Item.Properties(), Rank.ONE, GuPath.EARTH))
            .lang("Rotten Frozen Mud", "腐泥冻土").register();
    public static final DeferredItem<Item> JADE = REGISTRAR.item("jade",
            () -> new GuMaterialItem(new Item.Properties(), Rank.ONE, GuPath.EARTH))
            .lang("Jade", "玉").tag(ModItemTags.JADE_SKIN_FEED, ModItemTags.WHITE_JADE_FEED).register();
    public static final DeferredItem<Item> ROUGH_JADE = REGISTRAR.item("rough_jade",
            () -> new GuMaterialItem(new Item.Properties(), Rank.ONE, GuPath.EARTH))
            .lang("Rough Jade", "玉原石").register();
    public static final DeferredItem<Item> JADEITE = REGISTRAR.item("jadeite",
            () -> new GuMaterialItem(new Item.Properties(), Rank.ONE, GuPath.EARTH))
            .lang("Jadeite", "翡翠").tag(ModItemTags.JADE_SKIN_FEED, ModItemTags.WHITE_JADE_FEED).register();
    public static final DeferredItem<Item> ROUGH_JADEITE = REGISTRAR.item("rough_jadeite",
            () -> new GuMaterialItem(new Item.Properties(), Rank.ONE, GuPath.EARTH))
            .lang("Rough Jadeite", "翡翠原石").register();
    //endregion

    //region Blood materials [血道蛊材] -- pure Gu material, blood path rank I; no natural source yet
    public static final DeferredItem<Item> BLOOD_BOTTLE = REGISTRAR.item("blood_bottle",
            () -> new GuMaterialItem(new Item.Properties().craftRemainder(Items.GLASS_BOTTLE).stacksTo(16),
                    Rank.ONE, GuPath.BLOOD))
            .lang("Blood Bottle", "血瓶").register();
    //endregion

    //region Metal [金属] -- plain items like iron's: the Steel Tendon Gu's ingot from a blast furnace, its block of nine
    public static final DeferredItem<Item> STEEL_INGOT = REGISTRAR.item("steel_ingot",
            () -> new Item(new Item.Properties()))
            .lang("Steel Ingot", "钢锭").tag(ModItemTags.INGOTS_STEEL).tab(ModCreativeTabs.GU_MATERIAL).register();
    public static final DeferredItem<Item> STEEL_BLOCK = REGISTRAR.item("steel_block",
            () -> new BlockItem(ModBlocks.STEEL_BLOCK.get(), new Item.Properties()))
            .lang("Steel Block", "钢块").model(ItemModel.BLOCK).tag(ModItemTags.STORAGE_BLOCKS_STEEL)
            .tab(ModCreativeTabs.GU_MATERIAL).register();
    //endregion

    //region Human Aperture [人窍] -- pure Gu material, ranks I to V; a wiped death drops one per aperture
    public static final DeferredItem<Item> HUMAN_APERTURE_1 =
            registerHumanAperture("human_aperture_1", Rank.ONE, "Human Aperture I", "一转人窍");
    public static final DeferredItem<Item> HUMAN_APERTURE_2 =
            registerHumanAperture("human_aperture_2", Rank.TWO, "Human Aperture II", "二转人窍");
    public static final DeferredItem<Item> HUMAN_APERTURE_3 =
            registerHumanAperture("human_aperture_3", Rank.THREE, "Human Aperture III", "三转人窍");
    public static final DeferredItem<Item> HUMAN_APERTURE_4 =
            registerHumanAperture("human_aperture_4", Rank.FOUR, "Human Aperture IV", "四转人窍");
    public static final DeferredItem<Item> HUMAN_APERTURE_5 =
            registerHumanAperture("human_aperture_5", Rank.FIVE, "Human Aperture V", "五转人窍");

    private static DeferredItem<Item> registerHumanAperture(String id, Rank rank, String english, String simplified) {
        return REGISTRAR.item(id, () -> new GuMaterialItem(new Item.Properties().stacksTo(64), rank, GuPath.HUMAN))
                .lang(english, simplified).register();
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

    //region Advancement Icon [进度图标] -- no tab, no recipe viewer; only advancements show it
    public static final DeferredItem<AdvancementIconItem> ADVANCEMENT_ICON = REGISTRAR.typedItem("advancement_icon",
            () -> new AdvancementIconItem(new Item.Properties().stacksTo(1)))
            .lang("Advancement Icon", "进度图标").model(ItemModel.NUMERALS).tag(Tags.Items.HIDDEN_FROM_RECIPE_VIEWERS)
            .register();
    //endregion

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
