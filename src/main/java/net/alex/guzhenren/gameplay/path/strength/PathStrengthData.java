package net.alex.guzhenren.gameplay.path.strength;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * 力道读数：玩家炼入的兽力[Beast Strength]与人力，不可变。
 * <ul>
 *   <li>不存档：{@code PathStrengthService.get} 由肉体兽力、人力流派效果构建，效果是唯一真相，无道痕[Path Marks]
 *       即无力量；只含永久部分，限时增益以 {@link StrengthAmplifier} 计入
 *       {@code StrengthService.getStrengthPoints}。</li>
 *   <li>力量点[Strength Points]是两边共用单位：1 斤 = 1 点，兽力按声明点数；1 点 = {@link #STRANDS_PER_POINT}
 *       丝力道道痕，{@link #getStrands} 是唯一换算，蛊都经它声明丝数；{@link #getPoints(long)} 反向，
 *       结算由效果的丝读点数。</li>
 *   <li>{@link #with} 先移除被 {@link BeastStrength#replaces} 者，每兽种只持一种（成对猪除外）；{@link #getCovering}
 *       找出使新兽力多余者；构造器原样保留集合。</li>
 *   <li>⚠ 构造器须用 {@code new EnumMap<>(Class)} + {@code putAll} / {@code noneOf} + {@code addAll}：
 *       {@code EnumMap(Map)} 遇 {@code DEFAULT} 的空表会抛异常。</li>
 *   <li>⚠ 五种层数上限（9/9/30/30/30）合计恰为 99,999 斤，改一个即破坏。</li>
 *   <li>⚠ {@code getBeastReadings()} 按兽种计数，两种猪共用一个。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see PathStrengthService
 * @since 1.0.0
 */

public record PathStrengthData(Set<BeastStrength> beasts, Map<HumanStrength, Integer> humanStrength) {

    public static final PathStrengthData DEFAULT = new PathStrengthData(Set.of(), Map.of());
    public static final double ATTACK_PER_POINT = 0.125D;
    public static final long STRANDS_PER_POINT = 2L;

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

    /** 永久力量点[Strength Points]：全部斤数加已持兽力点数。 */
    public int getStrengthPoints() { return getTotalJin() + getBeastPoints(); }

    public static long getStrands(int points) { return Math.max(0, points) * STRANDS_PER_POINT; }

    public static int getPoints(long strands) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, strands) / STRANDS_PER_POINT);
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
