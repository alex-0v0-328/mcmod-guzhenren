package net.alex.guzhenren.client.screen;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.display.InfoModel;
import net.alex.guzhenren.display.ModDisplayText;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

/**
 * The B panel's text for each {@link InfoModel} entry: a label on the left, an optional value on the right,
 * and, for the secondary-path row, a click that opens the {@link PathPicker}. A header row has no value and
 * takes the tab's accent color; the mind header returns {@code null} because its tab title already says it.
 *
 * <p>{@link #getRows} turns a tab's model into its rows, two path rows sharing one line ({@code paired}), and a strength
 * reading row into {@code cells}; {@link #getApertureGroups} splits the aperture tab's model into one group per
 * aperture, blanks dropped, for the two-column layout.
 *
 * <p>{@link #getBlock} measures a tab's rows, or one aperture column, into a {@link RowBlock}: values start one column
 * past the widest indented label that carries a value, the second path of a pair one column past the widest first
 * path, and reading cells are as wide as the widest cell; each column adds a {@code VALUE_GAP} of 16 px. Widths are
 * measured from the rows alone, never the panel, so a wide panel keeps a value next to its label.
 *
 * <p>{@link #draw} switches over the sealed entry with no default, one case per row kind: a new row does not compile
 * until it has its line here. ⚠ The model owns the rows and their order, never the
 * wording -- the panel puts the label in one column and the value in another.
 *
 * @author Alex
 * @version 1.0.0
 * @see PlayerInfoScreen
 * @see InfoModel
 * @since 1.0.0
 */

final class InfoRowRenderer {

    private static final int VALUE_GAP = 16;

    private InfoRowRenderer() {}

    static RowBlock getBlock(Font font, List<ScreenRow> rows) {
        int label = 0;
        int pair = 0;
        int cell = 0;
        for (ScreenRow row : rows) {
            int width = row.indent() + font.width(row.label());
            if (row.paired()) {
                pair = Math.max(pair, width);
            } else if (row.value() != null) {
                label = Math.max(label, width);
            }
            for (Component c : row.cells()) cell = Math.max(cell, font.width(c));
        }
        return new RowBlock(rows, label + VALUE_GAP, pair + VALUE_GAP, cell + VALUE_GAP);
    }

    static List<ScreenRow> getRows(List<InfoModel.Row> model) {
        List<ScreenRow> rows = new ArrayList<>(model.size());
        for (int i = 0; i < model.size(); i++) {
            InfoModel.Row row = model.get(i);
            ScreenRow drawn = draw(row.indent(), row.entry());
            if (drawn == null) continue;

            ScreenRow partner = row.entry() instanceof InfoModel.PathRow ? getPathPartner(model, i + 1) : null;
            if (partner != null) {
                rows.add(new ScreenRow(row.indent(), drawn.label(), partner.label(), null, List.of(), true));
                i++;
            } else {
                rows.add(drawn);
            }
        }
        return rows;
    }

    static List<List<ScreenRow>> getApertureGroups(List<InfoModel.Row> model) {
        List<List<ScreenRow>> groups = new ArrayList<>();
        List<ScreenRow> current = null;
        for (InfoModel.Row modelRow : model) {
            if (modelRow.entry() instanceof InfoModel.Blank) continue;
            ScreenRow drawn = draw(modelRow.indent(), modelRow.entry());
            if (drawn == null) continue;
            if (modelRow.entry() instanceof InfoModel.ApertureIndex || current == null) {
                current = new ArrayList<>();
                groups.add(current);
            }
            current.add(drawn);
        }
        return groups;
    }

    private static @Nullable ScreenRow getPathPartner(List<InfoModel.Row> model, int index) {
        if (index >= model.size() || !(model.get(index).entry() instanceof InfoModel.PathRow second)) return null;
        return draw(model.get(index).indent(), second);
    }

