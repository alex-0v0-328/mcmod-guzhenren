package net.alex.guzhenren.registry.item;

import com.google.common.math.IntMath;
import java.util.List;
import java.util.function.Predicate;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.effect.timed.BruteForceLonghornBeetleGuEffect;
import net.alex.guzhenren.effect.timed.CasualGuEffect;
import net.alex.guzhenren.effect.timed.FlowerBoarGuEffect;
import net.alex.guzhenren.effect.timed.MaliciousThoughtGuEffect;
import net.alex.guzhenren.effect.timed.StrengthQiGuEffect;
import net.alex.guzhenren.entity.projectile.MoonBladeEntity;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.body.Bone;
import net.alex.guzhenren.gameplay.body.ShieldType;
import net.alex.guzhenren.gameplay.body.Skin;
import net.alex.guzhenren.gameplay.body.Tendon;
import net.alex.guzhenren.gameplay.mind.ThoughtTag;
import net.alex.guzhenren.gameplay.path.EffectCarrier;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.BigStrength;
import net.alex.guzhenren.gameplay.path.strength.HumanStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthData;
import net.alex.guzhenren.gameplay.path.strength.StrengthPathBranch;
import net.alex.guzhenren.gameplay.rewind.ChunkKind;
import net.alex.guzhenren.gameplay.secretrealm.SoulsunderMountain;
import net.alex.guzhenren.item.AdvancementIconItem;
import net.alex.guzhenren.item.Booklet;
import net.alex.guzhenren.item.BookletItem;
import net.alex.guzhenren.item.Chronicle;
import net.alex.guzhenren.item.armor.SchoolUniformItem;
import net.alex.guzhenren.item.gu.GuBehavior;
import net.alex.guzhenren.item.gu.GuProperties;
import net.alex.guzhenren.item.gu.GuType;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.OneShotGuItem;
import net.alex.guzhenren.item.gu.killingmove.KillingMove;
import net.alex.guzhenren.item.gu.killingmove.KillingMoveBookItem;
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
import net.alex.guzhenren.item.gu.mortal.path.strength.BeastStrengthBehavior;
import net.alex.guzhenren.item.gu.mortal.path.strength.BigStrengthBehavior;
import net.alex.guzhenren.item.gu.mortal.path.strength.HardshipBehavior;
import net.alex.guzhenren.item.gu.mortal.path.strength.HumanStrengthBehavior;
import net.alex.guzhenren.item.gu.mortal.path.strength.SelfRelianceBehavior;
import net.alex.guzhenren.item.gu.mortal.path.strength.StoreStrengthBehavior;
import net.alex.guzhenren.item.gu.mortal.path.time.ManAsBeforeGuItem;
import net.alex.guzhenren.item.gu.mortal.path.time.TerrainAsBeforeGuItem;
import net.alex.guzhenren.item.gu.mortal.path.time.WatchBehavior;
import net.alex.guzhenren.item.gu.mortal.path.transformation.ZombieBehavior;
import net.alex.guzhenren.item.gu.mortal.path.water.ShieldBehavior;
import net.alex.guzhenren.item.gu.mortal.path.wisdom.ThoughtBehavior;
import net.alex.guzhenren.item.gu.mortal.path.wisdom.ThoughtBurstBehavior;
import net.alex.guzhenren.item.gu.mortal.path.wood.NineLeafVitalityGrassItem;
import net.alex.guzhenren.item.gu.mortal.path.wood.TreasureLotusGuItem;
import net.alex.guzhenren.item.gu.mortal.path.wood.VitalityLeafBehavior;
import net.alex.guzhenren.item.gu.tending.ConsumedGuItem;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.alex.guzhenren.item.material.GuMaterialItem;
import net.alex.guzhenren.item.material.LiquorItem;
import net.alex.guzhenren.item.material.PrimevalStoneItem;
import net.alex.guzhenren.item.material.RottenFrozenMudItem;
import net.alex.guzhenren.refinement.GuRecipeItem;
import net.alex.guzhenren.registry.block.ModBlocks;
import net.alex.guzhenren.registry.builder.ItemModel;
import net.alex.guzhenren.registry.builder.ItemRegistrar;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.alex.guzhenren.registry.effect.ModPathEffectTypes;
import net.alex.guzhenren.registry.fluid.ModFluids;
import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

/**
 * 本模组全部物品的 {@code DeferredRegister} 注册表，也是全仓所有蛊虫数值（炼化、催动消耗、饱食上限与冷却）的唯一真值来
 * 源。
 * <ul>
 *   <li>数值真值规则：各物品注册链的 {@link GuProperties} 或阶梯族辅助方法即为唯一权威数值（其他处副本若不一致以此为
 *       准，故本类有意超过 300 行）；真元消耗仅随转数 r 缩放，炼化固定为 {@value #REFINE_ESSENCE} × 8^(r-1)，连发月刃
 *       催动为 {@value #FREQUENT_USE_ESSENCE} × 8^(r-1)，其余付真元蛊为 {@value #INFREQUENT_USE_ESSENCE} × 8^(r-1)。
 *       </li>
 *   <li>饱食上限规则：{@link #createRankedProperties} 按次付费，上限为 2^(rank+1) 次 × {@value #HUNGER_PER_USE} 加上 2
 *       天缓冲（每天 {@value #HUNGER_PER_DAY}）；{@link #createChannelProperties} 用于灌注类蛊[Channeled Gu]，每轮耗力
 *       道 {@value #STRENGTH_ROUND_ESSENCE} × 10^(r-1) 或炼体 {@value #TEMPERING_ROUND_ESSENCE} × 10^(r-1)（每
 *       {@value #ROUND_ESSENCE_PER_HUNGER} × 10^(r-1) 计 1 饱食），一二转按 1 轮、三转起按 3 轮外加同等缓冲计算；月刃
 *       蛊的 {@value #MOON_BLADE_COOLDOWN_TICKS} 刻间隔为自身连发节奏而非冷却遮罩。</li>
 *   <li>注册链与命名：每件物品通过 {@link ItemRegistrar} 链依次绑定工厂、简体中文名称、模型、标签与可选创造栏（注册顺序
 *       即创造栏展示顺序）；{@link #findGuWith} 用于由流派效果反查可产生对应肉身状态的蛊。</li>
 *   <li>⚠ 蛊虫区段必须严格按 {@link GuPath} 顺序组织（一道一区、道内按分支或蛊族划分），蛊材、书册与图标收尾；物品 ID
 *       须与 Java 常量名及贴图文件名一致，蛊虫默认带转数后缀（{@code _1} 至 {@code _5}，唯寿蛊[Lifespan Gu]、希望蛊
 *       [Hope Gu]、舍利蛊[Relics Gu]不带后缀），蛊材用裸 ID 以跨转通用。</li>
 *   <li>特殊物品映射：{@link #SPIRIT_SPRING} 为元泉[Spirit Spring]的 {@code BlockItem}（与方块共用翻译键并平铺流体静止
 *       贴图）；{@link #INTIMATE_GRASS_SEEDS} 为 {@link ItemNameBlockItem}（名字归物品，作物方块另有独立键）；
 *       {@link #BLACK_SOIL_FARMLAND} 仅进创造栏，生存通过腐泥冻土（{@link RottenFrozenMudItem}）右键转化；
 *       {@link #PURE_WATER_BUCKET} 负责纯水桶交互，{@link #ADVANCEMENT_ICON}（{@link AdvancementIconItem}）为仅供修为进
 *       度展示数字的虚拟物品。</li>
 * </ul>
 */

public final class ModItems {

