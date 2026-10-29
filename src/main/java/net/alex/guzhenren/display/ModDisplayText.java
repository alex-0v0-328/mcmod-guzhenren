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
import net.alex.guzhenren.gameplay.path.DaoMarks;
import net.alex.guzhenren.gameplay.path.GuAttainment;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.path.PathEffect;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

/**
 * Every phrase the HUD, the B panel and the Gu tooltips share.
 *
 * <p>The single source for shared wording: a phrase built at a call site is one that will eventually
 * word the same fact differently somewhere else. If two surfaces say a thing, the sentence belongs in
 * this file. Brackets ({@code [无]}, {@code [太日阳莽体]}) are baked into the lang value, never assembled here:
 * {@link #formatNone} is the {@code [无]}, {@link #formatDetail} wraps a muted aside, {@link #formatTalentLine} is a
 * talent with its base fraction. {@link #getCountdownSeconds} is the whole second a countdown shows, for a caller
 * that caches the text per second. {@link #formatMarks} words whole marks [条] and strands [丝] as two counts side
 * by side with thousands separators, never converting one into the other, and drops a count that is zero; a caller
 * hides the line when both are. {@link #formatEffectTime} takes the whole
 * seconds a path effect has left, or {@code PathEffect.PERMANENT}.
 *
 * <p>⚠ Two decimals on lifespan, not one ({@link #formatLifespan(double, double)}): a hundredth of a year is
 * twelve real seconds at ordinary speed, the coarsest step the eye still reads as movement; a tenth
 * stands still for two minutes.
 *
 * @author Alex
 * @version 1.0.0
 * @see InfoModel
 * @since 1.0.0
 */

public final class ModDisplayText {

    private ModDisplayText() {}

    private static final String GAP = "  ";

    public static MutableComponent formatRealm(Aperture aperture) {
        return Component.translatable("guzhenren.display.realm",
                Component.translatable(aperture.rank().getTranslationKey()),
                Component.translatable(aperture.stage().getTranslationKey()));
    }

    public static MutableComponent formatRealmTitle(Aperture aperture) {
        MutableComponent title = Component.translatable(Title.fromRank(aperture.rank()).getTranslationKey());
        if (aperture.rank() == Rank.NONE) return title;

        return Component.translatable("guzhenren.display.realm_title", formatRealm(aperture), title);
    }

    public static MutableComponent formatTalent(Aperture aperture) {
        return Component.translatable(aperture.getTalent().getTranslationKey());
    }

    public static MutableComponent formatTalentLine(Aperture aperture, boolean awakened) {
        MutableComponent line = formatTalent(aperture);
        if (awakened) line.append(formatDetail(formatBaseFraction(aperture.baseEssence())));
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
                : formatBaseFraction(aperture.baseEssence());
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

    public static MutableComponent formatPathLine(GuPath path, GuAttainment attainment, DaoMarks marks) {
        MutableComponent line = formatPath(path);
        if (attainment != GuAttainment.NONE) {
            line.append(" ").append(Component.translatable(attainment.getTranslationKey()));
        }
        if (!marks.isEmpty()) line.append(" ").append(formatMarks(marks));
        return line;
    }

    public static MutableComponent formatMarks(DaoMarks marks) {
        String whole = formatCount(marks.marks());
        String strands = formatCount(marks.strands());
        if (marks.marks() == 0L) return Component.translatable("guzhenren.display.marks.strands", strands);
        if (marks.strands() == 0L) return Component.translatable("guzhenren.display.marks.marks", whole);
        return Component.translatable("guzhenren.display.marks.both", whole, strands);
    }

    public static String formatCount(long value) { return String.format(Locale.ROOT, "%,d", value); }

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

    public static String formatCountdown(long remainingTicks) {
        return formatClock(getCountdownSeconds(remainingTicks));
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

    public static MutableComponent formatTimeRateUp(int rate) {
        return Component.translatable("guzhenren.display.time_rate_up", rate);
    }

    public static MutableComponent formatBeastStrengthLine(PathStrengthData data) {
        MutableComponent line = Component.empty();
        data.getBeastReadings().forEach((family, reading) -> line.append(formatBeastReading(family, reading)));
        return line;
    }

    private static Component formatBeastReading(BeastStrengthFamily family, int reading) {
        return Component.translatable("guzhenren.display.strength.beast_reading",
                Component.translatable("guzhenren.display.strength.beast_number." + reading),
                Component.translatable(family.getTranslationKey()));
    }

    public static MutableComponent formatHumanStrengthLine(PathStrengthData data) {
        MutableComponent line = Component.empty();
        appendFamily(line, "guzhenren.display.strength.jun_reading", data.getJunReading());
        appendFamily(line, "guzhenren.display.strength.jin_reading", data.getJinReading());
        return line;
    }

    public static MutableComponent formatStrengthLabel(MutableComponent branchName, int totalJin) {
        if (totalJin <= 0) return branchName;
        return branchName.append(Component.translatable("guzhenren.display.strength.jin_total", totalJin));
    }

    private static void appendFamily(MutableComponent line, String readingKey, int reading) {
        if (reading <= 0) return;
        line.append(Component.translatable(readingKey, formatStrengthNumber(reading)));
    }

    private static Component formatStrengthNumber(int count) {
        if (count < 100) return formatBelowHundred(count, false);
        Component h = Component.translatable("guzhenren.display.strength.num_hundreds." + (count / 100));
        int rest = count % 100;
        if (rest == 0) return h;
        String join = rest < 10 ? "guzhenren.display.strength.num_join_zero" : "guzhenren.display.strength.num_join";
        return Component.translatable(join, h, formatBelowHundred(rest, true));
    }

    private static Component formatBelowHundred(int count, boolean led) {
        int tens = count / 10;
        int units = count % 10;
        Component t = tens == 0 ? null : Component.translatable(led && tens == 1
                ? "guzhenren.display.strength.num_tens_led.1"
                : "guzhenren.display.strength.num_tens." + tens);
        Component u = units > 0 ? Component.translatable("guzhenren.display.strength.num_units." + units) : null;
        if (t != null && u != null) return Component.translatable("guzhenren.display.strength.num_join", t, u);
        return t != null ? t : u;
    }

    public static Component formatBaseFraction(int base) {
        if (base >= 100) return Component.translatable("guzhenren.display.base_full");
        Component tens = Component.translatable("guzhenren.display.base_tens." + (base / 10));
        int units = base % 10;
        return units == 0
                ? Component.translatable("guzhenren.display.base_round", tens)
                : Component.translatable("guzhenren.display.base_fraction", tens,
                Component.translatable("guzhenren.display.base_units." + units));
    }
}
