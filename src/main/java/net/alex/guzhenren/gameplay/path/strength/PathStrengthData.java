package net.alex.guzhenren.gameplay.path.strength;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * 玩家永久力道读数快照：汇总肉体上永久持有的兽力[Beast Strength]集合与各档人力层数。
 * <ul>
 *   <li>本记录不单独落盘，由 {@link PathStrengthService#get} 从玩家身上的永久兽力与人力流派效果实时构建（流派效果是唯一
 *       数据源，无道痕[Path Marks]即无力量）；仅包含永久力量（{@link #getStrengthPoints}），限时增益以
 *       {@link StrengthAmplifier} 形式在 {@link StrengthService} 中合并。</li>
 *   <li>力量点[Strength Points]为兽力与人力的统一计量单位：1 斤人力 = 1 点，兽力按枚举声明点数计；1 点力量恒等于
 *       {@link #MARK_POINTS_PER_POINT}（2）点力道道痕，所有力道蛊统一经 {@link #getMarkPoints} 换算道痕点，结算时再经
 *       {@link #getPoints(long)} 从效果道痕点反推力量点。</li>
 *   <li>{@link #with} 在加入新兽力前先剔除被 {@link BeastStrength#replaces} 判定替换的同兽种旧兽力（成对的白豕与黑豕除
 *       外）；{@link #getCovering} 返回已持有且点数不低于目标的同兽种兽力。</li>
 *   <li>⚠ 紧凑构造器复制枚举集合时必须使用 {@code EnumSet.noneOf} + {@code addAll} 与 {@code new EnumMap<>(Class)}：直
 *       接调 {@code EnumSet.copyOf} 或 {@code new EnumMap<>(Map)} 遇到 {@link #DEFAULT} 的空集合会抛异常。</li>
 *   <li>⚠ 五档人力的最大层数（{@code 9 / 9 / 30 / 30 / 30}）总和恰好为 {@code 99,999} 斤，切勿改动单档上限；
 *       {@link #getBeastReadings} 按 {@link BeastStrengthFamily} 归并计数（白豕与黑豕合占猪类计数）。</li>
 * </ul>
 */

public record PathStrengthData(Set<BeastStrength> beasts, Map<HumanStrength, Integer> humanStrength) {

    public static final PathStrengthData DEFAULT = new PathStrengthData(Set.of(), Map.of());
    public static final double ATTACK_PER_POINT = 0.125D;
    public static final long MARK_POINTS_PER_POINT = 2L;

    public PathStrengthData {
        EnumSet<BeastStrength> takenBeasts = EnumSet.noneOf(BeastStrength.class);
        takenBeasts.addAll(beasts);
        beasts = Collections.unmodifiableSet(takenBeasts);

        Map<HumanStrength, Integer> pruned = new EnumMap<>(HumanStrength.class);
        humanStrength.forEach((kind, count) -> {
            if (count != null && count > 0) pruned.put(kind, Math.min(count, kind.getMaxLayers()));
        });
        humanStrength = Collections.unmodifiableMap(pruned);
    }

    public boolean has(BeastStrength beast) { return beasts.contains(beast); }

    public int getHumanStrengthCount(HumanStrength kind) { return humanStrength.getOrDefault(kind, 0); }

    public boolean isEmpty() { return beasts.isEmpty() && humanStrength.isEmpty(); }

    public PathStrengthData with(BeastStrength beast) {
        EnumSet<BeastStrength> next = EnumSet.of(beast);
        next.addAll(withoutReplacedBy(beast).beasts());
        return new PathStrengthData(next, humanStrength);
    }

    public PathStrengthData withoutReplacedBy(BeastStrength beast) {
        EnumSet<BeastStrength> kept = EnumSet.noneOf(BeastStrength.class);
        for (BeastStrength held : beasts) if (!beast.replaces(held)) kept.add(held);
        return new PathStrengthData(kept, humanStrength);
    }

    public @Nullable BeastStrength getCovering(BeastStrength beast) {
        for (BeastStrength held : beasts) if (beast.isCoveredBy(held)) return held;
        return null;
    }

    public Map<BeastStrengthFamily, Integer> getBeastReadings() {
        Map<BeastStrengthFamily, Integer> readings = new EnumMap<>(BeastStrengthFamily.class);
        for (BeastStrength beast : beasts) readings.merge(beast.getFamily(), 1, Integer::sum);
        return readings;
    }

    public @Nullable BeastStrength getInnate(BeastStrengthFamily family) {
        for (BeastStrength beast : beasts) {
            if (beast.getFamily() == family && beast.getForm() == BeastStrength.Form.INNATE) return beast;
        }
        return null;
    }

    public int getTotalJin() {
        int total = 0;
        for (HumanStrength kind : HumanStrength.values()) total += getHumanStrengthCount(kind) * kind.getJin();
        return total;
    }

    public int getBeastPoints() {
        int total = 0;
        for (BeastStrength beast : beasts) total += beast.getPoints();
        return total;
    }

    public int getBeastPoints(BeastStrengthFamily family) {
        int total = 0;
        for (BeastStrength beast : beasts) if (beast.getFamily() == family) total += beast.getPoints();
        return total;
    }

    public int getBeastCapacityBonus() {
        int total = 0;
        for (BeastStrength beast : beasts) total += beast.getCapacityBonus();
        return total;
    }

    public int getBeastDurationBonus() {
        int total = 0;
        for (BeastStrength beast : beasts) total += beast.getDurationBonusTicks();
        return total;
    }

    public float getBeastChanceBonus() {
        float total = 0.0F;
        for (BeastStrength beast : beasts) total += beast.getChanceBonus();
        return total;
    }

    public double getBeastDashBonus() {
        double total = 0.0D;
        for (BeastStrength beast : beasts) total += beast.getDashBonus();
        return total;
    }

    public double getBeastDashSpeedBonus() {
        double total = 0.0D;
        for (BeastStrength beast : beasts) total += beast.getDashSpeedBonus();
        return total;
    }

    public float getShieldMultiplier() {
        float total = 1.0F;
        for (BeastStrength beast : beasts) total += beast.getShieldBonus();
        return total;
    }

    public boolean hasTwoBoars() { return has(BeastStrength.WHITE_BOAR) && has(BeastStrength.BLACK_BOAR); }

    public double getBeastSprintBonus() {
        double total = 0.0D;
        for (BeastStrength beast : beasts) total += beast.getSprintBonus();
        return total;
    }

    public double getBeastSprintBonus(Collection<BeastStrengthFamily> families) {
        double total = 0.0D;
        for (BeastStrength beast : beasts) if (families.contains(beast.getFamily())) total += beast.getSprintBonus();
        return total;
    }

    public int getStrengthPoints() { return getTotalJin() + getBeastPoints(); }

    public static long getMarkPoints(int points) { return Math.max(0, points) * MARK_POINTS_PER_POINT; }

    public static int getPoints(long markPoints) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, markPoints) / MARK_POINTS_PER_POINT);
    }

    public int getJunReading() {
        return getReading(HumanStrength.JUN, HumanStrength.TEN_JUN)
                + HumanStrength.HUNDRED_FACTOR * getHumanStrengthCount(HumanStrength.HUNDRED_JUN);
    }

    public int getJinReading() { return getReading(HumanStrength.JIN, HumanStrength.TEN_JIN); }

    private int getReading(HumanStrength base, HumanStrength ten) {
        return getHumanStrengthCount(base) + HumanStrength.TEN_FACTOR * getHumanStrengthCount(ten);
    }

    public boolean hasPathBranch(StrengthPathBranch branch) {
        return switch (branch) {
            case BEAST_STRENGTH_PHANTOM -> !beasts.isEmpty();
            case HUMAN_JUN_STRENGTH -> !humanStrength.isEmpty();
            case ATMOSPHERIC_HEAVEN_AND_EARTH, NORMAL -> false;
        };
    }
}
