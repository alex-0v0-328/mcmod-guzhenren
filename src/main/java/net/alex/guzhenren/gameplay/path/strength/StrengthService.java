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
 * 力道综合结算服务：计算肉身承受力、总力量点[Strength Points]、肉体过载与实际可发挥力量点。
 * <ul>
 *   <li>纯只读静态服务：{@link #getPermanentPoints} 从永久兽力[Beast Strength]与人力流派效果的道痕[Path Marks]点换算永
 *       久力量点，{@link #getAmplifiers} 采集限时兽力与各 {@link StrengthAmplifierProvider} 增幅，全面解放额外点数由
 *       {@link ComboRound} 注入的 {@link ReleaseSource} 提供；{@link #snapshot} 一次性采集并返回
 *       {@link StrengthSnapshot}。</li>
 *   <li>常态承受力 {@link #getNormalCapacity} = 基础承受力 {@link #getBaseCapacity}（{@code 16 + 8 * 转数序号}，乘以十
 *       绝体承受力倍率）+ 皮、筋、骨炼体加成 + 永久兽力承受力加成（如石龟之力，固定加算、不乘十绝体倍率）。</li>
 *   <li>当前承受力 {@link #getCapacity} = 常态承受力 + 增幅承受力加成（含苦力蛊[Hardship Strength Gu]）+
 *       {@link #getAllOutCapacityBonus}（已入兽力虚影流或人力钧力流且运行三/四/五转全力以赴蛊[All-Out Effort Gu]时，按
 *       常态承受力的 50% / 100% / 200% 加算，与苦力蛊同以常态承受力为基数相加、互不叠乘）。</li>
 *   <li>可发挥力量点 {@link #getUsablePoints} = {@code min(totalPoints, capacity) + overloadPoints + releasedExtra}：其
 *       中 {@link #getOverloadPoints} 为限时兽力超出当前承受力的部分（全额发挥并按拳触发 {@link BodyOverload} 扣血，而
 *       指令赋予的超限永久力量仅截断不发挥）。</li>
 *   <li>{@link #getReleasedExtra} 计算连招轮收尾招全面解放兽种带来的额外发挥点数（解放兽种的永久点数乘以
 *       {@link #FULL_RELEASE_FACTOR}，连同其余点数仍封顶于当前承受力）；{@link #canAdmit} 校验扣除被替换旧兽力后，常态
 *       承受力能否容纳新永久兽力（不计临时增幅与全力以赴）。</li>
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
    public static final int FULL_RELEASE_FACTOR = 3;
    private static ReleaseSource releaseSource = (player, snapshot) -> 0;

    @FunctionalInterface
    public interface ReleaseSource {

        int getReleasedExtra(@NotNull Player player, @NotNull StrengthSnapshot snapshot);
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

    public static int getPermanentPoints(@NotNull Player player) { return getCarvedPoints(player, false); }

    private static int getCarvedPoints(Player player, boolean beastsOnly) {
        long markPoints = 0L;
        for (PathEffect<?> effect : PathEffectService.getEffects(player)) {
            if (effect.type() == ModPathEffectTypes.BEAST_STRENGTH
                    || !beastsOnly && effect.type() == ModPathEffectTypes.HUMAN_STRENGTH) {
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

    public static int getTimedBeastPoints(@NotNull List<StrengthAmplifier> amplifiers) {
        int total = 0;
        for (StrengthAmplifier amplifier : amplifiers) {
            if (amplifier.isBeast()) total += Math.max(0, amplifier.points());
        }
        return total;
    }

    public static int getCapacityBonus(@NotNull List<StrengthAmplifier> amplifiers) {
        int total = 0;
        for (StrengthAmplifier amplifier : amplifiers) total += amplifier.capacityBonus();
        return total;
    }

    public static int getStrengthPoints(@NotNull Player player) { return snapshot(player).totalPoints(); }

    public static @NotNull StrengthSnapshot snapshot(@NotNull Player player) {
        int normal = getNormalCapacity(player);
        int permanent = getPermanentPoints(player);
        return StrengthSnapshot.of(normal, permanent, getCarvedPoints(player, true),
                getAmplifiers(player, permanent, normal), getAllOutTier(player));
    }

    public static int getUsablePoints(@NotNull Player player) { return getUsablePoints(player, snapshot(player)); }

    public static int getUsablePoints(@NotNull Player player, @NotNull StrengthSnapshot snapshot) {
        return getUsablePoints(snapshot.capacity(), snapshot.totalPoints()) + snapshot.overloadPoints()
                + releaseSource.getReleasedExtra(player, snapshot);
    }

    public static int getUsablePoints(int capacity, int total) { return Math.min(capacity, total); }

    public static int getOverloadPoints(int capacity, int permanentBeastPoints, int timedBeastPoints) {
        int timed = Math.max(0, timedBeastPoints);
        long past = (long) Math.max(0, permanentBeastPoints) + timed - capacity;
        return (int) Math.clamp(past, 0L, timed);
    }

    public static int getReleasedExtra(int capacity, int total, int fullReleasePoints) {
        long released = Math.max(0, total) + (long) (FULL_RELEASE_FACTOR - 1) * Math.max(0, fullReleasePoints);
        return Math.max(0, (int) Math.min(capacity, released) - getUsablePoints(capacity, total));
    }

    public static boolean canAdmit(@NotNull Player player, @NotNull BeastStrength beast) {
        PathStrengthData held = PathStrengthService.get(player);
        PathStrengthData kept = held.withoutReplacedBy(beast);
        int normal = getNormalCapacity(player) - held.getBeastCapacityBonus() + kept.getBeastCapacityBonus();
        return canAdmit(normal, kept.getBeastPoints(), beast.getPoints());
    }

    public static boolean canAdmit(int normalCapacity, int heldBeastPoints, int newBeastPoints) {
        return heldBeastPoints + newBeastPoints <= normalCapacity;
    }
}
