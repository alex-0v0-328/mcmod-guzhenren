package net.alex.guzhenren.display;

import java.util.Locale;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.aperture.Aperture;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.aperture.Talent;
import net.alex.guzhenren.gameplay.aperture.Title;
import net.alex.guzhenren.gameplay.body.BodyData;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.alex.guzhenren.gameplay.body.Physique;
import net.alex.guzhenren.gameplay.path.PathMarks;
import net.alex.guzhenren.gameplay.path.GuAttainment;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.path.PathEffect;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import net.alex.guzhenren.gameplay.path.strength.HumanStrength;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

/**
 * HUD、B 面板与蛊虫物品提示共用的本地化文本格式化工具（与 {@link InfoModel} 配合使用）。
 * <ul>
 *   <li>作为跨界面共享措辞的唯一来源，凡两处界面表述同一事实时均应在此统一格式化，避免各自拼接导致文案分歧。</li>
 *   <li>方括号（如 {@code [无]}、{@code [太日阳莽体]}）写在语言文件译文中而非在此硬编码拼接；{@link #formatNone} 输出占
 *       位文本，{@link #formatDetail} 包装暗灰色旁注，{@link #formatTalentLine} 为资质[Aptitude]追加基础真元成数。</li>
 *   <li>{@link #getCountdownSeconds} 将剩余刻数向上取整为整秒供界面按秒缓存文本，{@link #formatEffectTime} 则将剩余整秒
 *       或 {@code PathEffect.PERMANENT} 格式化为倒计时或永久标记。</li>
 *   <li>{@link #formatMarks} 并列格式化道痕[Path Marks]的整数条数与散点数（使用千位分隔符且互不进位换算），为 0 的分量
 *       会省略，全 0 行由调用方隐藏。</li>
 *   <li>⚠ {@link #formatLifespan(double, double)} 保留 2 位小数而非 1 位：在正常流速下，0.01 年对应现实约 12 秒，是玩
 *       家能肉眼察觉寿元流动的最粗显示步长（0.1 年会导致显示静止约 2 分钟）。</li>
 * </ul>
 */

public final class ModDisplayText {

    private ModDisplayText() {}

    private static final String GAP = "  ";

    private static MutableComponent formatRealm(Aperture aperture) {
        return Component.translatable("guzhenren.display.realm",
                Component.translatable(aperture.rank().getTranslationKey()),
                Component.translatable(aperture.stage().getTranslationKey()));
    }

    public static MutableComponent formatRealmTitle(Aperture aperture) {
        MutableComponent title = Component.translatable(Title.fromRank(aperture.rank()).getTranslationKey());
        if (aperture.rank() == Rank.NONE) return title;

        return Component.translatable("guzhenren.display.realm_title", formatRealm(aperture), title);
    }

    private static MutableComponent formatTalent(Aperture aperture) {
        return Component.translatable(aperture.getTalent().getTranslationKey());
    }

    public static MutableComponent formatTalentLine(Aperture aperture, boolean awakened) {
        MutableComponent line = formatTalent(aperture);
        if (awakened) line.append(formatDetail(Talent.formatPercent(aperture.baseEssence())));
        return line;
    }

    public static MutableComponent formatNone() { return Component.translatable("guzhenren.display.none"); }

    public static MutableComponent formatDetail(Object value) {
        return Component.translatable("guzhenren.display.detail", value).withStyle(ChatFormatting.DARK_GRAY);
    }

    public static MutableComponent formatHudHeader(Aperture aperture, BodyData body) {
        return formatRealmTitle(aperture).append(GAP).append(formatHudAptitude(aperture, body));
    }

    public static MutableComponent formatHudAptitude(Aperture aperture, BodyData body) {
        if (aperture.getTalent() == Talent.NONE) return formatTalent(aperture);

        Component detail = aperture.getTalent() == Talent.TEN_EXTREMES && body.isExtreme()
                ? Component.translatable(body.extremePhysique().getTranslationKey())
                : Talent.formatPercent(aperture.baseEssence());
        return Component.translatable("guzhenren.display.aptitude_line", formatTalent(aperture), detail);
    }

    public static MutableComponent formatPhysiqueValue(@Nullable Physique physique, ExtremePhysique extremePhysique) {
        if (physique == null) return formatNone();

        MutableComponent value = Component.translatable(physique.getTranslationKey());
        if (physique == Physique.EXTREME) {
            value.append(" ").append(Component.translatable("guzhenren.display.physique",
                    Component.translatable(extremePhysique.getTranslationKey())));
        }
        return value;
    }

    public static MutableComponent formatGuLine(Rank rank, GuPath path, String kindKey) {
        return Component.translatable("guzhenren.display.gu_line",
                Component.translatable(rank.getTranslationKey()),
                Component.translatable(path.getTranslationKey()),
                Component.translatable(kindKey));
    }

    public static MutableComponent formatWild(Component name) {
        return Component.translatable("guzhenren.display.wild", name);
    }

    public static MutableComponent formatVital(Component name) {
        return Component.translatable("guzhenren.display.vital", name);
    }

    public static MutableComponent formatPath(@Nullable GuPath path) {
        return path == null ? formatNone() : Component.translatable(path.getTranslationKey());
    }

