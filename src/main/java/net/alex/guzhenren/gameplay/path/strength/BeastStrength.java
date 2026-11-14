package net.alex.guzhenren.gameplay.path.strength;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.core.NamedEnum;
import net.minecraft.util.StringRepresentable;

/**
 * 肉体可持有的兽力[Beast Strength]及其力量点[Strength Points]。
 * <ul>
 *   <li>存于 {@code PathStrengthData} 的 {@code Set}；新增兽力只加常量，不碰蛊物品与攻击合成；兄弟模组不得新增；
 *       每点 = 1 斤人力 = {@link PathStrengthData#ATTACK_PER_POINT} 攻击，同走承受力。</li>
 *   <li>每兽种只持一种：{@link #replaces}（一熊之力变棕熊本力，不叠加），{@link #isCoveredBy} 拦下不更强的蛊；
 *       唯一例外（今后亦然）为 {@link Form#PAIRED} 白豕、黑豕并存成两猪之力；{@link Form#INNATE} 按本名显示。</li>
 *   <li>持有期间特性：承受力、疾跑倍率、全力以赴外全面解放概率、冲撞倍数（全力以赴翻倍，
 *       {@code CrashDash#getMultiple}）、护盾点数、力道限时状态开始时的加时长；各兽数值见 wiki《蛊虫条目》。</li>
 *   <li>两猪之力（{@link PathStrengthData#hasTwoBoars}）冲撞时破方块（{@code CrashBreach}）。</li>
 *   <li>⚠ 兽种标记按兽种不按常量（白豕、黑豕共用 {@code BOAR}）；兽种与点数勿移到读取方。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public enum BeastStrength implements NamedEnum {

    WHITE_BOAR(BeastStrengthFamily.BOAR, Form.PAIRED, 24),
    BLACK_BOAR(BeastStrengthFamily.BOAR, Form.PAIRED, 24),
    BEAR(BeastStrengthFamily.BEAR, Form.COUNTED, 40),
    TIGER(BeastStrengthFamily.TIGER, Form.COUNTED, 64),
    STONE_TURTLE(BeastStrengthFamily.TURTLE, Form.COUNTED, 48, 128, 0.0D, 0, 0.0F, 0.0D, 0.5F),
    GALLOPING_HORSE(BeastStrengthFamily.HORSE, Form.COUNTED, 48, 0, 0.5D, 0, 0.0F, 1.0D, 0.0F),
    GREEN_BULL(BeastStrengthFamily.OX, Form.COUNTED, 48, 64, 0.0D, 10 * Ticks.SECOND, 0.0F, 0.0D, 0.0F),
    BROWN_BEAR(BeastStrengthFamily.BEAR, Form.INNATE, 96, 0, 0.0D, 0, 0.05F, 0.0D, 0.0F);

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
    private final float shieldBonus;

    BeastStrength(BeastStrengthFamily family, Form form, int points) {
        this(family, form, points, 0, 0.0D, 0, 0.0F, 0.0D, 0.0F);
    }

    BeastStrength(BeastStrengthFamily family, Form form, int points, int capacityBonus, double sprintBonus,
                  int durationBonusTicks, float chanceBonus, double dashBonus, float shieldBonus) {
        this.family = family;
        this.form = form;
        this.points = points;
        this.capacityBonus = capacityBonus;
        this.sprintBonus = sprintBonus;
        this.durationBonusTicks = durationBonusTicks;
        this.chanceBonus = chanceBonus;
        this.dashBonus = dashBonus;
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

    public float getShieldBonus() { return shieldBonus; }

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.strength.beast_strength."; }
}
