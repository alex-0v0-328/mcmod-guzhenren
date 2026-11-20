package net.alex.guzhenren.gameplay.path.strength;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * 力道暴击[Crit]规则：兽力虚影流[Beast Strength Phantom]与人力钧力流[Human Jun Strength]共用的暴击率、兽种抽取与暴击点
 * 数，由 {@link ComboRound}、蓄力一拳[Charged Punch]与力气蛊[Strength Qi Gu]远程攻击共同读取。
 * <ul>
 *   <li>暴击率 {@link #getChance}：平时 {@link #BASE_CHANCE} 加永久兽力的掷骰加成（棕熊本力，
 *       {@link PathStrengthData#getBeastChanceBonus}）；全力以赴蛊[All-Out Effort Gu]运行于三 / 四 / 五转时为
 *       80% / 90% / 100%，不再加兽力加成；未入两流（{@link StrengthPathBranch#NORMAL}）为 {@code 0}。</li>
 *   <li>兽力流暴击从持有的永久兽种（{@link #getAvailableFamilies}，仅有限时兽力时为空）中不放回抽取
 *       {@link #getCount} = {@code min(1 + 苦力伤势档位, 兽种数)} 种（{@link #pickFamilies}），每个抽中兽种的永久点数在结
 *       算力量点里再计一份（{@link #getCritPoints}，合计为 {@link #CRIT_FACTOR} 份）。人力流暴击不加点数，只放范围伤害。</li>
 *   <li>⚠ 纯静态、无状态：一轮只掷一次的约束由 {@link ComboRound} 保证，蓄满必暴击由
 *       {@link StoreStrength#getCritChance} 叠加。</li>
 * </ul>
 */

public final class StrengthCrit {

    private StrengthCrit() {}

    public static final float BASE_CHANCE = 0.05F;
    public static final int CRIT_FACTOR = 2;
    private static final float[] ALL_OUT_CHANCES = { 0.80F, 0.90F, 1.0F };

    //region Chance [暴击率]
    public static float getChance(int allOutTier, float beastBonus) {
        if (allOutTier < StrengthService.ALL_OUT_LOWEST_TIER) return Math.min(1.0F, BASE_CHANCE + beastBonus);
        return ALL_OUT_CHANCES[Math.min(allOutTier - StrengthService.ALL_OUT_LOWEST_TIER,
                ALL_OUT_CHANCES.length - 1)];
    }

    public static float getChance(@NotNull Player player) {
        if (PathStrengthService.getHeldBranch(player) == StrengthPathBranch.NORMAL) return 0.0F;
        return getChance(StrengthService.getAllOutTier(player), PathStrengthService.get(player).getBeastChanceBonus());
    }
    //endregion

    //region Families [兽种]
    public static int getCount(int hardshipTier, int families) {
        return Math.min(1 + Math.max(0, hardshipTier), Math.max(0, families));
    }

    public static @NotNull List<BeastStrengthFamily> getAvailableFamilies(@NotNull PathStrengthData data) {
        List<BeastStrengthFamily> families = new ArrayList<>();
        for (BeastStrengthFamily family : BeastStrengthFamily.values()) {
            if (data.getBeastPoints(family) > 0) families.add(family);
        }
        return families;
    }

    public static @NotNull List<BeastStrengthFamily> pickFamilies(@NotNull List<BeastStrengthFamily> available,
                                                                  int count, @NotNull RandomSource random) {
        List<BeastStrengthFamily> pool = new ArrayList<>(available);
        List<BeastStrengthFamily> picked = new ArrayList<>();
        while (picked.size() < count && !pool.isEmpty()) picked.add(pool.remove(random.nextInt(pool.size())));
        return picked;
    }

    public static int getCritPoints(@NotNull List<BeastStrengthFamily> families, @NotNull PathStrengthData permanent) {
        int points = 0;
        for (BeastStrengthFamily family : families) points += Math.max(0, permanent.getBeastPoints(family));
        return points;
    }
    //endregion
}