    public static Component formatApertureName(int number) {
        return Component.translatable("guzhenren.display.aperture_" + number);
    }

    public static MutableComponent formatPathLine(GuPath path, GuAttainment attainment, PathMarks marks) {
        MutableComponent line = formatPath(path);
        if (attainment != GuAttainment.NONE) {
            line.append(" ").append(Component.translatable(attainment.getTranslationKey()));
        }
        if (!marks.isEmpty()) line.append(" ").append(formatMarks(marks));
        return line;
    }

    public static MutableComponent formatMarks(PathMarks marks) {
        String whole = formatCount(marks.marks());
        String points = formatCount(marks.points());
        if (marks.marks() == 0L) return Component.translatable("guzhenren.display.marks.points", points);
        if (marks.points() == 0L) return Component.translatable("guzhenren.display.marks.marks", whole);
        return Component.translatable("guzhenren.display.marks.both", whole, points);
    }

    private static String formatCount(long value) { return String.format(Locale.ROOT, "%,d", value); }

    public static MutableComponent formatEffectTime(long secondsLeft) {
        return secondsLeft == PathEffect.PERMANENT ? Component.translatable("guzhenren.display.effect_permanent")
                : Component.literal(formatClock(secondsLeft));
    }

    public static MutableComponent formatLifespan(double lifespan, double age) {
        return Component.translatable("guzhenren.display.lifespan", formatYears(lifespan), formatYears(age));
    }

    public static MutableComponent formatHudLifespan(BodyData body) {
        return Component.translatable("guzhenren.display.lifespan",
                String.format(Locale.ROOT, "%.1f", body.getLifespanYears()),
                String.format(Locale.ROOT, "%.0f", body.ageYears()));
    }

    public static long getCountdownSeconds(long remainingTicks) {
        return (Math.max(0L, remainingTicks) + Ticks.SECOND - 1) / Ticks.SECOND;
    }

    public static String formatClock(long seconds) {
        return String.format(Locale.ROOT, "%02d:%02d", seconds / 60L, seconds % 60L);
    }

    private static String formatYears(double value) { return String.format(Locale.ROOT, "%.2f", value); }

    public static String formatPool(long current, long max) { return current + "/" + max; }

    public static String formatAttackBonus(double bonus) { return "+" + bonus; }

    public static String formatCritChance(float chance) { return "+" + Math.round(chance * 100.0F) + "%"; }

    public static MutableComponent formatTimeRateUp(int rate) {
        return Component.translatable("guzhenren.display.time_rate_up", rate);
    }

    public static MutableComponent formatStrengthReading(InfoModel.StrengthReading reading) {
        return switch (reading) {
            case InfoModel.BeastReading beast -> formatBeastReading(beast);
            case InfoModel.HumanReading human -> Component.translatable(human.unit() == HumanStrength.JUN
                    ? "guzhenren.display.strength.jun_reading" : "guzhenren.display.strength.jin_reading",
                    formatStrengthNumber(human.count()));
        };
    }

    private static MutableComponent formatBeastReading(InfoModel.BeastReading beast) {
        MutableComponent name = formatBeastStrength(beast.family(), beast.count(), beast.innate());
        if (beast.secondsLeft() == PathEffect.PERMANENT) return name;
        return name.append(" ").append(formatEffectTime(beast.secondsLeft()));
    }

    public static MutableComponent formatBeastStrength(BeastStrengthFamily family, int count,
                                                       @Nullable BeastStrength innate) {
        if (innate != null) {
            return Component.translatable("guzhenren.display.strength.innate_reading",
                    Component.translatable(innate.getTranslationKey()));
        }
        return Component.translatable("guzhenren.display.strength.beast_reading",
                Component.translatable("guzhenren.display.strength.beast_number." + count),
                Component.translatable(family.getTranslationKey()));
    }

    public static MutableComponent formatStrengthLabel(MutableComponent branchName, int totalJin) {
        if (totalJin <= 0) return branchName;
        return branchName.append(Component.translatable("guzhenren.display.strength.jin_total", totalJin));
    }

    private static Component formatStrengthNumber(int count) {
        if (count < 100) return formatBelowHundred(count, false);
        Component h = Component.translatable("guzhenren.display.strength.number_hundreds." + (count / 100));
        int rest = count % 100;
        if (rest == 0) return h;
        String join = rest < 10
                ? "guzhenren.display.strength.number_join_zero"
                : "guzhenren.display.strength.number_join";
        return Component.translatable(join, h, formatBelowHundred(rest, true));
    }

    private static Component formatBelowHundred(int count, boolean led) {
        int tens = count / 10;
        int units = count % 10;
        Component t = tens == 0 ? null : Component.translatable(led && tens == 1
                ? "guzhenren.display.strength.number_tens_led.1"
                : "guzhenren.display.strength.number_tens." + tens);
        Component u = units > 0 ? Component.translatable("guzhenren.display.strength.number_units." + units) : null;
        if (t != null && u != null) return Component.translatable("guzhenren.display.strength.number_join", t, u);
        return t != null ? t : u;
    }
}
