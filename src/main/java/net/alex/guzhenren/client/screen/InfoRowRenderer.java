package net.alex.guzhenren.client.screen;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.display.InfoModel;
import net.alex.guzhenren.display.ModDisplayText;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * The B panel's text for each {@link InfoModel} entry: a label on the left, an optional value on the right,
 * and, for the secondary-path row, a click that opens the {@link PathPicker}. A header row has no value and
 * takes the tab's accent colour; the mind header returns {@code null} because its tab title already says it.
 *
 * <p>{@link #getRows} turns a tab's model into its rows, two path rows sharing one line; {@link #getApertureGroups}
 * splits the aperture tab into one group per aperture, blanks dropped, for the two-column layout.
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

    static List<List<ScreenRow>> getApertureGroups(Player player) {
        List<List<ScreenRow>> groups = new ArrayList<>();
        List<ScreenRow> current = null;
        for (InfoModel.Row modelRow : InfoModel.aperture(player)) {
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
            case InfoModel.ApertureIndex e -> new ScreenRow(indent, ModDisplayText.apertureName(e.number()), null);
            case InfoModel.Blank ignored -> new ScreenRow(indent, Component.empty(), null);
            case InfoModel.Realm e -> new ScreenRow(indent, label("realm"), ModDisplayText.realmTitle(e.aperture()));
            case InfoModel.Status e -> new ScreenRow(indent, label("aperture_status"),
                    name(e.status().getTranslationKey()));
            case InfoModel.TalentRow e -> new ScreenRow(indent, label("talent"), talent(e));
            case InfoModel.Essence e -> new ScreenRow(indent, label("essence"), Component.literal(
                    ModDisplayText.pool(e.aperture().currentEssence(), e.aperture().maxEssence())));
            case InfoModel.Distilled e -> new ScreenRow(indent, label("distilled"), Component.literal(
                    ModDisplayText.pool(e.aperture().distilledEssence(), e.aperture().maxEssence())));
            case InfoModel.Pressure e -> new ScreenRow(indent,
                    Component.translatable("guzhenren.screen.label.aperture_pressure"), Component.literal(
                    e.aperture().pressure() + "%"));

            case InfoModel.PathChoice e -> e.primary()
                    ? new ScreenRow(indent, label("primary_path"), ModDisplayText.path(e.path()))
                    : new ScreenRow(indent, label("secondary_path"),
                    detail(Component.translatable("guzhenren.screen.pick.hint")).copy().append(e.path() == null
                            ? none().withStyle(ChatFormatting.DARK_GRAY)
                            : detail(ModDisplayText.path(e.path()))),
                    new Click(e.aperture()));

            case InfoModel.PhysiqueRow e -> new ScreenRow(indent, label("physique"),
                    ModDisplayText.physiqueValue(e.physique(), e.extremePhysique()));
            case InfoModel.RaceRow e -> new ScreenRow(indent, label("race"), name(e.race().getTranslationKey()));
            case InfoModel.Soul e -> new ScreenRow(indent, label("soul"),
                    Component.literal(ModDisplayText.pool(e.soul().currentSoul(), e.soul().maxSoul()))
                            .append(detail(name(e.soul().tier().getTranslationKey()))));
            case InfoModel.Lifespan e -> new ScreenRow(indent, label("lifespan"),
                    ModDisplayText.lifespan(e.lifespan(), e.age()));
            case InfoModel.PathsHeader e -> new ScreenRow(indent, label("paths"), e.empty() ? none() : null);
            case InfoModel.PathRow e ->
                    new ScreenRow(indent, ModDisplayText.pathLine(e.path(), e.entry()), Component.empty());
            case InfoModel.QiPathAchieveHeader ignored -> new ScreenRow(indent, label("qi_path_achieve"), null);
            case InfoModel.QiKindRow e -> new ScreenRow(indent, name(e.kind().getTranslationKey()),
                    Component.literal(String.valueOf(e.amount())));
            case InfoModel.TimePathAchieveHeader ignored -> new ScreenRow(indent, label("time_path_achieve"), null);
            case InfoModel.TimeRateUpRow e -> new ScreenRow(indent, label("time_rate_up"),
                    ModDisplayText.timeRateUp(e.rate()));
            case InfoModel.StrengthPathAchieveHeader ignored ->
                    new ScreenRow(indent, label("strength_path_achieve"), null);
            case InfoModel.StrengthPathBranchRow e -> new ScreenRow(indent,
                    ModDisplayText.strengthLabel(name(e.branch().getTranslationKey()), e.totalJin()), e.reading());
            case InfoModel.CapacityRow e -> new ScreenRow(indent,
                    Component.translatable("guzhenren.screen.label.body_capacity"),
                    Component.translatable("guzhenren.screen.capacity", e.usable(), e.total()));
            case InfoModel.AttackRow e -> new ScreenRow(indent, label("attack"),
                    Component.literal(ModDisplayText.attackBonus(e.bonus())));
            case InfoModel.WisdomPathAchieveHeader ignored -> new ScreenRow(indent, label("wisdom_path_achieve"), null);
            case InfoModel.ThoughtTagRow e -> new ScreenRow(indent, name(e.tag().getTranslationKey()),
                    Component.literal(String.valueOf(e.amount())));

            case InfoModel.BrillianceRow e -> new ScreenRow(indent, label("brilliance"),
                    name(e.brilliance().getTranslationKey()).append(detail(Component.translatable(
                            "guzhenren.display.brilliance_rate", e.brilliance().getThoughtsPerSecond()))));
            case InfoModel.MindHeader ignored -> null;
            case InfoModel.MindRow e -> new ScreenRow(indent, name(e.type().getTranslationKey()),
                    Component.literal(ModDisplayText.pool(e.pool().current(), e.pool().max())));
        };
    }

    private static MutableComponent talent(InfoModel.TalentRow event) {
        MutableComponent talent = ModDisplayText.talent(event.aperture());
        if (event.awakened()) talent.append(detail(ModDisplayText.baseFraction(event.aperture().baseEssence())));
        return talent;
    }

    private static MutableComponent name(String key) { return Component.translatable(key); }

    private static Component label(String name) { return Component.translatable("guzhenren.screen.label." + name); }

    private static MutableComponent none() { return Component.translatable("guzhenren.display.none"); }

    private static Component detail(Component value) {
        return Component.translatable("guzhenren.display.detail", value).withStyle(ChatFormatting.DARK_GRAY);
    }

    record ScreenRow(int indent, Component label, @Nullable Component value, @Nullable Click click) {

        ScreenRow(int indent, Component label, @Nullable Component value) { this(indent, label, value, null); }
    }

    record Click(int aperture) {}
}