    private ModItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guzhenren.MOD_ID);
    private static final ItemRegistrar REGISTRAR = ItemRegistrar.of(ITEMS);
    private static final long PRIMEVAL_STONE_ESSENCE = 100L;
    private static final float MOONLIGHT_DAMAGE = 6.0F;
    private static final int MOON_BLADE_COOLDOWN_TICKS = 2;
    private static final int HUNGER_PER_USE = 3;
    private static final int HUNGER_PER_DAY = 1;
    private static final int REFINE_ESSENCE = 800;
    private static final int FREQUENT_USE_ESSENCE = 10;
    private static final int INFREQUENT_USE_ESSENCE = 40;
    private static final int ESSENCE_GROWTH_PER_RANK = 8;
    private static final int STRENGTH_ROUND_ESSENCE = 1_800;
    private static final int TEMPERING_ROUND_ESSENCE = 1_200;
    private static final int ROUND_ESSENCE_PER_HUNGER = 100;

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

    //region Shared bars [共用饱食条]
    private static GuProperties createRankedProperties(Rank rank, GuPath path) {
        return GuProperties.of(rank, path)
                .hungerBar(3 * (2 << rank.ordinal()) + 2 * HUNGER_PER_DAY, 1)
                .hungerPerUse(HUNGER_PER_USE)
                .hungerPerDay(HUNGER_PER_DAY);
    }

    private static GuProperties createChannelProperties(Rank rank, GuPath path, int rankOneRound) {
        int rounds = rank.compareTo(Rank.THREE) < 0 ? 1 : 3;
        int hungerPerRound = rankOneRound / ROUND_ESSENCE_PER_HUNGER;
        return GuProperties.of(rank, path)
                .channel(IntMath.checkedMultiply(rankOneRound, Math.toIntExact(rank.getRankBase())))
                .hungerBar(rounds * hungerPerRound + 2 * HUNGER_PER_DAY, 1)
                .hungerPerRound(hungerPerRound)
                .hungerPerDay(HUNGER_PER_DAY);
    }
    //endregion

    //region Essence costs [真元消耗]
    private static int refineEssence(Rank rank) { return grow(REFINE_ESSENCE, rank); }

    private static int frequentUseEssence(Rank rank) { return grow(FREQUENT_USE_ESSENCE, rank); }

    private static int infrequentUseEssence(Rank rank) { return grow(INFREQUENT_USE_ESSENCE, rank); }

    private static int grow(int rankOne, Rank rank) {
        int steps = rank.ordinal() - Rank.ONE.ordinal();
        return IntMath.checkedMultiply(rankOne, IntMath.checkedPow(ESSENCE_GROWTH_PER_RANK, steps));
    }
    //endregion

    //region Heaven Path [天道]
    public static final DeferredItem<Item> LIFESPAN_GU = REGISTRAR.item("lifespan_gu",
            () -> createLifespanGu(1, 9))
            .lang("寿蛊").register();
    public static final DeferredItem<Item> TENS_LIFESPAN_GU = REGISTRAR.item("tens_lifespan_gu",
            () -> createLifespanGu(10, 19))
            .lang("十年寿蛊").register();
    public static final DeferredItem<Item> HUNDREDS_LIFESPAN_GU = REGISTRAR.item("hundreds_lifespan_gu",
            () -> createLifespanGu(100, 199))
            .lang("百年寿蛊").register();
    public static final DeferredItem<Item> THOUSANDS_LIFESPAN_GU = REGISTRAR.item("thousands_lifespan_gu",
            () -> createLifespanGu(1000, 1999))
            .lang("千年寿蛊").register();
    public static final DeferredItem<Item> COPPER_RELICS_GU = REGISTRAR.item("copper_relics_gu",
            () -> createRelicsGu(Rank.ONE))
            .lang("青铜舍利蛊").register();
    public static final DeferredItem<Item> STEEL_RELICS_GU = REGISTRAR.item("steel_relics_gu",
            () -> createRelicsGu(Rank.TWO))
            .lang("赤铁舍利蛊").register();
    public static final DeferredItem<Item> SILVER_RELICS_GU = REGISTRAR.item("silver_relics_gu",
            () -> createRelicsGu(Rank.THREE))
            .lang("白银舍利蛊").register();
    public static final DeferredItem<Item> GOLD_RELICS_GU = REGISTRAR.item("gold_relics_gu",
            () -> createRelicsGu(Rank.FOUR))
            .lang("黄金舍利蛊").register();
    public static final DeferredItem<Item> CRYSTAL_RELICS_GU = REGISTRAR.item("crystal_relics_gu",
            () -> createRelicsGu(Rank.FIVE))
            .lang("紫晶舍利蛊").register();

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

    //region Rule Path [律道]
    public static final DeferredItem<Item> AMPLIFY_EFFECT_GU_2 = REGISTRAR.item("amplify_effect_gu_2",
            () -> createAmplifyEffectGu(Rank.TWO))
            .lang("二转功倍蛊").register();
    public static final DeferredItem<Item> AMPLIFY_EFFECT_GU_3 = REGISTRAR.item("amplify_effect_gu_3",
            () -> createAmplifyEffectGu(Rank.THREE))
            .lang("三转功倍蛊").register();
    public static final DeferredItem<Item> AMPLIFY_EFFECT_GU_4 = REGISTRAR.item("amplify_effect_gu_4",
            () -> createAmplifyEffectGu(Rank.FOUR))
            .lang("四转功倍蛊").register();
    public static final DeferredItem<Item> AMPLIFY_EFFECT_GU_5 = REGISTRAR.item("amplify_effect_gu_5",
            () -> createAmplifyEffectGu(Rank.FIVE))
            .lang("五转功倍蛊").register();

    private static AmplifyEffectGuItem createAmplifyEffectGu(Rank rank) {
        return new AmplifyEffectGuItem(createRankedProperties(rank, GuPath.RULE)
                .type(GuType.AMPLIFICATION)
                .effect(EffectCarrier.BODY)
                .refine(refineEssence(rank))
                .costPerUse(infrequentUseEssence(rank))
                .feed(ModItemTags.AMPLIFY_EFFECT_FEED, 1));
    }
    //endregion

    //region Space Path [空道]
    public static final DeferredItem<Item> PRIMEVAL_ELDER_GU_1 = REGISTRAR.item("primeval_elder_gu_1",
            () -> createPrimevalElderGu(Rank.ONE, 1_000L))
            .lang("一转元老蛊").register();
    public static final DeferredItem<Item> PRIMEVAL_ELDER_GU_2 = REGISTRAR.item("primeval_elder_gu_2",
            () -> createPrimevalElderGu(Rank.TWO, 10_000L))
            .lang("二转元老蛊").register();
    public static final DeferredItem<Item> PRIMEVAL_ELDER_GU_3 = REGISTRAR.item("primeval_elder_gu_3",
            () -> createPrimevalElderGu(Rank.THREE, 100_000L))
            .lang("三转元老蛊").register();
    public static final DeferredItem<Item> PRIMEVAL_ELDER_GU_4 = REGISTRAR.item("primeval_elder_gu_4",
            () -> createPrimevalElderGu(Rank.FOUR, 1_000_000L))
            .lang("四转元老蛊").register();
    public static final DeferredItem<Item> PRIMEVAL_ELDER_GU_5 = REGISTRAR.item("primeval_elder_gu_5",
            () -> createPrimevalElderGu(Rank.FIVE, 100_000_000L))
            .lang("五转元老蛊").register();

    private static PrimevalElderGuItem createPrimevalElderGu(Rank rank, long capacity) {
        return new PrimevalElderGuItem(capacity, GuProperties.of(rank, GuPath.SPACE)
                .type(GuType.STORAGE)
                .refine(refineEssence(rank))
                .costPerUse(0));
    }
    //endregion

    //region Time Path [宙道]
    public static final DeferredItem<Item> SECOND_WATCH_GU_4 = REGISTRAR.item("second_watch_gu_4",
            () -> createWatchGu(Rank.FOUR, ModEffects.SECOND_WATCH_GU))
            .lang("四转两更蛊").register();
    public static final DeferredItem<Item> THIRD_WATCH_GU_5 = REGISTRAR.item("third_watch_gu_5",
            () -> createWatchGu(Rank.FIVE, ModEffects.THIRD_WATCH_GU))
            .lang("五转三更蛊").register();
    public static final DeferredItem<Item> MAN_AS_BEFORE_GU_5 = REGISTRAR.item("man_as_before_gu_5",
            () -> new ManAsBeforeGuItem(createAsBeforeProperties().type(GuType.HEALING)))
            .lang("五转人如故").register();
    public static final DeferredItem<Item> MOUNTAIN_AS_BEFORE_GU_5 = REGISTRAR.item("mountain_as_before_gu_5",
            () -> new TerrainAsBeforeGuItem(ChunkKind.LAND, createAsBeforeProperties()))
            .lang("五转山如故").register();
    public static final DeferredItem<Item> RIVER_AS_BEFORE_GU_5 = REGISTRAR.item("river_as_before_gu_5",
            () -> new TerrainAsBeforeGuItem(ChunkKind.WATER, createAsBeforeProperties()))
            .lang("五转江如故").register();

    private static ConsumedGuItem createWatchGu(Rank rank, Holder<MobEffect> form) {
        return new ConsumedGuItem(GuProperties.of(rank, GuPath.TIME)
                .type(GuType.AMPLIFICATION)
                .effect(EffectCarrier.BODY)
                .refine(refineEssence(rank))
                .costPerUse(0)
                .cooldown(Ticks.SECOND),
                new WatchBehavior(form, 5 * Ticks.MINUTE));
    }

    private static GuProperties createAsBeforeProperties() {
        return GuProperties.of(Rank.FIVE, GuPath.TIME)
                .refine(refineEssence(Rank.FIVE))
                .costPerUse(infrequentUseEssence(Rank.FIVE))
                .recoverBar(9, Ticks.DAY / 3)
                .hungerPerUse(2)
                .cooldown(Ticks.SECOND);
    }
    //endregion

    //region Human Path [人道]
    public static final DeferredItem<Item> HOPE_GU = REGISTRAR.item("hope_gu",
            () -> new HopeGuItem(GuProperties.of(Rank.ONE, GuPath.HUMAN)
                    .type(GuType.CULTIVATION)))
            .lang("希望蛊").register();
    public static final DeferredItem<Item> SECOND_APERTURE_GU_1 = REGISTRAR.item("second_aperture_gu_1",
            () -> createSecondApertureGu(Rank.ONE))
            .lang("一转第二空窍蛊").register();
    public static final DeferredItem<Item> SECOND_APERTURE_GU_2 = REGISTRAR.item("second_aperture_gu_2",
            () -> createSecondApertureGu(Rank.TWO))
            .lang("二转第二空窍蛊").register();
    public static final DeferredItem<Item> SECOND_APERTURE_GU_3 = REGISTRAR.item("second_aperture_gu_3",
            () -> createSecondApertureGu(Rank.THREE))
            .lang("三转第二空窍蛊").register();
    public static final DeferredItem<Item> SECOND_APERTURE_GU_4 = REGISTRAR.item("second_aperture_gu_4",
            () -> createSecondApertureGu(Rank.FOUR))
            .lang("四转第二空窍蛊").register();
    public static final DeferredItem<Item> SECOND_APERTURE_GU_5 = REGISTRAR.item("second_aperture_gu_5",
            () -> createSecondApertureGu(Rank.FIVE))
            .lang("五转第二空窍蛊").register();

    private static OneShotGuItem createSecondApertureGu(Rank rank) {
        return new OneShotGuItem(GuProperties.of(rank, GuPath.HUMAN)
                .type(GuType.CULTIVATION),
                new SecondApertureBehavior());
    }
    //endregion

    //region Metal Path [金道]
    public static final DeferredItem<Item> BRONZE_SKIN_GU_1 = REGISTRAR.item("bronze_skin_gu_1",
            () -> createTemperingGu(Rank.ONE, ModItemTags.BRONZE_SKIN_FEED,
                    new SkinBehavior(Skin.BRONZE_SKIN_1, 0)))
            .lang("一转铜皮蛊").register();
    public static final DeferredItem<Item> BRONZE_SKIN_GU_2 = REGISTRAR.item("bronze_skin_gu_2",
            () -> createTemperingGu(Rank.TWO, ModItemTags.BRONZE_SKIN_FEED,
                    new SkinBehavior(Skin.BRONZE_SKIN_2, 0)))
            .lang("二转铜皮蛊").register();
    public static final DeferredItem<Item> BRONZE_SKIN_GU_3 = REGISTRAR.item("bronze_skin_gu_3",
            () -> createTemperingGu(Rank.THREE, ModItemTags.BRONZE_SKIN_FEED,
                    new SkinBehavior(Skin.BRONZE_SKIN_3, 0)))
            .lang("三转铜皮蛊").register();
    public static final DeferredItem<Item> BRONZE_SKIN_GU_4 = REGISTRAR.item("bronze_skin_gu_4",
            () -> createTemperingGu(Rank.FOUR, ModItemTags.BRONZE_SKIN_FEED,
                    new SkinBehavior(Skin.BRONZE_SKIN_4, 0)))
            .lang("四转铜皮蛊").register();
    public static final DeferredItem<Item> IRON_SKIN_GU_1 = REGISTRAR.item("iron_skin_gu_1",
            () -> createTemperingGu(Rank.ONE, ModItemTags.IRON_SKIN_FEED,
                    new SkinBehavior(Skin.IRON_SKIN_1, 0)))
            .lang("一转铁皮蛊").register();
    public static final DeferredItem<Item> IRON_SKIN_GU_2 = REGISTRAR.item("iron_skin_gu_2",
            () -> createTemperingGu(Rank.TWO, ModItemTags.IRON_SKIN_FEED,
                    new SkinBehavior(Skin.IRON_SKIN_2, 0)))
            .lang("二转铁皮蛊").register();
    public static final DeferredItem<Item> IRON_SKIN_GU_3 = REGISTRAR.item("iron_skin_gu_3",
            () -> createTemperingGu(Rank.THREE, ModItemTags.IRON_SKIN_FEED,
                    new SkinBehavior(Skin.IRON_SKIN_3, 0)))
            .lang("三转铁皮蛊").register();
    public static final DeferredItem<Item> IRON_SKIN_GU_4 = REGISTRAR.item("iron_skin_gu_4",
            () -> createTemperingGu(Rank.FOUR, ModItemTags.IRON_SKIN_FEED,
                    new SkinBehavior(Skin.IRON_SKIN_4, 0)))
            .lang("四转铁皮蛊").register();
    public static final DeferredItem<Item> STEEL_TENDON_GU_3 = REGISTRAR.item("steel_tendon_gu_3",
            () -> createTemperingGu(Rank.THREE, ModItemTags.STEEL_TENDON_FEED,
                    new TendonBehavior(Tendon.STEEL_TENDON_3)))
            .lang("三转钢筋蛊").register();
    public static final DeferredItem<Item> STEEL_TENDON_GU_4 = REGISTRAR.item("steel_tendon_gu_4",
            () -> createTemperingGu(Rank.FOUR, ModItemTags.STEEL_TENDON_FEED,
                    new TendonBehavior(Tendon.STEEL_TENDON_4)))
            .lang("四转钢筋蛊").register();
    public static final DeferredItem<Item> IRON_BONE_GU_3 = REGISTRAR.item("iron_bone_gu_3",
            () -> createTemperingGu(Rank.THREE, ModItemTags.IRON_BONE_FEED,
                    new BoneBehavior(Bone.IRON_BONE_3)))
            .lang("三转铁骨蛊").register();
    public static final DeferredItem<Item> IRON_BONE_GU_4 = REGISTRAR.item("iron_bone_gu_4",
            () -> createTemperingGu(Rank.FOUR, ModItemTags.IRON_BONE_FEED,
                    new BoneBehavior(Bone.IRON_BONE_4)))
            .lang("四转铁骨蛊").register();

    private static TendedGuItem createTemperingGu(Rank rank, TagKey<Item> feed, GuBehavior tempering) {
        return new TendedGuItem(createChannelProperties(rank, GuPath.METAL, TEMPERING_ROUND_ESSENCE)
                .type(GuType.PATH_MARK_CARVING)
                .effect(EffectCarrier.BODY)
                .refine(refineEssence(rank))
                .feed(feed, 1),
                tempering);
    }
    //endregion

    //region Wood Path [木道]
    public static final DeferredItem<Item> VITALITY_LEAF_GU_1 = REGISTRAR.item("vitality_leaf_gu_1",
            () -> new OneShotGuItem(GuProperties.of(Rank.ONE, GuPath.WOOD)
                    .type(GuType.HEALING)
                    .effect(EffectCarrier.BODY),
                    new VitalityLeafBehavior()))
            .lang("一转生机叶蛊").register();
    public static final DeferredItem<Item> HEAVENLY_ESSENCE_TREASURE_LOTUS_GU_3 = REGISTRAR.item(
            "heavenly_essence_treasure_lotus_gu_3",
            () -> createTreasureLotusGu(Rank.THREE, 1))
            .lang("三转天元宝莲").register();
    public static final DeferredItem<Item> HEAVENLY_ESSENCE_TREASURE_MONARCH_LOTUS_GU_4 = REGISTRAR.item(
            "heavenly_essence_treasure_monarch_lotus_gu_4",
            () -> createTreasureLotusGu(Rank.FOUR, 10))
            .lang("四转天元宝君莲").register();
    public static final DeferredItem<Item> HEAVENLY_ESSENCE_TREASURE_KING_LOTUS_GU_5 = REGISTRAR.item(
            "heavenly_essence_treasure_king_lotus_gu_5",
            () -> createTreasureLotusGu(Rank.FIVE, 100))
            .lang("五转天元宝王莲").register();
    public static final DeferredItem<Item> NINE_LEAF_VITALITY_GRASS_3 = REGISTRAR.item("nine_leaf_vitality_grass_3",
            () -> new NineLeafVitalityGrassItem(3 * Ticks.MINUTE, 80, GuProperties.of(Rank.THREE, GuPath.WOOD)
                    .type(GuType.HEALING)
                    .refine(refineEssence(Rank.THREE))
                    .costPerUse(0)))
            .lang("三转九叶生机草").model(ItemModel.LEAF_STAGES).register();

    private static TreasureLotusGuItem createTreasureLotusGu(Rank rank, int stonesPerSecond) {
        return new TreasureLotusGuItem(stonesPerSecond, GuProperties.of(rank, GuPath.WOOD)
                .type(GuType.CULTIVATION)
                .refine(refineEssence(rank))
                .costPerUse(0));
    }
    //endregion

    //region Water Path [水道]
    public static final DeferredItem<Item> HEAVENLY_MUGWORT_GU_3 = REGISTRAR.item("heavenly_mugwort_gu_3",
            () -> new TendedGuItem(createRankedProperties(Rank.THREE, GuPath.WATER)
                    .type(GuType.DEFENSIVE)
                    .effect(EffectCarrier.BODY)
                    .refine(refineEssence(Rank.THREE))
                    .costPerUse(infrequentUseEssence(Rank.THREE))
                    .feed(ModItemTags.SMOOTH_STONE_FEED, 1)
                    .cooldown(30 * Ticks.SECOND, Ticks.SECOND),
                    new ShieldBehavior(ShieldType.HEAVENLY_MUGWORT, 180.0F, 30 * Ticks.SECOND),
                    new RecastBehavior()))
            .lang("三转天蓬蛊").register();
    //endregion

    //region Earth Path [土道]
    public static final DeferredItem<Item> JADE_SKIN_GU_1 = REGISTRAR.item("jade_skin_gu_1",
            () -> createJadeSkinGu(Rank.ONE, ModItemTags.JADE_SKIN_FEED, Skin.JADE_SKIN, 60 * Ticks.SECOND))
            .lang("一转玉皮蛊").register();
    public static final DeferredItem<Item> WHITE_JADE_GU_2 = REGISTRAR.item("white_jade_gu_2",
            () -> createJadeSkinGu(Rank.TWO, ModItemTags.WHITE_JADE_FEED, Skin.WHITE_JADE_SKIN,
                    90 * Ticks.SECOND))
            .lang("二转白玉蛊").register();
    public static final DeferredItem<Item> STONE_APERTURE_GU_3 = REGISTRAR.item("stone_aperture_gu_3",
            () -> createStoneApertureGu(Rank.THREE))
            .lang("三转石窍蛊").register();
    public static final DeferredItem<Item> STONE_APERTURE_GU_4 = REGISTRAR.item("stone_aperture_gu_4",
            () -> createStoneApertureGu(Rank.FOUR))
            .lang("四转石窍蛊").register();
    public static final DeferredItem<Item> STONE_APERTURE_GU_5 = REGISTRAR.item("stone_aperture_gu_5",
            () -> createStoneApertureGu(Rank.FIVE))
            .lang("五转石窍蛊").register();

    private static TendedGuItem createJadeSkinGu(Rank rank, TagKey<Item> feed, Skin skin, int durationTicks) {
        return new TendedGuItem(createRankedProperties(rank, GuPath.EARTH)
                .type(GuType.DEFENSIVE)
                .effect(EffectCarrier.BODY)
                .refine(refineEssence(rank))
                .costPerUse(infrequentUseEssence(rank))
                .feed(feed, 1)
                .cooldown(30 * Ticks.SECOND, Ticks.SECOND),
                new SkinBehavior(skin, durationTicks));
    }

    private static ConsumedGuItem createStoneApertureGu(Rank rank) {
        return new ConsumedGuItem(GuProperties.of(rank, GuPath.EARTH)
                .type(GuType.AMPLIFICATION)
                .effect(EffectCarrier.APERTURE)
                .refine(refineEssence(rank))
                .costPerUse(0)
                .cooldown(Ticks.SECOND),
                new PetrifyBehavior());
    }
    //endregion

    //region Qi Path [气道]
    public static final DeferredItem<Item> STRENGTH_QI_GU_3 = REGISTRAR.item("strength_qi_gu_3",
            () -> new TendedGuItem(createRankedProperties(Rank.THREE, GuPath.QI)
                    .effect(EffectCarrier.BODY)
                    .refine(refineEssence(Rank.THREE))
                    .costPerUse(infrequentUseEssence(Rank.THREE))
                    .feed(ModItemTags.BEEF_FEED, 1)
                    .cooldown(30 * Ticks.SECOND, Ticks.SECOND),
                    new TimedBuffBehavior(ModEffects.STRENGTH_QI_GU, StrengthQiGuEffect.DURATION_TICKS),
                    new RecastBehavior()))
            .lang("三转力气蛊").register();
    //endregion

    //region Light Path [光道]
    public static final DeferredItem<Item> LITTLE_LIGHT_GU_1 = REGISTRAR.item("little_light_gu_1",
            () -> new LittleLightGuItem(createRankedProperties(Rank.ONE, GuPath.LIGHT)
                    .refine(refineEssence(Rank.ONE))
                    .costPerUse(infrequentUseEssence(Rank.ONE))
                    .feed(ModItemTags.LITTLE_LIGHT_FEED, 1)))
            .lang("一转小光蛊").register();
    //endregion

    //region Moon Path [月道]
    public static final DeferredItem<Item> MOONLIGHT_GU_1 = REGISTRAR.item("moonlight_gu_1",
            () -> createMoonBladeGu(Rank.ONE, GuPath.MOON, ModItemTags.MOONLIGHT_FEED,
                    MoonBladeEntity.Variant.MOON, MOONLIGHT_DAMAGE))
            .lang("一转月光蛊").register();
    public static final DeferredItem<Item> MOONGLOW_GU_2 = REGISTRAR.item("moonglow_gu_2",
            () -> createMoonBladeGu(Rank.TWO, GuPath.MOON, ModItemTags.MOONLIGHT_FEED,
                    MoonBladeEntity.Variant.MOON, 3 * MOONLIGHT_DAMAGE))
            .lang("二转月芒蛊").register();

    private static TendedGuItem createMoonBladeGu(Rank rank, GuPath path, TagKey<Item> feed,
                                                  MoonBladeEntity.Variant variant, float damage) {
        return new TendedGuItem(createRankedProperties(rank, path)
                .type(GuType.OFFENSIVE)
                .refine(refineEssence(rank))
                .costPerUse(frequentUseEssence(rank))
                .feed(feed, 1)
                .cooldown(MOON_BLADE_COOLDOWN_TICKS),
                new MoonBladeBehavior(variant, damage));
    }
    //endregion

    //region Strength Path [力道]
    //region Beast Strength Phantom Branch [兽力虚影流]
    public static final DeferredItem<Item> WHITE_BOAR_GU_1 = REGISTRAR.item("white_boar_gu_1",
            () -> createBeastStrengthGu(Rank.ONE, ModItemTags.BOAR_FEED, BeastStrength.WHITE_BOAR))
            .lang("一转白豕蛊").register();
    public static final DeferredItem<Item> BLACK_BOAR_GU_1 = REGISTRAR.item("black_boar_gu_1",
            () -> createBeastStrengthGu(Rank.ONE, ModItemTags.BOAR_FEED, BeastStrength.BLACK_BOAR))
            .lang("一转黑豕蛊").register();
    public static final DeferredItem<Item> BEAR_STRENGTH_GU_1 = REGISTRAR.item("bear_strength_gu_1",
            () -> createBeastStrengthGu(Rank.ONE, ModItemTags.BEAR_FEED, BeastStrength.BEAR))
            .lang("一转熊力蛊").register();
    public static final DeferredItem<Item> TIGER_STRENGTH_GU_2 = REGISTRAR.item("tiger_strength_gu_2",
            () -> createBeastStrengthGu(Rank.TWO, ModItemTags.TIGER_FEED, BeastStrength.TIGER))
            .lang("二转虎力蛊").register();
    public static final DeferredItem<Item> STONE_TURTLE_STRENGTH_GU_3 = REGISTRAR.item("stone_turtle_strength_gu_3",
            () -> createBeastStrengthGu(Rank.THREE, ModItemTags.TURTLE_FEED, BeastStrength.STONE_TURTLE))
            .lang("三转石龟负力蛊").register();
    public static final DeferredItem<Item> GALLOPING_HORSE_STRENGTH_GU_3 = REGISTRAR.item(
            "galloping_horse_strength_gu_3",
            () -> createBeastStrengthGu(Rank.THREE, ModItemTags.HORSE_FEED, BeastStrength.GALLOPING_HORSE))
            .lang("三转驰马骏力蛊").register();
    public static final DeferredItem<Item> GREEN_BULL_LABOR_GU_3 = REGISTRAR.item("green_bull_labor_gu_3",
            () -> createBeastStrengthGu(Rank.THREE, ModItemTags.OX_FEED, BeastStrength.GREEN_BULL))
            .lang("三转青牛劳力蛊").register();
    public static final DeferredItem<Item> BROWN_BEAR_INNATE_STRENGTH_GU_3 = REGISTRAR.item(
            "brown_bear_innate_strength_gu_3",
            () -> createBeastStrengthGu(Rank.THREE, ModItemTags.BEAR_FEED, BeastStrength.BROWN_BEAR))
            .lang("三转棕熊本力蛊").register();
    public static final DeferredItem<Item> FLOWER_BOAR_GU_1 = REGISTRAR.item("flower_boar_gu_1",
            () -> new TendedGuItem(createBeastBuffProperties(ModItemTags.BOAR_FEED, FlowerBoarGuEffect.POINTS),
                    new TimedBuffBehavior(ModEffects.FLOWER_BOAR_GU, FlowerBoarGuEffect.DURATION_TICKS),
                    new RecastBehavior()))
            .lang("一转花豕蛊").register();
    public static final DeferredItem<Item> BRUTE_FORCE_LONGHORN_BEETLE_GU_1 = REGISTRAR.item(
            "brute_force_longhorn_beetle_gu_1",
            () -> new TendedGuItem(createBeastBuffProperties(ModItemTags.BEEF_FEED,
                    BruteForceLonghornBeetleGuEffect.POINTS),
                    new TimedBuffBehavior(ModEffects.BRUTE_FORCE_LONGHORN_BEETLE_GU,
                            BruteForceLonghornBeetleGuEffect.DURATION_TICKS),
                    new RecastBehavior()))
            .lang("一转蛮力天牛蛊").register();

    private static TendedGuItem createBeastStrengthGu(Rank rank, TagKey<Item> feed, BeastStrength beast) {
        return new TendedGuItem(createChannelProperties(rank, GuPath.STRENGTH, STRENGTH_ROUND_ESSENCE)
                .type(GuType.PATH_MARK_CARVING)
                .strengthPathBranch(StrengthPathBranch.BEAST_STRENGTH_PHANTOM)
                .effect(EffectCarrier.BODY, PathStrengthData.getMarkPoints(beast.getPoints()))
                .refine(refineEssence(rank))
                .feed(feed, 1),
                new BeastStrengthBehavior(beast));
    }

    private static GuProperties createBeastBuffProperties(TagKey<Item> feed, int points) {
        return createRankedProperties(Rank.ONE, GuPath.STRENGTH)
                .type(GuType.AMPLIFICATION)
                .strengthPathBranch(StrengthPathBranch.BEAST_STRENGTH_PHANTOM)
                .effect(EffectCarrier.BODY, PathStrengthData.getMarkPoints(points))
                .refine(refineEssence(Rank.ONE))
                .costPerUse(infrequentUseEssence(Rank.ONE))
                .feed(feed, 1)
                .cooldown(30 * Ticks.SECOND, Ticks.SECOND);
    }
    //endregion

    //region Human Jun Strength Branch [人力钧力流]
    public static final DeferredItem<Item> JIN_STRENGTH_GU_1 = REGISTRAR.item("jin_strength_gu_1",
            () -> createHumanStrengthGu(Rank.ONE, ModItemTags.IRON_FEED, HumanStrength.JIN))
            .lang("一转斤力蛊").register();
    public static final DeferredItem<Item> TENS_JIN_STRENGTH_GU_2 = REGISTRAR.item("tens_jin_strength_gu_2",
            () -> createHumanStrengthGu(Rank.TWO, ModItemTags.IRON_FEED, HumanStrength.TEN_JIN))
            .lang("二转十斤力蛊").register();
    public static final DeferredItem<Item> JUN_STRENGTH_GU_3 = REGISTRAR.item("jun_strength_gu_3",
            () -> createHumanStrengthGu(Rank.THREE, ModItemTags.IRON_FEED_SMELTED, HumanStrength.JUN))
            .lang("三转钧力蛊").register();
    public static final DeferredItem<Item> TENS_JUN_STRENGTH_GU_4 = REGISTRAR.item("tens_jun_strength_gu_4",
            () -> createHumanStrengthGu(Rank.FOUR, ModItemTags.IRON_FEED_SMELTED, HumanStrength.TEN_JUN))
            .lang("四转十钧力蛊").register();
    public static final DeferredItem<Item> HUNDREDS_JUN_STRENGTH_GU_5 = REGISTRAR.item("hundreds_jun_strength_gu_5",
            () -> createHumanStrengthGu(Rank.FIVE, ModItemTags.IRON_FEED_SMELTED, HumanStrength.HUNDRED_JUN))
            .lang("五转百钧之力蛊").register();

    private static TendedGuItem createHumanStrengthGu(Rank rank, TagKey<Item> feed, HumanStrength kind) {
        return new TendedGuItem(createChannelProperties(rank, GuPath.STRENGTH, STRENGTH_ROUND_ESSENCE)
                .type(GuType.PATH_MARK_CARVING)
                .strengthPathBranch(StrengthPathBranch.HUMAN_JUN_STRENGTH)
                .effect(EffectCarrier.BODY, PathStrengthData.getMarkPoints(kind.getJin()))
                .refine(refineEssence(rank))
                .feed(feed, 1),
                new HumanStrengthBehavior(kind));
    }
    //endregion

    //region Normal [基础力道]
    public static final DeferredItem<Item> HORIZONTAL_CRASH_GU_3 = REGISTRAR.item("horizontal_crash_gu_3",
            () -> createCrashGu(Rank.THREE,
                    new TimedBuffBehavior(ModEffects.HORIZONTAL_CRASH_GU, 30 * Ticks.SECOND)))
            .lang("三转横冲蛊").register();
    public static final DeferredItem<Item> VERTICAL_CRASH_GU_3 = REGISTRAR.item("vertical_crash_gu_3",
            () -> createCrashGu(Rank.THREE,
                    new TimedBuffBehavior(ModEffects.VERTICAL_CRASH_GU, 30 * Ticks.SECOND)))
            .lang("三转直撞蛊").register();
    public static final DeferredItem<Item> CHARGING_CRASH_GU_4 = REGISTRAR.item("charging_crash_gu_4",
            () -> createCrashGu(Rank.FOUR,
                    new TimedBuffBehavior(ModEffects.CHARGING_CRASH_GU, 30 * Ticks.SECOND, 3)))
            .lang("四转横冲直撞蛊").register();
    public static final DeferredItem<Item> CHARGING_CRASH_GU_5 = REGISTRAR.item("charging_crash_gu_5",
            () -> createCrashGu(Rank.FIVE,
                    new TimedBuffBehavior(ModEffects.CHARGING_CRASH_GU, 60 * Ticks.SECOND, 4)))
            .lang("五转横冲直撞蛊").register();
    public static final DeferredItem<Item> SELF_RELIANCE_GU_2 = REGISTRAR.item("self_reliance_gu_2",
            () -> createSelfRelianceGu(Rank.TWO, 1, 0.03F, 15))
            .lang("二转自力更生蛊").register();
    public static final DeferredItem<Item> SELF_RELIANCE_GU_3 = REGISTRAR.item("self_reliance_gu_3",
            () -> createSelfRelianceGu(Rank.THREE, 2, 0.04F, 20))
            .lang("三转自力更生蛊").register();
    public static final DeferredItem<Item> SELF_RELIANCE_GU_4 = REGISTRAR.item("self_reliance_gu_4",
            () -> createSelfRelianceGu(Rank.FOUR, 3, 0.05F, 25))
            .lang("四转自力更生蛊").register();
    public static final DeferredItem<Item> HARDSHIP_STRENGTH_GU_4 = REGISTRAR.item("hardship_strength_gu_4",
            () -> new TendedGuItem(createRankedProperties(Rank.FOUR, GuPath.STRENGTH)
                    .type(GuType.AMPLIFICATION)
                    .effect(EffectCarrier.BODY)
                    .refine(refineEssence(Rank.FOUR))
                    .costPerUse(infrequentUseEssence(Rank.FOUR))
                    .feed(ModItemTags.POTATO_FEED, 1)
                    .cooldown(30 * Ticks.SECOND, Ticks.SECOND),
                    new HardshipBehavior()))
            .lang("四转苦力蛊").register();
    public static final DeferredItem<Item> ALL_OUT_EFFORT_GU_3 = REGISTRAR.item("all_out_effort_gu_3",
            () -> createAllOutEffortGu(Rank.THREE, 80, 60))
            .lang("三转全力以赴蛊").register();
    public static final DeferredItem<Item> ALL_OUT_EFFORT_GU_4 = REGISTRAR.item("all_out_effort_gu_4",
            () -> createAllOutEffortGu(Rank.FOUR, 100, 90))
            .lang("四转全力以赴蛊").register();
    public static final DeferredItem<Item> ALL_OUT_EFFORT_GU_5 = REGISTRAR.item("all_out_effort_gu_5",
            () -> createAllOutEffortGu(Rank.FIVE, 120, 120))
            .lang("五转全力以赴蛊").register();
    public static final DeferredItem<Item> STORE_STRENGTH_GU_3 = REGISTRAR.item("store_strength_gu_3",
            () -> createStoreStrengthGu(Rank.THREE, ModEffects.STORE_STRENGTH_GU))
            .lang("三转蓄力蛊").register();
    public static final DeferredItem<Item> HABITUAL_STRENGTH_GU_4 = REGISTRAR.item("habitual_strength_gu_4",
            () -> createStoreStrengthGu(Rank.FOUR, ModEffects.HABITUAL_STRENGTH_GU))
            .lang("四转惯力蛊").register();
    public static final DeferredItem<Item> BIG_STRENGTH_GU_2 = REGISTRAR.item("big_strength_gu_2",
            () -> new TendedGuItem(createRankedProperties(Rank.TWO, GuPath.STRENGTH)
                    .type(GuType.AMPLIFICATION)
                    .effect(EffectCarrier.BODY, PathStrengthData.getMarkPoints(BigStrength.POINTS))
                    .refine(refineEssence(Rank.TWO))
                    .costPerUse(infrequentUseEssence(Rank.TWO))
                    .feed(ModItemTags.IRON_FEED, 1)
                    .cooldown(30 * Ticks.SECOND),
                    new BigStrengthBehavior(30)))
            .lang("二转大力蛊").register();

    private static TendedGuItem createStoreStrengthGu(Rank rank, Holder<MobEffect> effect) {
        return new TendedGuItem(createRankedProperties(rank, GuPath.STRENGTH)
                .type(GuType.AMPLIFICATION)
                .effect(EffectCarrier.BODY)
                .refine(refineEssence(rank))
                .costPerUse(infrequentUseEssence(rank))
                .feed(ModItemTags.ALL_OUT_FEED, 1)
                .cooldown(60 * Ticks.SECOND),
                new StoreStrengthBehavior(effect, 60));
    }

    private static TendedGuItem createCrashGu(Rank rank, TimedBuffBehavior crash) {
        return new TendedGuItem(createRankedProperties(rank, GuPath.STRENGTH)
                .type(GuType.MOVEMENT)
                .effect(EffectCarrier.BODY)
                .refine(refineEssence(rank))
                .costPerUse(infrequentUseEssence(rank))
                .feed(ModItemTags.ANVIL_FEED, 1)
                .cooldown(30 * Ticks.SECOND, Ticks.SECOND),
                crash,
                new RecastBehavior());
    }

    private static TendedGuItem createSelfRelianceGu(Rank rank, int grade, float pulseHeal, int durationSeconds) {
        return new TendedGuItem(createRankedProperties(rank, GuPath.STRENGTH)
                .type(GuType.HEALING)
                .effect(EffectCarrier.BODY)
                .refine(refineEssence(rank))
                .costPerUse(infrequentUseEssence(rank))
                .feed(ModItemTags.COBBLESTONE_FEED, 1)
                .cooldown(15 * Ticks.SECOND, Ticks.SECOND),
                new SelfRelianceBehavior(grade, pulseHeal, durationSeconds));
    }

    private static TendedGuItem createAllOutEffortGu(Rank rank, int cooldownSeconds, int effectSeconds) {
        return new TendedGuItem(createRankedProperties(rank, GuPath.STRENGTH)
                .type(GuType.AMPLIFICATION)
                .effect(EffectCarrier.BODY)
                .refine(refineEssence(rank))
                .costPerUse(infrequentUseEssence(rank))
                .feed(ModItemTags.ALL_OUT_FEED, 1)
                .cooldown(cooldownSeconds * Ticks.SECOND),
                new AllOutEffortBehavior(effectSeconds));
    }
    //endregion
    //endregion

    //region Wisdom Path [智道]
    public static final DeferredItem<Item> CASUAL_GU_1 = REGISTRAR.item("casual_gu_1",
            () -> createCasualGu(Rank.ONE, 8))
            .lang("一转随意蛊").register();
    public static final DeferredItem<Item> CASUAL_GU_2 = REGISTRAR.item("casual_gu_2",
            () -> createCasualGu(Rank.TWO, 10))
            .lang("二转随意蛊").register();
    public static final DeferredItem<Item> MALICIOUS_THOUGHT_GU_2 = REGISTRAR.item("malicious_thought_gu_2",
            () -> createMaliciousThoughtGu(Rank.TWO, 10, 64L))
            .lang("二转恶念蛊").register();
    public static final DeferredItem<Item> MALICIOUS_THOUGHT_GU_3 = REGISTRAR.item("malicious_thought_gu_3",
            () -> createMaliciousThoughtGu(Rank.THREE, 12, 640L))
            .lang("三转恶念蛊").register();
    public static final DeferredItem<Item> MALICIOUS_THOUGHT_GU_4 = REGISTRAR.item("malicious_thought_gu_4",
            () -> createMaliciousThoughtGu(Rank.FOUR, 14, 6_400L))
            .lang("四转恶念蛊").register();
    public static final DeferredItem<Item> MALICIOUS_THOUGHT_GU_5 = REGISTRAR.item("malicious_thought_gu_5",
            () -> createMaliciousThoughtGu(Rank.FIVE, 16, 64_000L))
            .lang("五转恶念蛊").register();

    private static ConsumedGuItem createCasualGu(Rank rank, int hungerBar) {
        return new ConsumedGuItem(GuProperties.of(rank, GuPath.WISDOM)
                .type(GuType.CULTIVATION)
                .effect(EffectCarrier.MIND)
                .refine(refineEssence(rank))
                .costPerUse(0)
                .hungerBar(hungerBar, 1)
                .hungerPerUse(0)
                .feed(ModItemTags.CASUAL_FEED, 1)
                .cooldown(Ticks.SECOND),
                new ThoughtBehavior(ModPathEffectTypes.CASUAL_THOUGHT, ModEffects.CASUAL_GU, 0,
                        CasualGuEffect.DURATION_TICKS));
    }

    private static ConsumedGuItem createMaliciousThoughtGu(Rank rank, int hungerBar, long thoughts) {
        return new ConsumedGuItem(GuProperties.of(rank, GuPath.WISDOM)
                .type(GuType.CULTIVATION)
                .effect(EffectCarrier.MIND)
                .refine(refineEssence(rank))
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

    //region Blood Path [血道]
    public static final DeferredItem<Item> BLOOD_MOON_GU_3 = REGISTRAR.item("blood_moon_gu_3",
            () -> createMoonBladeGu(Rank.THREE, GuPath.BLOOD, ModItemTags.BLOOD_MOON_FEED,
                    MoonBladeEntity.Variant.BLOOD, 9 * MOONLIGHT_DAMAGE))
            .lang("三转血月蛊").register();
    //endregion

    //region Soul Path [魂道]
    public static final DeferredItem<Item> GUTS_GU_1 = REGISTRAR.item("guts_gu_1",
            () -> new OneShotGuItem(GuProperties.of(Rank.ONE, GuPath.SOUL)
                    .type(GuType.CULTIVATION),
                    new SoulCapBehavior(SoulsunderMountain.GUTS_GU_SOUL_BONUS)))
            .lang("一转胆识蛊").register();
    //endregion

    //region Food Path [食道]
    public static final DeferredItem<Item> LIQUOR_WORM_1 = REGISTRAR.item("liquor_worm_1",
            () -> createLiquorWorm(Rank.ONE, 8, 3, 1))
            .lang("一转酒虫").register();
    public static final DeferredItem<Item> FOUR_FLAVORS_LIQUOR_WORM_2 = REGISTRAR.item("four_flavors_liquor_worm_2",
            () -> createLiquorWorm(Rank.TWO, 16, 6, 2))
            .lang("二转四味酒虫").register();
    public static final DeferredItem<Item> SEVEN_FRAGRANCES_LIQUOR_WORM_3 = REGISTRAR.item(
            "seven_fragrances_liquor_worm_3",
            () -> createLiquorWorm(Rank.THREE, 32, 12, 4))
            .lang("三转七香酒虫").register();
    public static final DeferredItem<Item> NINE_EYES_LIQUOR_WORM_4 = REGISTRAR.item("nine_eyes_liquor_worm_4",
            () -> createLiquorWorm(Rank.FOUR, 64, 24, 8))
            .lang("四转九眼酒虫").register();

    private static TendedGuItem createLiquorWorm(Rank rank, int hungerBar, int hungerPerUse, int hungerPerDay) {
        return new TendedGuItem(GuProperties.of(rank, GuPath.FOOD)
                .type(GuType.CULTIVATION)
                .effect(EffectCarrier.BODY)
                .refine(refineEssence(rank))
                .costPerUse(infrequentUseEssence(rank))
                .hungerBar(hungerBar, 1)
                .hungerPerUse(hungerPerUse)
                .hungerPerDay(hungerPerDay)
                .feed(ModItemTags.LIQUOR_FEED, 1),
                new DistillBehavior());
    }
    //endregion

    //region Transformation Path [变化道]
    public static final DeferredItem<Item> ROAMING_ZOMBIE_GU_2 = REGISTRAR.item("roaming_zombie_gu_2",
            () -> createZombieGu(Rank.TWO, 16, 6, 2, 2 * Ticks.MINUTE))
            .lang("二转游僵蛊").register();
    public static final DeferredItem<Item> HAIRY_ZOMBIE_GU_3 = REGISTRAR.item("hairy_zombie_gu_3",
            () -> createZombieGu(Rank.THREE, 32, 12, 4, 4 * Ticks.MINUTE))
            .lang("三转毛僵蛊").register();
    public static final DeferredItem<Item> HOPPING_ZOMBIE_GU_4 = REGISTRAR.item("hopping_zombie_gu_4",
            () -> createZombieGu(Rank.FOUR, 64, 24, 8, 6 * Ticks.MINUTE))
            .lang("四转跳僵蛊").register();
    public static final DeferredItem<Item> HEAVENLY_DEMON_ZOMBIE_GU_5 = REGISTRAR.item("heavenly_demon_zombie_gu_5",
            ModItems::createFifthRankZombieGu)
            .lang("五转天魔尸蛊").register();
    public static final DeferredItem<Item> NIGHTMARE_ZOMBIE_GU_5 = REGISTRAR.item("nightmare_zombie_gu_5",
            ModItems::createFifthRankZombieGu)
            .lang("五转梦魇尸蛊").register();
    public static final DeferredItem<Item> ASURA_ZOMBIE_GU_5 = REGISTRAR.item("asura_zombie_gu_5",
            ModItems::createFifthRankZombieGu)
            .lang("五转修罗尸蛊").register();
    public static final DeferredItem<Item> EARTH_CHIEF_ZOMBIE_GU_5 = REGISTRAR.item("earth_chief_zombie_gu_5",
            ModItems::createFifthRankZombieGu)
            .lang("五转地魁尸蛊").register();
    public static final DeferredItem<Item> PLAGUE_ZOMBIE_GU_5 = REGISTRAR.item("plague_zombie_gu_5",
            ModItems::createFifthRankZombieGu)
            .lang("五转病瘟尸蛊").register();
    public static final DeferredItem<Item> BLOOD_WIGHT_GU_5 = REGISTRAR.item("blood_wight_gu_5",
            ModItems::createFifthRankZombieGu)
            .lang("五转血鬼尸蛊").register();

    private static TendedGuItem createZombieGu(Rank rank, int hungerBar, int hungerPerUse, int hungerPerDay,
                                               int tempZombieTicks) {
        return new TendedGuItem(GuProperties.of(rank, GuPath.TRANSFORMATION)
                .effect(EffectCarrier.BODY)
                .refine(refineEssence(rank))
                .costPerUse(infrequentUseEssence(rank))
                .hungerBar(hungerBar, 1)
                .hungerPerUse(hungerPerUse)
                .hungerPerDay(hungerPerDay)
                .feed(ModItemTags.ZOMBIE_FEED, 1)
                .cooldown(Ticks.SECOND),
                new ZombieBehavior(tempZombieTicks));
    }

    private static TendedGuItem createFifthRankZombieGu() {
        return createZombieGu(Rank.FIVE, 128, 48, 16, 8 * Ticks.MINUTE);
    }
    //endregion

    //region Killing move books [杀招书]
    public static final DeferredItem<Item> KM_MORTAL_LITTLE_LIGHT_AMPLIFY = REGISTRAR.item(
            "km_mortal_little_light_amplify",
            () -> new KillingMoveBookItem(new Item.Properties(), KillingMove.LITTLE_LIGHT_AMPLIFY))
            .lang("凡道杀招：小光蛊增幅").register();
    //endregion

    //region Gu materials [蛊材]
    public static final DeferredItem<Item> PRIMEVAL_STONE = REGISTRAR.item("primeval_stone",
            () -> new PrimevalStoneItem(new Item.Properties(), PRIMEVAL_STONE_ESSENCE))
            .lang("元石").register();
    public static final DeferredItem<Item> LIQUOR = REGISTRAR.item("liquor",
            () -> new LiquorItem(new Item.Properties()))
            .lang("酒").tag(ModItemTags.LIQUOR_FEED).register();
    public static final DeferredItem<Item> SOUR_LIQUOR = REGISTRAR.item("sour_liquor",
            () -> new LiquorItem(new Item.Properties()))
            .lang("酸酒").tag(ModItemTags.LIQUOR_FEED).register();
    public static final DeferredItem<Item> SWEET_LIQUOR = REGISTRAR.item("sweet_liquor",
            () -> new LiquorItem(new Item.Properties()))
            .lang("甜酒").tag(ModItemTags.LIQUOR_FEED).register();
    public static final DeferredItem<Item> BITTER_LIQUOR = REGISTRAR.item("bitter_liquor",
            () -> new LiquorItem(new Item.Properties()))
            .lang("苦酒").tag(ModItemTags.LIQUOR_FEED).register();
    public static final DeferredItem<Item> SPICY_LIQUOR = REGISTRAR.item("spicy_liquor",
            () -> new LiquorItem(new Item.Properties()))
            .lang("辣酒").tag(ModItemTags.LIQUOR_FEED).register();
    public static final DeferredItem<Item> SPIRIT_SPRING = REGISTRAR.item("spirit_spring",
            () -> new BlockItem(ModBlocks.SPIRIT_SPRING.get(), new Item.Properties()))
            .lang("元泉").model(ItemModel.FLUID).tab(ModCreativeTabs.GU_MATERIAL).register();
    public static final DeferredItem<Item> PURE_WATER_BUCKET = REGISTRAR.item("pure_water_bucket",
            () -> new BucketItem(ModFluids.PURE_WATER.get(),
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)))
            .lang("纯水桶").tab(ModCreativeTabs.GU_MATERIAL).register();
    //endregion

    //region Herb materials [草木蛊材]
    public static final DeferredItem<Item> INTIMATE_GRASS_BUNDLE = REGISTRAR.item("intimate_grass_bundle",
            () -> new GuMaterialItem(new Item.Properties(), Rank.ONE, GuPath.WOOD))
            .lang("知心草束").register();
    public static final DeferredItem<Item> INTIMATE_GRASS_SEEDS = REGISTRAR.item("intimate_grass_seeds",
            () -> new ItemNameBlockItem(ModBlocks.INTIMATE_GRASS_CROP.get(), new Item.Properties()))
            .lang("知心草种子").tag(Tags.Items.SEEDS).tab(ModCreativeTabs.GU_MATERIAL)
            .register();
    public static final DeferredItem<Item> MOON_ORCHID_PETALS = REGISTRAR.item("moon_orchid_petals",
            () -> new GuMaterialItem(new Item.Properties(), Rank.ONE, GuPath.WOOD))
            .lang("月兰花瓣").register();
    //endregion

    //region Earth materials [土道蛊材]
    public static final DeferredItem<Item> ROTTEN_FROZEN_MUD = REGISTRAR.item("rotten_frozen_mud",
            () -> new RottenFrozenMudItem(new Item.Properties()))
            .lang("腐泥冻土").register();
    public static final DeferredItem<Item> JADE = REGISTRAR.item("jade",
            () -> new GuMaterialItem(new Item.Properties(), Rank.ONE, GuPath.EARTH))
            .lang("玉").tag(ModItemTags.JADE_SKIN_FEED, ModItemTags.WHITE_JADE_FEED).register();
    public static final DeferredItem<Item> ROUGH_JADE = REGISTRAR.item("rough_jade",
            () -> new GuMaterialItem(new Item.Properties(), Rank.ONE, GuPath.EARTH))
            .lang("玉原石").register();
    public static final DeferredItem<Item> JADEITE = REGISTRAR.item("jadeite",
            () -> new GuMaterialItem(new Item.Properties(), Rank.ONE, GuPath.EARTH))
            .lang("翡翠").tag(ModItemTags.JADE_SKIN_FEED, ModItemTags.WHITE_JADE_FEED).register();
    public static final DeferredItem<Item> ROUGH_JADEITE = REGISTRAR.item("rough_jadeite",
            () -> new GuMaterialItem(new Item.Properties(), Rank.ONE, GuPath.EARTH))
            .lang("翡翠原石").register();
    //endregion

    //region Blood materials [血道蛊材]
    public static final DeferredItem<Item> BLOOD_BOTTLE = REGISTRAR.item("blood_bottle",
            () -> new GuMaterialItem(new Item.Properties().craftRemainder(Items.GLASS_BOTTLE).stacksTo(16),
                    Rank.ONE, GuPath.BLOOD))
            .lang("血瓶").register();
    //endregion

    //region Metal [金属]
    public static final DeferredItem<Item> STEEL_INGOT = REGISTRAR.item("steel_ingot",
            () -> new Item(new Item.Properties()))
            .lang("钢锭").tag(ModItemTags.INGOTS_STEEL).tab(ModCreativeTabs.GU_MATERIAL).register();
    public static final DeferredItem<Item> STEEL_BLOCK = REGISTRAR.item("steel_block",
            () -> new BlockItem(ModBlocks.STEEL_BLOCK.get(), new Item.Properties()))
            .lang("钢块").model(ItemModel.BLOCK).tag(ModItemTags.STORAGE_BLOCKS_STEEL)
            .tab(ModCreativeTabs.GU_MATERIAL).register();
    //endregion

    //region Farmland [耕地]
    public static final DeferredItem<Item> BLACK_SOIL_FARMLAND = REGISTRAR.item("black_soil_farmland",
            () -> new BlockItem(ModBlocks.BLACK_SOIL_FARMLAND.get(), new Item.Properties()))
            .lang("黑土耕地").model(ItemModel.BLOCK).tab(ModCreativeTabs.GU_MATERIAL)
            .register();
    //endregion

    //region Human Aperture [人窍]
    public static final DeferredItem<Item> HUMAN_APERTURE_1 =
            registerHumanAperture("human_aperture_1", Rank.ONE, "一转人窍");
    public static final DeferredItem<Item> HUMAN_APERTURE_2 =
            registerHumanAperture("human_aperture_2", Rank.TWO, "二转人窍");
    public static final DeferredItem<Item> HUMAN_APERTURE_3 =
            registerHumanAperture("human_aperture_3", Rank.THREE, "三转人窍");
    public static final DeferredItem<Item> HUMAN_APERTURE_4 =
            registerHumanAperture("human_aperture_4", Rank.FOUR, "四转人窍");
    public static final DeferredItem<Item> HUMAN_APERTURE_5 =
            registerHumanAperture("human_aperture_5", Rank.FIVE, "五转人窍");

    private static DeferredItem<Item> registerHumanAperture(String id, Rank rank, String simplified) {
        return REGISTRAR.item(id, () -> new GuMaterialItem(new Item.Properties().stacksTo(64), rank, GuPath.HUMAN))
                .lang(simplified).register();
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

    //region Booklets [书册]
    public static final DeferredItem<Item> GU_ATLAS_HOPE = REGISTRAR.item("gu_atlas_hope",
            () -> new BookletItem(new Item.Properties(), Booklet.GU_ATLAS_HOPE))
            .lang("蛊虫图说：希望").register();
    public static final List<DeferredItem<Item>> RENZU_ZHUAN = registerChronicle(Chronicle.Book.RENZU_ZHUAN);
    public static final DeferredItem<Item> GU_RECIPE = REGISTRAR.item("gu_recipe",
            () -> new GuRecipeItem(new Item.Properties()))
            .lang("蛊方").register();

    private static List<DeferredItem<Item>> registerChronicle(Chronicle.Book book) {
        return Chronicle.sections(book).stream().map(section -> REGISTRAR.item(book.id() + "_" + section.section(),
                        () -> new BookletItem(new Item.Properties(), section))
                .lang(section.itemName(false))
                .model(ItemModel.SERIES).register()).toList();
    }
    //endregion

    //region Advancement Icon [进度图标]
    public static final DeferredItem<AdvancementIconItem> ADVANCEMENT_ICON = REGISTRAR.typedItem("advancement_icon",
            () -> new AdvancementIconItem(new Item.Properties().stacksTo(1)))
            .lang("进度图标").model(ItemModel.NUMERALS).tag(Tags.Items.HIDDEN_FROM_RECIPE_VIEWERS)
            .register();
    //endregion

    //region Clothing [服饰]
    public static final DeferredItem<SchoolUniformItem> SCHOOL_UNIFORM_CHESTPLATE = registerSchoolUniform(
            "school_uniform_chestplate", ArmorItem.Type.CHESTPLATE, "学堂制服上衣", true);
    public static final DeferredItem<SchoolUniformItem> SCHOOL_UNIFORM_LEGGINGS = registerSchoolUniform(
            "school_uniform_leggings", ArmorItem.Type.LEGGINGS, "学堂制服下装", true);
    public static final DeferredItem<SchoolUniformItem> SCHOOL_UNIFORM_BOOTS = registerSchoolUniform(
            "school_uniform_boots", ArmorItem.Type.BOOTS, "学堂布鞋", false);

    private static DeferredItem<SchoolUniformItem> registerSchoolUniform(
            String id, ArmorItem.Type type, String simplified, boolean dyeable) {
        var builder = REGISTRAR.typedItem(id, () -> new SchoolUniformItem(
                        ModArmorMaterials.SCHOOL_UNIFORM, type,
                        new Item.Properties().durability(
                                type.getDurability(ModArmorMaterials.SCHOOL_UNIFORM_DURABILITY_MULTIPLIER)),
                        dyeable))
                .lang(simplified)
                .tab(ModCreativeTabs.GU_MATERIAL);
        if (dyeable) builder.model(ItemModel.LAYERED);
        return builder.register();
    }
    //endregion

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