    static @Nullable ScreenRow draw(int indent, InfoModel.Entry entry) {
        return switch (entry) {
            case InfoModel.ApertureIndex e ->
                    new ScreenRow(indent, ModDisplayText.formatApertureName(e.number()), null);
            case InfoModel.Blank ignored -> new ScreenRow(indent, Component.empty(), null);
            case InfoModel.Realm e ->
                    new ScreenRow(indent, formatLabel("realm"), ModDisplayText.formatRealmTitle(e.aperture()));
            case InfoModel.Status e ->
                    new ScreenRow(indent, formatLabel("aperture_status"), formatName(e.status().getTranslationKey()));
            case InfoModel.TalentRow e -> new ScreenRow(indent, formatLabel("talent"),
                    ModDisplayText.formatTalentLine(e.aperture(), e.awakened()));
            case InfoModel.Essence e -> new ScreenRow(indent, formatLabel("essence"), Component.literal(
                    ModDisplayText.formatPool(e.aperture().currentEssence(), e.aperture().getMaxEssence())));
            case InfoModel.Distilled e -> new ScreenRow(indent, formatLabel("distilled"), Component.literal(
                    ModDisplayText.formatPool(e.aperture().distilledEssence(), e.aperture().getMaxEssence())));
            case InfoModel.Pressure e -> new ScreenRow(indent, formatLabel("aperture_pressure"),
                    Component.literal(e.aperture().pressure() + "%"));
            case InfoModel.PathChoice e -> e.primary()
                    ? new ScreenRow(indent, formatLabel("primary_path"), ModDisplayText.formatPath(e.path()))
                    : new ScreenRow(indent, formatLabel("secondary_path"),
                    ModDisplayText.formatDetail(Component.translatable("guzhenren.screen.pick.hint"))
                            .append(e.path() == null
                                    ? ModDisplayText.formatNone().withStyle(ChatFormatting.DARK_GRAY)
                                    : ModDisplayText.formatDetail(ModDisplayText.formatPath(e.path()))),
                    new Click(e.aperture()));
            case InfoModel.PhysiqueRow e -> new ScreenRow(indent, formatLabel("physique"),
                    ModDisplayText.formatPhysiqueValue(e.physique(), e.extremePhysique()));
            case InfoModel.RaceRow e -> formatNamed(indent, "race", e.race().getTranslationKey());
            case InfoModel.TendonRow e -> formatNamed(indent, "tendon", e.tendon().getTranslationKey());
            case InfoModel.BoneRow e -> formatNamed(indent, "bone", e.bone().getTranslationKey());
            case InfoModel.SkinRow e -> formatNamed(indent, "skin", e.skin().getTranslationKey());
            case InfoModel.FleshRow e -> formatNamed(indent, "flesh", e.flesh().getTranslationKey());
            case InfoModel.Soul e -> new ScreenRow(indent, formatLabel("soul"),
                    Component.literal(ModDisplayText.formatPool(e.soul().currentSoul(), e.soul().maxSoul()))
                            .append(ModDisplayText.formatDetail(formatName(e.soul().getTier().getTranslationKey()))));
            case InfoModel.Lifespan e -> new ScreenRow(indent, formatLabel("lifespan"),
                    ModDisplayText.formatLifespan(e.lifespan(), e.age()));
            case InfoModel.PathsHeader e ->
                    new ScreenRow(indent, formatLabel("path_marks"), e.empty() ? ModDisplayText.formatNone() : null);
            case InfoModel.EffectsHeader e ->
                    new ScreenRow(indent, formatLabel("effects"), e.empty() ? ModDisplayText.formatNone() : null);
            case InfoModel.PathRow e -> new ScreenRow(indent,
                    ModDisplayText.formatPathLine(e.path(), e.attainment(), e.marks()), Component.empty(), null,
                    List.of(), true);
            case InfoModel.EffectCategoryHeader e ->
                    new ScreenRow(indent, formatName(e.category().getTranslationKey()), null);
            case InfoModel.EffectCarrierRow e ->
                    new ScreenRow(indent, formatName(e.carrier().getTranslationKey()), null);
            case InfoModel.PathEffectRow e ->
                    new ScreenRow(indent, e.effect().getName(), ModDisplayText.formatEffectTime(e.secondsLeft()));
            case InfoModel.TimePathAchieveHeader ignored ->
                    new ScreenRow(indent, formatLabel("time_path_achieve"), null);
            case InfoModel.TimeRateUpRow e ->
                    new ScreenRow(indent, formatLabel("time_rate_up"), ModDisplayText.formatTimeRateUp(e.rate()));
            case InfoModel.StrengthPathAchieveHeader ignored ->
                    new ScreenRow(indent, formatLabel("strength_path_achieve"), null);
            case InfoModel.StrengthPathBranchRow e -> new ScreenRow(indent,
                    ModDisplayText.formatStrengthLabel(formatName(e.branch().getTranslationKey()), e.totalJin()), null);
            case InfoModel.StrengthReadingRow e ->
                    new ScreenRow(indent, Component.empty(), null, null, formatReadings(e.readings()), false);
            case InfoModel.CapacityRow e -> new ScreenRow(indent, formatLabel("body_capacity"),
                    formatCapacity(e));
            case InfoModel.AttackRow e -> new ScreenRow(indent, formatLabel("attack"),
                    Component.literal(ModDisplayText.formatAttackBonus(e.bonus())));
            case InfoModel.WisdomPathAchieveHeader ignored ->
                    new ScreenRow(indent, formatLabel("wisdom_path_achieve"), null);
            case InfoModel.ThoughtTagRow e -> new ScreenRow(indent, formatName(e.tag().getTranslationKey()),
                    Component.literal(String.valueOf(e.amount())));
            case InfoModel.BrillianceRow e -> new ScreenRow(indent, formatLabel("brilliance"),
                    formatName(e.brilliance().getTranslationKey()).append(ModDisplayText.formatDetail(
                            Component.translatable("guzhenren.display.brilliance_rate",
                                    e.brilliance().getThoughtsPerSecond()))));
            case InfoModel.MindHeader ignored -> null;
            case InfoModel.MindRow e -> new ScreenRow(indent, formatName(e.type().getTranslationKey()),
                    Component.literal(ModDisplayText.formatPool(e.pool().current(), e.pool().max())));
        };
    }

