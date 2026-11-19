package net.alex.guzhenren.gameplay.path.strength;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.body.BodyData;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.path.PathEffect;
import net.alex.guzhenren.gameplay.path.PathEffectService;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.alex.guzhenren.registry.effect.ModPathEffectTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * 力道综合结算服务：计算肉身承受力、总力量点[Strength Points]、结算力量点与永久力量的准入。
 * <ul>
 *   <li>纯只读静态服务：{@link #getPermanentPoints} 从永久兽力[Beast Strength]与人力流派效果的道痕[Path Marks]点换算永
 *       久力量点，{@link #getAmplifiers} 采集限时兽力与各 {@link StrengthAmplifierProvider} 增幅，本击解放兽种的永久点
 *       数由 {@link ComboRound} 注入的 {@link ReleaseSource} 提供；{@link #snapshot} 一次性采集并返回
 *       {@link StrengthSnapshot}。</li>
 *   <li>常态承受力 {@link #getNormalCapacity} = 基础承受力 {@link #getBaseCapacity}（{@code 16 + 8 * 转数序号}，乘以十
 *       绝体承受力倍率）+ 皮、筋、骨炼体加成 + 永久兽力承受力加成（如石龟之力，固定加算、不乘十绝体倍率）。</li>
 *   <li>当前承受力 {@link #getCapacity} = 常态承受力 + 增幅承受力加成 + {@link #getAllOutCapacityBonus}（已入兽力虚影
 *       流或人力钧力流且运行三/四/五转全力以赴蛊[All-Out Effort Gu]时，按常态承受力的 50% / 100% / 200% 加算）。</li>
 *   <li>结算力量点 {@link #getSettledPoints} = {@code floor(Σ(来源点数 × 来源倍率) × Π 整拳倍率)}：来源为每个永久兽
 *       种、人力与每项增幅；来源倍率只有全面解放的永久兽种为 {@link #FULL_RELEASE_FACTOR}，其余为 1；整拳倍率为蓄力与
 *       苦力。点数全额发挥、不按承受力截断，超出当前承受力的部分 {@link #getOverloadPoints} 由 {@link BodyOverload}
 *       结算过载反噬。</li>
 *   <li>新内容按层接入：加点数的登记为来源（{@link StrengthAmplifier#points}），只放大某个来源的进来源倍率，放大整拳
 *       的进整拳倍率（{@link StrengthAmplifier#punchPercent}），换打法的在出手处乘出手系数（不进过载），抬上限的进承
 *       受力（{@link StrengthAmplifier#capacityBonus}）。</li>
 *   <li>准入 {@link #canAdmit}：永久兽力与永久人力共用，已有永久点数（扣除被替换的旧兽力）加新增点数不得超过常态承受
 *       力（不计增幅与全力以赴）；增幅与指令不过准入。</li>
 *   <li>⚠ 接收基本类型的重载方法供无 {@link Player} 环境的单元测试直接调用；唯一可变静态字段 {@code releaseSource} 仅在
 *       通用初始化阶段由 {@link ComboRound} 设置一次。</li>
 * </ul>
 */

public final class StrengthService {

    private StrengthService() {}

    public static final int MORTAL_CAPACITY = 16;
    public static final int CAPACITY_PER_RANK = 8;
    public static final int NO_ALL_OUT = -1;
    public static final int ALL_OUT_LOWEST_TIER = Rank.THREE.ordinal() - Rank.ONE.ordinal();
    private static final int[] ALL_OUT_CAPACITY_PERCENT = { 50, 100, 200 };
    public static final int FULL_RELEASE_FACTOR = 2;
    private static final double FLOOR_SLACK = 1.0E-6D;
    private static ReleaseSource releaseSource = player -> 0;

    @FunctionalInterface
    public interface ReleaseSource {

        int getReleasedPoints(@NotNull Player player);
    }

    public static void setReleaseSource(@NotNull ReleaseSource source) { releaseSource = source; }

    public static int getBaseCapacity(@NotNull Rank rank, @NotNull ExtremePhysique physique) {
        int rankIndex = Math.min(rank.ordinal(), Rank.HIGHEST.ordinal());
        return (MORTAL_CAPACITY + CAPACITY_PER_RANK * rankIndex) * physique.getCapacityMultiplier();
    }

    public static int getNormalCapacity(@NotNull Player player) {
        BodyData body = BodyService.get(player);
        return getBaseCapacity(ApertureService.getHealthRank(player), body.extremePhysique())
                + body.skin().getCapacityBonus()
                + body.tendon().getCapacityBonus()
                + body.bone().getCapacityBonus()
                + PathStrengthService.get(player).getBeastCapacityBonus();
    }

    public static int getCapacity(@NotNull Player player) { return snapshot(player).capacity(); }

    public static int getCapacity(int normalCapacity, int amplifierBonus, int allOutTier) {
        return normalCapacity + amplifierBonus + getAllOutCapacityBonus(normalCapacity, allOutTier);
    }

    public static int getAllOutCapacityBonus(int normalCapacity, int allOutTier) {
        if (allOutTier < ALL_OUT_LOWEST_TIER) return 0;

        int percent = ALL_OUT_CAPACITY_PERCENT[Math.min(allOutTier - ALL_OUT_LOWEST_TIER,
                ALL_OUT_CAPACITY_PERCENT.length - 1)];
        return (int) ((long) Math.max(0, normalCapacity) * percent / 100L);
    }

    public static int getAllOutTier(@NotNull Player player) {
        MobEffectInstance effect = player.getEffect(ModEffects.ALL_OUT_EFFORT_GU);
        if (effect == null || PathStrengthService.getHeldBranch(player) == StrengthPathBranch.NORMAL) return NO_ALL_OUT;
        return Math.max(ALL_OUT_LOWEST_TIER, effect.getAmplifier());
    }

    public static boolean isAllOut(@NotNull Player player) { return getAllOutTier(player) != NO_ALL_OUT; }

    public static int getPermanentPoints(@NotNull Player player) {
        long markPoints = 0L;
        for (PathEffect<?> effect : PathEffectService.getEffects(player)) {
            if (effect.type() == ModPathEffectTypes.BEAST_STRENGTH
                    || effect.type() == ModPathEffectTypes.HUMAN_STRENGTH) {
                markPoints += effect.getMarks(GuPath.STRENGTH).points();
            }
        }
        return PathStrengthData.getPoints(markPoints);
    }

    public static @NotNull List<StrengthAmplifier> getAmplifiers(@NotNull Player player) {
        return getAmplifiers(player, getPermanentPoints(player), getNormalCapacity(player));
    }

    private static List<StrengthAmplifier> getAmplifiers(Player player, int permanent, int normal) {
        List<StrengthAmplifier> amplifiers = new ArrayList<>(PathStrengthService.getBeastBuffs(player));
        for (PathEffect<?> effect : PathEffectService.getEffects(player)) {
            if (!(effect.payload() instanceof StrengthAmplifierProvider provider)) continue;

            StrengthAmplifier amplifier = provider.getAmplifier(player, permanent, normal);
            if (amplifier != null) amplifiers.add(amplifier);
        }
        return amplifiers;
    }

    public static int getAmplifierPoints(@NotNull List<StrengthAmplifier> amplifiers) {
        int total = 0;
        for (StrengthAmplifier amplifier : amplifiers) total += amplifier.points();
        return total;
    }

    public static int getCapacityBonus(@NotNull List<StrengthAmplifier> amplifiers) {
        int total = 0;
        for (StrengthAmplifier amplifier : amplifiers) total += amplifier.capacityBonus();
        return total;
    }

    public static double getPunchFactor(@NotNull List<StrengthAmplifier> amplifiers) {
        double factor = 1.0D;
        for (StrengthAmplifier amplifier : amplifiers) factor *= 1.0D + amplifier.punchPercent() / 100.0D;
        return factor;
    }

    public static int getStrengthPoints(@NotNull Player player) { return snapshot(player).totalPoints(); }

    public static @NotNull StrengthSnapshot snapshot(@NotNull Player player) {
        int normal = getNormalCapacity(player);
        int permanent = getPermanentPoints(player);
        return StrengthSnapshot.of(normal, permanent, getAmplifiers(player, permanent, normal), getAllOutTier(player));
    }

    public static int getSettledPoints(@NotNull Player player) { return getSettledPoints(player, snapshot(player)); }

    public static int getSettledPoints(@NotNull Player player, @NotNull StrengthSnapshot snapshot) {
        return snapshot.settle(releaseSource.getReleasedPoints(player), 1.0D);
    }

    public static int getSettledPoints(int totalPoints, int releasedPoints, double punchFactor) {
        long weighted = Math.max(0, totalPoints) + (long) (FULL_RELEASE_FACTOR - 1) * Math.max(0, releasedPoints);
        return (int) Math.min(Integer.MAX_VALUE, Math.floor(weighted * Math.max(0.0D, punchFactor) + FLOOR_SLACK));
    }

    public static int getOverloadPoints(int settledPoints, int capacity) {
        return Math.max(0, settledPoints - Math.max(0, capacity));
    }

    public static boolean canAdmit(@NotNull Player player, @NotNull BeastStrength beast) {
        PathStrengthData held = PathStrengthService.get(player);
        PathStrengthData kept = held.withoutReplacedBy(beast);
        int normal = getNormalCapacity(player) - held.getBeastCapacityBonus() + kept.getBeastCapacityBonus();
        return canAdmit(normal, kept.getStrengthPoints(), beast.getPoints());
    }

    public static boolean canAdmit(@NotNull Player player, @NotNull HumanStrength kind) {
        return canAdmit(getNormalCapacity(player), PathStrengthService.get(player).getStrengthPoints(), kind.getJin());
    }

    public static boolean canAdmit(int normalCapacity, int heldPoints, int newPoints) {
        return heldPoints + newPoints <= normalCapacity;
    }
}
