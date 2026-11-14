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
 * 力道结算：身体实际发挥的力量点[Strength Points]。
 * <ul>
 *   <li>静态只读，位于空窍[Aperture]与肉体之上（承受力读两者）；永久点数读兽力[Beast Strength]与人力效果的力道丝
 *       （{@link #getPermanentPoints}、{@link PathStrengthData#getPoints(long)}，无效果无点数），增幅见
 *       {@link #getAmplifiers}，解放额外点数由轮次管理方设置的 {@link ReleaseSource} 给出；{@link #snapshot} 一次收集前
 *       两类及结算（{@link StrengthSnapshot}），承受力、总点数、发挥点数各取一份。</li>
 *   <li>常态承受力 {@link #getNormalCapacity} = {@link #getBaseCapacity}（{@link #MORTAL_CAPACITY} + 每转
 *       {@link #CAPACITY_PER_RANK}：16，一至五转 24–56）× 体质倍率 + 皮筋骨与兽力加成（石龟
 *       {@link BeastStrength#getCapacityBonus}，不乘倍率）。</li>
 *   <li>当前承受力 {@link #getCapacity} 再加增幅承受加成（含苦力蛊[Hardship Strength Gu]）与
 *       {@link #getAllOutCapacityBonus}：全力以赴蛊[All-Out Effort Gu]在持人力钧力流或兽力虚影流的身体上运行时，三 / 四
 *       / 五转加常态 50% / 100% / 200%，转数读效果等级即蛊 tier（{@link #getAllOutTier}，至少
 *       {@link #ALL_OUT_LOWEST_TIER}：命令给的 0 级按三转算；否则 {@link #NO_ALL_OUT}）；各加成以常态为基数相加，不
 *       互乘；{@link #isAllOut} 为是否版，供范围冲击概率与横冲系冲刺（后者在所属客户端读同步数据）。</li>
 *   <li>发挥 {@link #getUsablePoints} = 当前承受力下 {@code min(total, capacity)} + 肉体过载 + 解放额外点数，余者保留；
 *       过载 {@link #getOverloadPoints} 为限时兽力超当前承受力部分（计在永久兽力上），全额发挥、每拳扣血
 *       （{@code BodyOverload}）；指令超限永久兽力与人力斤数只截断。</li>
 *   <li>{@link #getReleasedExtra}：全面解放，类似暴击，被解放兽种永久点数 × {@link #FULL_RELEASE_FACTOR}、其余 × 1，合
 *       计仍截于当前承受力，取超出普通截断的部分；{@link #canAdmit}：留下的永久兽力 + 新兽力 ≤ 常态承受力，计留下兽力的
 *       承受加成，不计新兽力与被替换兽力的（{@code PathStrengthData#withoutReplacedBy}），增幅与全力以赴不参与。</li>
 *   <li>⚠ {@code int} 重载为单测接缝，无需 {@link Player}；唯一可变静态 {@code releaseSource} 亦为接缝，轮次管理方在通
 *       用初始化设一次、不再换，之前返回 0。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see PathStrengthService
 * @see AttackDamageService
 * @since 1.0.0
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

    /** 此刻的解放额外点数，轮外为 0。 */
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
        long strands = 0L;
        for (PathEffect<?> effect : PathEffectService.getEffects(player)) {
            if (effect.type() == ModPathEffectTypes.BEAST_STRENGTH
                    || !beastsOnly && effect.type() == ModPathEffectTypes.HUMAN_STRENGTH) {
                strands += effect.getMarks(GuPath.STRENGTH).strands();
            }
        }
        return PathStrengthData.getPoints(strands);
    }

    /** 生效中的增幅：先限时兽力增益，后各路径效果 payload。 */
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

    /** 总力量：永久点数 + 各增幅点数。 */
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

    /** 常态承受力能否再容一种永久兽力。 */
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