    private static MutableComponent formatName(String key) { return Component.translatable(key); }

    private static Component formatCapacity(InfoModel.CapacityRow row) {
        MutableComponent text = Component.translatable("guzhenren.screen.capacity", row.usable(), row.total(),
                row.capacity(), row.normalCapacity());
        if (row.overload() <= 0) return text;
        return text.append(Component.translatable("guzhenren.screen.capacity.body_overload", row.overload()));
    }

    private static Component formatLabel(String name) {
        return Component.translatable("guzhenren.screen.label." + name);
    }

    private static ScreenRow formatNamed(int indent, String label, String key) {
        return new ScreenRow(indent, formatLabel(label), formatName(key));
    }

    private static List<Component> formatReadings(List<InfoModel.StrengthReading> readings) {
        List<Component> cells = new ArrayList<>(readings.size());
        for (InfoModel.StrengthReading reading : readings) cells.add(ModDisplayText.formatStrengthReading(reading));
        return cells;
    }

    record ScreenRow(int indent, Component label, @Nullable Component value, @Nullable Click click,
                     List<Component> cells, boolean paired) {

        ScreenRow(int indent, Component label, @Nullable Component value) { this(indent, label, value, null); }

        ScreenRow(int indent, Component label, @Nullable Component value, @Nullable Click click) {
            this(indent, label, value, click, List.of(), false);
        }
    }

    record RowBlock(List<ScreenRow> rows, int valueLeft, int pairLeft, int cellWidth) {

        static final RowBlock EMPTY = new RowBlock(List.of(), 0, 0, 0);
    }

    record Click(int aperture) {}
}
