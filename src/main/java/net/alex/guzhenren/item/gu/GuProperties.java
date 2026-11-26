package net.alex.guzhenren.item.gu;

import com.google.common.math.LongMath;
import java.util.Map;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.path.PathMarks;
import net.alex.guzhenren.gameplay.path.EffectCarrier;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.path.PathEffect;
import net.alex.guzhenren.gameplay.path.PathEffectType;
import net.alex.guzhenren.gameplay.path.strength.StrengthPathBranch;
import net.alex.guzhenren.item.gu.tending.GuHunger;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 单只凡蛊的静态数值规格：在注册时通过流式构建链声明，作为该蛊数值身份的唯一权威来源。
 * <ul>
 *   <li>集中声明炼化消耗、灌注[Channel]每轮或单次催动的真元消耗、饱食[Hunger]条规格（或随世界时间自动恢复、无需喂食的
 *       {@link #recoverBar}）、补食标签与折算单位、效果冷却与物品冷却、用途分类（{@link GuType}，可空）以及流派效果（载
 *       体与本流派道痕[Path Marks]）。</li>
 *   <li>实例保存在 {@link MortalGuItem} 中，供 {@link TendedGuItem}、物品提示框与 HUD 读取，并用于构建 {@link GuHunger}；
 *       行为专属参数（如持续时长、效果等级）留在 {@link GuBehavior} 中；蛊虫伤势只有三态、不设生命数值，不经构建链配
 *       置。</li>
 *   <li>默认每次催动消耗 1 点饱食；显式传入 {@code hungerPerUse(0)} 即为 0 点而不会被兜底改成 1，
 *       恶念蛊[Malicious Thought Gu]与随意蛊[Casual Gu]的保质期计时条依赖此特性。</li>
 *   <li>{@link #effect(EffectCarrier)} 赋予按转数计算的默认道痕点数（{@link #getDefaultPoints}：一转 12 点，每升一转乘
 *       4），{@link #effect(EffectCarrier, long)} 指定点数，{@link #effect(EffectCarrier, PathMarks)} 直接指定完整道
 *       痕；未调用者表示不施加持续流派效果、不携带道痕（如瞬发蛊或纯数值修改蛊）；{@link #createEffect} 用于构建交由
 *       {@code PathEffectService.apply} 施加的流派效果实例。</li>
 *   <li>⚠ {@link #validate} 在物品注册阶段执行启动校验，是防止配置错误的底线护栏（绝不可绕过）：灌注真元不能被每轮饱食
 *       整除、单份折算超过 1 点饱食的食物（{@code GuMeal} 只整份消耗且绝不超额进食，小于单份食物的缺口将永远无法填补）
 *       不能整除饱食上限/单次消耗/每日消耗、声明了效果载体却无道痕、或凡蛊效果携带了整条道痕（凡蛊只能携带道痕
 *       点）时，均会直接点名该蛊并抛出异常阻止启动。</li>
 * </ul>
 */

public final class GuProperties {

    private final Rank rank;
    private final GuPath path;
    private StrengthPathBranch strengthPathBranch = StrengthPathBranch.NORMAL;
    private @Nullable GuType type;
    private int refineCost;
    private int essencePerRound;
    private boolean channels;
    private int maxHunger;
    private int recoverTicks;
    private int unitsPerHunger = 1;
    private int hungerPerRound;
    private int hungerPerUse = 1;
    private int hungerPerDay = 1;
    private @Nullable TagKey<Item> feedTag;
    private int feedUnits;
    private int effectCooldownTicks;
    private int itemCooldownTicks;
    private static final long DEFAULT_POINTS = 12L;
    private static final long POINTS_PER_RANK = 4L;
    private @Nullable EffectCarrier effectCarrier;
    private PathMarks effectMarks = PathMarks.NONE;

    private GuProperties(Rank rank, GuPath path) {
        this.rank = rank;
        this.path = path;
    }

    public static GuProperties of(Rank rank, GuPath path) { return new GuProperties(rank, path); }

    //region the chain [链]
    public GuProperties refine(int cost) {
        this.refineCost = cost;
        return this;
    }

    public GuProperties channel(int essencePerRound) {
        this.essencePerRound = essencePerRound;
        this.channels = true;
        return this;
    }

    public GuProperties costPerUse(int essence) {
        this.essencePerRound = essence;
        this.channels = false;
        return this;
    }

    public GuProperties hungerBar(int max, int unitsPerHunger) {
        this.maxHunger = max;
        this.unitsPerHunger = unitsPerHunger;
        return this;
    }

    public GuProperties recoverBar(int max, int ticksPerPoint) {
        this.maxHunger = max;
        this.recoverTicks = ticksPerPoint;
        return this;
    }

    public GuProperties hungerPerRound(int points) {
        this.hungerPerRound = points;
        return this;
    }

    public GuProperties hungerPerUse(int points) {
        this.hungerPerUse = points;
        return this;
    }

    public GuProperties hungerPerDay(int points) {
        this.hungerPerDay = points;
        return this;
    }

    public GuProperties feed(TagKey<Item> tag, int units) {
        this.feedTag = tag;
        this.feedUnits = units;
        return this;
    }

    public GuProperties cooldown(int ticks) {
        this.effectCooldownTicks = ticks;
        this.itemCooldownTicks = ticks;
        return this;
    }

    public GuProperties cooldown(int effectTicks, int itemTicks) {
        this.effectCooldownTicks = effectTicks;
        this.itemCooldownTicks = itemTicks;
        return this;
    }

    public GuProperties strengthPathBranch(StrengthPathBranch branch) {
        this.strengthPathBranch = branch;
        return this;
    }

    public GuProperties type(GuType type) {
        this.type = type;
        return this;
    }

    public GuProperties effect(EffectCarrier carrier) { return effect(carrier, getDefaultPoints(rank)); }

    public GuProperties effect(EffectCarrier carrier, long points) { return effect(carrier, PathMarks.ofPoints(points)); }

    public GuProperties effect(EffectCarrier carrier, PathMarks marks) {
        this.effectCarrier = carrier;
        this.effectMarks = marks;
        return this;
    }
    //endregion

    //region what the base classes read [基类读取]
    public Rank getRank() { return rank; }

    public GuPath getPath() { return path; }

    public StrengthPathBranch getStrengthPathBranch() { return strengthPathBranch; }

    public @Nullable GuType getType() { return type; }

    public int getRefineCost() { return refineCost; }

    public int getEssencePerRound() { return essencePerRound; }

    public boolean isChanneled() { return channels; }

    public int getEffectCooldownTicks() { return effectCooldownTicks; }

    public int getItemCooldownTicks() { return itemCooldownTicks; }

    public int getMaxHunger() { return maxHunger; }

    public int getUnitsPerHunger() { return unitsPerHunger; }

    public int getHungerPerUse() { return hungerPerUse; }

    public int getHungerPerRound() { return hungerPerRound; }

    public int getFeedUnits() { return feedUnits; }

    public @Nullable EffectCarrier getEffectCarrier() { return effectCarrier; }

    public PathMarks getEffectMarks() { return effectMarks; }

    public Map<GuPath, PathMarks> getEffectPaths() {
        return effectCarrier == null ? Map.of() : Map.of(path, effectMarks);
    }

    public static long getDefaultPoints(Rank rank) {
        int steps = Math.max(0, rank.ordinal() - Rank.LOWEST.ordinal());
        return LongMath.saturatedMultiply(DEFAULT_POINTS, LongMath.saturatedPow(POINTS_PER_RANK, steps));
    }

    public <P> PathEffect<P> createEffect(PathEffectType<P> effectType, ResourceLocation source, long endTick,
                                          P payload) {
        if (effectCarrier == null) throw new IllegalStateException("Gu properties declare no effect for " + source);
        return PathEffect.of(effectType, effectCarrier, source, getEffectPaths(), endTick, payload);
    }

    public GuHunger buildHunger() {
        if (recoverTicks > 0) return new GuHunger.RecoverBar(maxHunger, recoverTicks, hungerPerUse);
        if (maxHunger > 0) {
            return new GuHunger.HungerBar(maxHunger, unitsPerHunger, essencePerRound, hungerPerRound, hungerPerUse,
                    hungerPerDay);
        }
        return new GuHunger.NoClock();
    }

    public int feedUnits(ItemStack food) {
        return feedTag != null && food.is(feedTag) ? feedUnits : 0;
    }
    //endregion

    //region the startup check [启动校验]
    public void validate(String id) {
        if (refineCost < 0) throw fault(id, "refine cost is negative");
        if (essencePerRound < 0) throw fault(id, "essence per round is negative");
        if (hungerPerDay <= 0) throw fault(id, "drains no hunger a day");
        if (recoverTicks > 0 && maxHunger <= 0) throw fault(id, "recovers with time but has no bar");
        if (recoverTicks > 0 && (channels || feedTag != null)) {
            throw fault(id, "recovers with time but channels or feeds");
        }
        if (feedTag != null && feedUnits > unitsPerHunger) checkWholeMeals(id);
        if (effectCarrier != null) validateEffect(id);

        if (!channels) return;

        if (essencePerRound == 0) throw fault(id, "channels but pours nothing");
        if (hungerPerRound <= 0) throw fault(id, "channels but costs no hunger");
        if (essencePerRound % hungerPerRound != 0) {
            throw fault(id, "round %d does not divide by %d hunger points"
                    .formatted(essencePerRound, hungerPerRound));
        }
    }

    private void validateEffect(String id) {
        if (effectMarks.isEmpty()) throw fault(id, "lays down an effect with no marks");
        if (rank.ordinal() <= Rank.HIGHEST.ordinal() && effectMarks.marks() > 0L) {
            throw fault(id, "is mortal but declares %d whole marks; a mortal Gu carries points only"
                    .formatted(effectMarks.marks()));
        }
    }

    private void checkWholeMeals(String id) {
        if (feedUnits % unitsPerHunger != 0) {
            throw fault(id, "food of %d units is not whole points at rate %d".formatted(feedUnits, unitsPerHunger));
        }
        int food = feedUnits / unitsPerHunger;
        int[] sizes = {maxHunger, hungerPerUse, hungerPerDay};
        String[] names = {"bar", "use", "day"};
        for (int i = 0; i < sizes.length; i++) {
            if (sizes[i] % food != 0) {
                throw fault(id, "%s %d does not divide by a food of %d".formatted(names[i], sizes[i], food));
            }
        }
    }

    private static IllegalStateException fault(String id, String what) {
        return new IllegalStateException("Gu properties for '%s' %s".formatted(id, what));
    }
    //endregion
}
