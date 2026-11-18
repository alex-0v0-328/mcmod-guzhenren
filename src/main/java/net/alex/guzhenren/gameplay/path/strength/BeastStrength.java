package net.alex.guzhenren.gameplay.path.strength;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.core.NamedEnum;
import net.minecraft.util.StringRepresentable;

/**
 * 肉体永久持有的兽力[Beast Strength]枚举：定义各兽力的所属兽种（{@link BeastStrengthFamily}）、力量点[Strength Points]
 * 与被动特性加成。
 * <ul>
 *   <li>存于 {@link PathStrengthData#beastStrengths}；每 1 点兽力等同于 1 斤人力，对应
 *       {@link PathStrengthData#ATTACK_PER_POINT} 空手攻击力并共同占用肉身承受力。</li>
 *   <li>同一兽种默认只能持有一种兽力（{@link #replaces}，如棕熊本力替换一熊之力而不叠加，{@link #isCoveredBy} 拦截不高
 *       于当前点数的同兽种蛊）；唯一例外为标有 {@link Form#PAIRED} 的白豕与黑豕，可并存组成「两猪之力」；
 *       {@link Form#INNATE} 则按本名显示。</li>
 *   <li>被动特性字段涵盖：承受力加成、疾跑速度倍率、非全力以赴下的全面解放概率、横冲冲刺距离与动画播放速度加成
 *       （{@link CrashDash}）、肉身护盾倍率（{@link BeastTraitEvents}）以及力道限时状态初始时长加成。</li>
 *   <li>「两猪之力」（{@link PathStrengthData#hasTwoBoars}）属于双猪并存的组合特性，不在单个枚举常量上设字段，由
 *       {@link CrashSources} 统一读取并加成冲撞距离、破块硬度与终点打击伤害。</li>
 *   <li>⚠ 兽种统计与虚影抽取一律按 {@link BeastStrengthFamily} 归并（白豕与黑豕共用
 *       {@link BeastStrengthFamily#BOAR}）。</li>
 * </ul>
 */

public enum BeastStrength implements NamedEnum {

    WHITE_BOAR(BeastStrengthFamily.BOAR, Form.PAIRED, 24),
    BLACK_BOAR(BeastStrengthFamily.BOAR, Form.PAIRED, 24),
    BEAR(BeastStrengthFamily.BEAR, Form.COUNTED, 40),
    TIGER(BeastStrengthFamily.TIGER, Form.COUNTED, 64),
    STONE_TURTLE(BeastStrengthFamily.TURTLE, Form.COUNTED, 48, 128, 0.0D, 0, 0.0F, 0.0D, 0.0D, 0.5F),
    GALLOPING_HORSE(BeastStrengthFamily.HORSE, Form.COUNTED, 48, 0, 0.5D, 0, 0.0F, 1.0D, 0.5D, 0.0F),
    GREEN_BULL(BeastStrengthFamily.OX, Form.COUNTED, 48, 64, 0.0D, 10 * Ticks.SECOND, 0.0F, 0.0D, 0.0D, 0.0F),
    BROWN_BEAR(BeastStrengthFamily.BEAR, Form.INNATE, 96, 0, 0.0D, 0, 0.05F, 0.0D, 0.0D, 0.0F);

    public enum Form { COUNTED, PAIRED, INNATE }

    public static final Codec<BeastStrength> CODEC = StringRepresentable.fromEnum(BeastStrength::values);
    private final BeastStrengthFamily family;
    private final Form form;
    private final int points;
    private final int capacityBonus;
    private final double sprintBonus;
    private final int durationBonusTicks;
    private final float chanceBonus;
    private final double dashBonus;
    private final double dashSpeedBonus;
    private final float shieldBonus;

    BeastStrength(BeastStrengthFamily family, Form form, int points) {
        this(family, form, points, 0, 0.0D, 0, 0.0F, 0.0D, 0.0D, 0.0F);
    }

    BeastStrength(BeastStrengthFamily family, Form form, int points, int capacityBonus, double sprintBonus,
                  int durationBonusTicks, float chanceBonus, double dashBonus, double dashSpeedBonus,
                  float shieldBonus) {
        this.family = family;
        this.form = form;
        this.points = points;
        this.capacityBonus = capacityBonus;
        this.sprintBonus = sprintBonus;
        this.durationBonusTicks = durationBonusTicks;
        this.chanceBonus = chanceBonus;
        this.dashBonus = dashBonus;
        this.dashSpeedBonus = dashSpeedBonus;
        this.shieldBonus = shieldBonus;
    }

    public BeastStrengthFamily getFamily() { return family; }

    public Form getForm() { return form; }

    public boolean replaces(BeastStrength held) {
        return held != this && held.family == family && !(held.form == Form.PAIRED && form == Form.PAIRED);
    }

    public boolean isCoveredBy(BeastStrength held) {
        return held == this || (replaces(held) && held.points >= points);
    }

    public int getPoints() { return points; }

    public int getCapacityBonus() { return capacityBonus; }

    public double getSprintBonus() { return sprintBonus; }

    public int getDurationBonusTicks() { return durationBonusTicks; }

    public float getChanceBonus() { return chanceBonus; }

    public double getDashBonus() { return dashBonus; }

    public double getDashSpeedBonus() { return dashSpeedBonus; }

    public float getShieldBonus() { return shieldBonus; }

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.strength.beast_strength."; }
}
