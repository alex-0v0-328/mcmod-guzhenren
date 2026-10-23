package net.alex.guzhenren.client.screen;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.display.InfoModel;
import net.alex.guzhenren.display.ModDisplayText;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

/**
 * The B panel's text for each {@link InfoModel} entry: a label on the left, an optional value on the right,
 * and, for the secondary-path row, a click that opens the {@link PathPicker}. A header row has no value and
 * takes the tab's accent colour; the mind header returns {@code null} because its tab title already says it.
 *
 * <p>{@link #getRows} turns a tab's model into its rows, two path rows sharing one line; {@link #getApertureGroups}
 * splits the aperture tab's model into one group per aperture, blanks dropped, for the two-column layout.
 *
 * <p>{@link #draw} is one exhaustive switch over the sealed {@code InfoModel.Entry}, past 40 lines on
 * purpose: a new row does not compile until it has its line here and in {@code /gzr info}. ⚠ The model owns
 * the rows and their order, never the wording -- the panel puts the label in one column and the value in
 * another, the command bakes both into one key.
 *
 * @author Alex
 * @version 1.0.0
 * @see PlayerInfoScreen
 * @see InfoModel
 * @since 1.0.0
 */

final class InfoRowRenderer {

    private InfoRowRenderer() {}

    static List<ScreenRow> getRows(List<InfoModel.Row> model) {
        List<ScreenRow> rows = new ArrayList<>(model.size());
        for (int i = 0; i < model.size(); i++) {
            InfoModel.Row row = model.get(i);
            ScreenRow drawn = draw(row.indent(), row.entry());
            if (drawn == null) continue;

            ScreenRow partner = row.entry() instanceof InfoModel.PathRow ? getPathPartner(model, i + 1) : null;
            if (partner != null) {
                rows.add(new ScreenRow(row.indent(), drawn.label(), partner.label()));
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
        // ⚠ Java 17 has no pattern switch, and the Forge 1.20.1 port compiles this file for Java 17: the compiler no
        // longer checks this chain covers the sealed InfoModel.Entry, so a new Entry record needs its branch here.
        if (entry instanceof InfoModel.ApertureIndex e) {
            return new ScreenRow(indent, ModDisplayText.apertureName(e.number()), null);
        }
        if (entry instanceof InfoModel.Blank) return new ScreenRow(indent, Component.empty(), null);
        if (entry instanceof InfoModel.Realm e) {
            return new ScreenRow(indent, label("realm"), ModDisplayText.realmTitle(e.aperture()));
        }
        if (entry instanceof InfoModel.Status e) {
            return new ScreenRow(indent, label("aperture_status"),
                    name(e.status().getTranslationKey()));
        }
        if (entry instanceof InfoModel.TalentRow e) {
            return new ScreenRow(indent, label("talent"),
                    ModDisplayText.talentLine(e.aperture(), e.awakened()));
        }
        if (entry instanceof InfoModel.Essence e) {
            return new ScreenRow(indent, label("essence"), Component.literal(
                    ModDisplayText.pool(e.aperture().currentEssence(), e.aperture().maxEssence())));
        }
        if (entry instanceof InfoModel.Distilled e) {
            return new ScreenRow(indent, label("distilled"), Component.literal(
                    ModDisplayText.pool(e.aperture().distilledEssence(), e.aperture().maxEssence())));
        }
        if (entry instanceof InfoModel.Pressure e) {
            return new ScreenRow(indent,
                    Component.translatable("guzhenren.screen.label.aperture_pressure"), Component.literal(
                    e.aperture().pressure() + "%"));
        }
        if (entry instanceof InfoModel.PathChoice e) {
            return e.primary()
                    ? new ScreenRow(indent, label("primary_path"), ModDisplayText.path(e.path()))
                    : new ScreenRow(indent, label("secondary_path"),
                    ModDisplayText.detail(Component.translatable("guzhenren.screen.pick.hint"))
                            .append(e.path() == null
                                    ? ModDisplayText.none().withStyle(ChatFormatting.DARK_GRAY)
                                    : ModDisplayText.detail(ModDisplayText.path(e.path()))),
                    new Click(e.aperture()));
        }
        if (entry instanceof InfoModel.PhysiqueRow e) {
            return new ScreenRow(indent, label("physique"),
                    ModDisplayText.physiqueValue(e.physique(), e.extremePhysique()));
        }
        if (entry instanceof InfoModel.RaceRow e) {
            return new ScreenRow(indent, label("race"), name(e.race().getTranslationKey()));
        }
        if (entry instanceof InfoModel.Soul e) {
            return new ScreenRow(indent, label("soul"),
                    Component.literal(ModDisplayText.pool(e.soul().currentSoul(), e.soul().maxSoul()))
                            .append(ModDisplayText.detail(name(e.soul().tier().getTranslationKey()))));
        }
        if (entry instanceof InfoModel.Lifespan e) {
            return new ScreenRow(indent, label("lifespan"),
                    ModDisplayText.lifespan(e.lifespan(), e.age()));
        }
        if (entry instanceof InfoModel.PathsHeader e) {
            return new ScreenRow(indent, label("paths"), e.empty() ? ModDisplayText.none() : null);
        }
        if (entry instanceof InfoModel.PathRow e) {
            return new ScreenRow(indent, ModDisplayText.pathLine(e.path(), e.entry()), Component.empty());
        }
        if (entry instanceof InfoModel.QiPathAchieveHeader) {
            return new ScreenRow(indent, label("qi_path_achieve"), null);
        }
        if (entry instanceof InfoModel.QiKindRow e) {
            return new ScreenRow(indent, name(e.kind().getTranslationKey()),
                    Component.literal(String.valueOf(e.amount())));
        }
        if (entry instanceof InfoModel.TimePathAchieveHeader) {
            return new ScreenRow(indent, label("time_path_achieve"), null);
        }
        if (entry instanceof InfoModel.TimeRateUpRow e) {
            return new ScreenRow(indent, label("time_rate_up"),
                    ModDisplayText.timeRateUp(e.rate()));
        }
        if (entry instanceof InfoModel.StrengthPathAchieveHeader) {
            return new ScreenRow(indent, label("strength_path_achieve"), null);
        }
        if (entry instanceof InfoModel.StrengthPathBranchRow e) {
            return new ScreenRow(indent,
                    ModDisplayText.strengthLabel(name(e.branch().getTranslationKey()), e.totalJin()), e.reading());
        }
        if (entry instanceof InfoModel.CapacityRow e) {
            return new ScreenRow(indent,
                    Component.translatable("guzhenren.screen.label.body_capacity"),
                    Component.translatable("guzhenren.screen.capacity", e.usable(), e.total()));
        }
        if (entry instanceof InfoModel.AttackRow e) {
            return new ScreenRow(indent, label("attack"),
                    Component.literal(ModDisplayText.attackBonus(e.bonus())));
        }
        if (entry instanceof InfoModel.WisdomPathAchieveHeader) {
            return new ScreenRow(indent, label("wisdom_path_achieve"), null);
        }
        if (entry instanceof InfoModel.ThoughtTagRow e) {
            return new ScreenRow(indent, name(e.tag().getTranslationKey()),
                    Component.literal(String.valueOf(e.amount())));
        }
        if (entry instanceof InfoModel.BrillianceRow e) {
            return new ScreenRow(indent, label("brilliance"),
                    name(e.brilliance().getTranslationKey()).append(ModDisplayText.detail(Component.translatable(
                            "guzhenren.display.brilliance_rate", e.brilliance().getThoughtsPerSecond()))));
        }
        if (entry instanceof InfoModel.MindHeader) return null;
        if (entry instanceof InfoModel.MindRow e) {
            return new ScreenRow(indent, name(e.type().getTranslationKey()),
                    Component.literal(ModDisplayText.pool(e.pool().current(), e.pool().max())));
        }
        throw new IllegalArgumentException("unhandled info entry: " + entry);
    }

    private static MutableComponent name(String key) { return Component.translatable(key); }

    private static Component label(String name) { return Component.translatable("guzhenren.screen.label." + name); }

    record ScreenRow(int indent, Component label, @Nullable Component value, @Nullable Click click) {

        ScreenRow(int indent, Component label, @Nullable Component value) { this(indent, label, value, null); }
    }

    record Click(int aperture) {}
}
