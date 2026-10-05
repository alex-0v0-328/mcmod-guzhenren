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
import net.alex.guzhenren.gameplay.path.GuAttainment;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.path.PathEntry;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

/**
 * Every phrase the HUD, the B panel and the info command share.
 *
 * <p>The single source for shared wording: a phrase built at a call site is one that will eventually
 * word the same fact differently somewhere else. If two surfaces say a thing, the sentence belongs in
 * this file. Brackets ({@code [无]}, {@code [太日阳莽体]}) are baked into the lang value, never assembled here.
 *
 * <p>⚠ Two decimals on lifespan, not one ({@link #lifespan(double, double)}): a hundredth of a year is
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

    public static MutableComponent realm(Aperture aperture) {
        return Component.translatable("guzhenren.display.realm",
                Component.translatable(aperture.rank().getTranslationKey()),
                Component.translatable(aperture.stage().getTranslationKey()));
    }

    public static MutableComponent realmTitle(Aperture aperture) {
        MutableComponent title = Component.translatable(Title.fromRank(aperture.rank()).getTranslationKey());
        if (aperture.rank() == Rank.NONE) return title;

        return Component.translatable("guzhenren.display.realm_title", realm(aperture), title);
    }

    public static MutableComponent talent(Aperture aperture) {
        return Component.translatable(aperture.talent().getTranslationKey());
    }

    public static MutableComponent hudHeader(Aperture aperture, BodyData body) {
        return realmTitle(aperture).append(GAP).append(hudAptitude(aperture, body));
    }

    public static MutableComponent hudAptitude(Aperture aperture, BodyData body) {
        if (aperture.talent() == Talent.NONE) return talent(aperture);

        Component detail = aperture.talent() == Talent.TEN_EXTREMES && body.isExtreme()
                ? Component.translatable(body.extremePhysique().getTranslationKey())
                : baseFraction(aperture.baseEssence());
        return Component.translatable("guzhenren.display.aptitude_line", talent(aperture), detail);
    }

    public static MutableComponent physique(@Nullable Physique physique, ExtremePhysique extremePhysique) {
        return Component.translatable("guzhenren.display.physique_line",
                physiqueValue(physique, extremePhysique));
    }

    public static MutableComponent physiqueValue(@Nullable Physique physique, ExtremePhysique extremePhysique) {
        if (physique == null) return Component.translatable("guzhenren.display.none");

        MutableComponent value = Component.translatable(physique.getTranslationKey());
        if (physique == Physique.EXTREME) {
            value.append(" ").append(Component.translatable("guzhenren.display.physique",
                    Component.translatable(extremePhysique.getTranslationKey())));
        }
        return value;
    }

    public static MutableComponent guLine(Rank rank, GuPath path, String kindKey) {
        return Component.translatable("guzhenren.display.gu_line",
                Component.translatable(rank.getTranslationKey()),
                Component.translatable(path.getTranslationKey()),
                Component.translatable(kindKey));
    }

    public static MutableComponent wild(Component name) {
        return Component.translatable("guzhenren.display.wild", name);
    }

    public static MutableComponent vital(Component name) {
        return Component.translatable("guzhenren.display.vital", name);
    }

    public static MutableComponent path(@Nullable GuPath path) {
        return path == null
                ? Component.translatable("guzhenren.display.none")
                : Component.translatable(path.getTranslationKey());
    }

    public static Component apertureName(int number) {
        return Component.translatable("guzhenren.display.aperture_" + number);
    }

    public static MutableComponent pathLine(GuPath path, PathEntry entry) {
        MutableComponent line = path(path);
        if (entry.attainment() != GuAttainment.NONE) {
            line.append(" ").append(Component.translatable(entry.attainment().getTranslationKey()));
        }
        if (entry.markTotal() > 0L) {
            line.append(Component.translatable("guzhenren.display.path_marks", entry.markTotal()));
        }
        return line;
    }

    public static MutableComponent lifespan(double lifespan, double age) {
        return Component.translatable("guzhenren.display.lifespan", years(lifespan), years(age));
    }

    public static MutableComponent hudLifespan(BodyData body) {
        return Component.translatable("guzhenren.display.lifespan",
                String.format(Locale.ROOT, "%.1f", body.lifespanYears()),
                String.format(Locale.ROOT, "%.0f", body.ageYears()));
    }

    public static String countdown(long remainingTicks) {
        long seconds = (Math.max(0L, remainingTicks) + Ticks.SECOND - 1) / Ticks.SECOND;
        return String.format(Locale.ROOT, "%02d:%02d", seconds / 60L, seconds % 60L);
    }

    private static String years(double value) { return String.format(Locale.ROOT, "%.2f", value); }

    public static String pool(long current, long max) { return current + "/" + max; }

    public static String attackBonus(double bonus) { return "+" + bonus; }

    public static MutableComponent timeRateUp(int rate) {
        return Component.translatable("guzhenren.display.time_rate_up", rate);
    }

    public static MutableComponent beastStrengthLine(PathStrengthData data) {
        MutableComponent line = Component.empty();
        data.beastReadings().forEach((family, reading) -> line.append(beastReading(family, reading)));
        return line;
    }

    private static Component beastReading(BeastStrengthFamily family, int reading) {
        return Component.translatable("guzhenren.display.strength.beast_reading",
                Component.translatable("guzhenren.display.strength.beast_number." + reading),
                Component.translatable(family.getTranslationKey()));
    }

    public static MutableComponent humanStrengthLine(PathStrengthData data) {
        MutableComponent line = Component.empty();
        appendFamily(line, "guzhenren.display.strength.jun_reading", data.junReading());
        appendFamily(line, "guzhenren.display.strength.jin_reading", data.jinReading());
        return line;
    }

    public static MutableComponent strengthLabel(MutableComponent branchName, int totalJin) {
        if (totalJin <= 0) return branchName;
        return branchName.append(Component.translatable("guzhenren.display.strength.jin_total", totalJin));
    }

    private static void appendFamily(MutableComponent line, String readingKey, int reading) {
        if (reading <= 0) return;
        line.append(Component.translatable(readingKey, strengthNumber(reading)));
    }

    private static Component strengthNumber(int count) {
        if (count < 100) return belowHundred(count, false);
        Component h = Component.translatable("guzhenren.display.strength.num_hundreds." + (count / 100));
        int rest = count % 100;
        if (rest == 0) return h;
        String join = rest < 10 ? "guzhenren.display.strength.num_join_zero" : "guzhenren.display.strength.num_join";
        return Component.translatable(join, h, belowHundred(rest, true));
    }

    private static Component belowHundred(int count, boolean led) {
        int tens = count / 10;
        int units = count % 10;
        Component t = tens == 0 ? null : Component.translatable(led && tens == 1
                ? "guzhenren.display.strength.num_tens_led.1"
                : "guzhenren.display.strength.num_tens." + tens);
        Component u = units > 0 ? Component.translatable("guzhenren.display.strength.num_units." + units) : null;
        if (t != null && u != null) return Component.translatable("guzhenren.display.strength.num_join", t, u);
        return t != null ? t : u;
    }

    public static Component baseFraction(int base) {
        if (base >= 100) return Component.translatable("guzhenren.display.base_full");
        Component tens = Component.translatable("guzhenren.display.base_tens." + (base / 10));
        int units = base % 10;
        return units == 0
                ? Component.translatable("guzhenren.display.base_round", tens)
                : Component.translatable("guzhenren.display.base_fraction", tens,
                Component.translatable("guzhenren.display.base_units." + units));
    }
}
