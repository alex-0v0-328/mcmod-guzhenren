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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * 力道综合结算服务：计算肉身承受力、总力量点[Strength Points]、结算力量点与永久力量的准入。
 * <ul>
 *   <li>纯只读静态服务：{@link #getPermanentPoints} 从永久兽力[Beast Strength]与人力流派效果的道痕[Path Marks]点换算永
 *       久力量点，{@link #getAmplifiers} 采集限时兽力与各 {@link StrengthAmplifierProvider} 增幅；{@link #snapshot} 一次
 *       性采集并返回 {@link StrengthSnapshot}。</li>
 *   <li>承受力 {@link #getCapacity} = 基础承受力 {@link #getBaseCapacity}（体质的未开窍起点 + 每转增量 × 转数序号，
 *       数值见 {@link ExtremePhysique}）+ 皮、筋、骨炼体加成 + 永久兽力承受力加成（如石龟之力，固定加算）；增幅与全力以赴蛊
 *       [All-Out Effort Gu]都不加承受力。</li>
 *   <li>结算力量点 {@link #getSettledPoints} = {@code floor((总力量点 + 暴击兽种点数) × Π 伤害倍率)}：暴击兽种点数由
 *       {@link ComboRound#getCritPoints} 提供（仅服务端玩家、仅收尾招进行中），伤害倍率为蓄力与苦力。点数全额发挥、不按
 *       承受力截断，超出承受力的部分 {@link #getOverloadPoints} 由 {@link BodyOverload} 结算过载反噬。</li>
 *   <li>新内容按层接入：加点数的登记为来源（{@link StrengthAmplifier#points}），放大整次伤害的进伤害倍率
 *       （{@link StrengthAmplifier#damagePercent}），换打法的在出手处乘远程系数（不进过载）。</li>
 *   <li>准入 {@link #canAdmit}：永久兽力与永久人力共用，已有永久点数（扣除被替换的旧兽力）加新增点数不得超过承受力；增
 *       幅与指令不过准入。</li>
 *   <li>⚠ 接收基本类型的重载方法供无 {@link Player} 环境的单元测试直接调用；暴击点数只在服务端读 {@link ComboRound}，客户
 *       端读数恒不含暴击。</li>
 * </ul>
 */

public final class StrengthService {

    private StrengthService() {}

    public static final int NO_ALL_OUT = -1;
    public static final int ALL_OUT_LOWEST_TIER = Rank.THREE.ordinal() - Rank.ONE.ordinal();
    private static final double FLOOR_SLACK = 1.0E-6D;

    public static int getBaseCapacity(@NotNull Rank rank, @NotNull ExtremePhysique physique) {
        int rankIndex = Math.min(rank.ordinal(), Rank.HIGHEST.ordinal());
        return physique.getUnawakenedCapacity() + physique.getCapacityPerRank() * rankIndex;
    }

    public static int getCapacity(@NotNull Player player) {
        BodyData body = BodyService.get(player);
        return getBaseCapacity(ApertureService.getHealthRank(player), body.extremePhysique())
                + body.skin().getCapacityBonus()
                + body.tendon().getCapacityBonus()
                + body.bone().getCapacityBonus()
                + PathStrengthService.get(player).getBeastCapacityBonus();
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
        return getAmplifiers(player, getPermanentPoints(player), getCapacity(player));
    }

    private static List<StrengthAmplifier> getAmplifiers(Player player, int permanent, int capacity) {
        List<StrengthAmplifier> amplifiers = new ArrayList<>(PathStrengthService.getBeastBuffs(player));
        for (PathEffect<?> effect : PathEffectService.getEffects(player)) {
            if (!(effect.payload() instanceof StrengthAmplifierProvider provider)) continue;

            StrengthAmplifier amplifier = provider.getAmplifier(player, permanent, capacity);
            if (amplifier != null) amplifiers.add(amplifier);
        }
        return amplifiers;
    }

    public static int getAmplifierPoints(@NotNull List<StrengthAmplifier> amplifiers) {
        int total = 0;
        for (StrengthAmplifier amplifier : amplifiers) total += amplifier.points();
        return total;
    }

    public static double getDamageFactor(@NotNull List<StrengthAmplifier> amplifiers) {
        double factor = 1.0D;
        for (StrengthAmplifier amplifier : amplifiers) factor *= 1.0D + amplifier.damagePercent() / 100.0D;
        return factor;
    }

    public static int getStrengthPoints(@NotNull Player player) { return snapshot(player).totalPoints(); }

    public static @NotNull StrengthSnapshot snapshot(@NotNull Player player) {
        int capacity = getCapacity(player);
        int permanent = getPermanentPoints(player);
        return StrengthSnapshot.of(capacity, permanent, getAmplifiers(player, permanent, capacity));
    }

    public static int getSettledPoints(@NotNull Player player) { return getSettledPoints(player, snapshot(player)); }

    public static int getSettledPoints(@NotNull Player player, @NotNull StrengthSnapshot snapshot) {
        return snapshot.settle(player instanceof ServerPlayer server ? ComboRound.getCritPoints(server) : 0, 1.0D);
    }

    public static int getSettledPoints(int totalPoints, int critPoints, double damageFactor) {
        long weighted = (long) Math.max(0, totalPoints) + Math.max(0, critPoints);
        return (int) Math.min(Integer.MAX_VALUE, Math.floor(weighted * Math.max(0.0D, damageFactor) + FLOOR_SLACK));
    }

    public static int getOverloadPoints(int settledPoints, int capacity) {
        return Math.max(0, settledPoints - Math.max(0, capacity));
    }

    public static boolean canAdmit(@NotNull Player player, @NotNull BeastStrength beast) {
        PathStrengthData held = PathStrengthService.get(player);
        PathStrengthData kept = held.withoutReplacedBy(beast);
        int capacity = getCapacity(player) - held.getBeastCapacityBonus() + kept.getBeastCapacityBonus();
        return canAdmit(capacity, kept.getStrengthPoints(), beast.getPoints());
    }

    public static boolean canAdmit(@NotNull Player player, @NotNull HumanStrength kind) {
        return canAdmit(getCapacity(player), PathStrengthService.get(player).getStrengthPoints(), kind.getJin());
    }

    public static boolean canAdmit(int capacity, int heldPoints, int newPoints) {
        return heldPoints + newPoints <= capacity;
    }
}
