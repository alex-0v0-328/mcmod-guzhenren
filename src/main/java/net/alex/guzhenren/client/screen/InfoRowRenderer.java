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
 * <p>{@link #draw} hands each entry to {@link EntryDrawer}, one {@code InfoModel.Visitor} method per row kind: a
 * new row does not compile until it has its line here. ⚠ The model owns the rows and their order, never the
 * wording -- the panel puts the label in one column and the value in another.
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
        return entry.accept(new EntryDrawer(indent));
    }

    private static MutableComponent formatName(String key) { return Component.translatable(key); }

    private static Component formatLabel(String name) {
        return Component.translatable("guzhenren.screen.label." + name);
    }

    private static final class EntryDrawer implements InfoModel.Visitor<ScreenRow> {

        private final int indent;

        EntryDrawer(int indent) { this.indent = indent; }

        @Override
        public ScreenRow visit(InfoModel.ApertureIndex entry) {
            return new ScreenRow(indent, ModDisplayText.formatApertureName(entry.number()), null);
        }

        @Override
        public ScreenRow visit(InfoModel.Blank entry) { return new ScreenRow(indent, Component.empty(), null); }

        @Override
        public ScreenRow visit(InfoModel.Realm entry) {
            return new ScreenRow(indent, formatLabel("realm"), ModDisplayText.formatRealmTitle(entry.aperture()));
        }

        @Override
        public ScreenRow visit(InfoModel.Status entry) {
            return new ScreenRow(indent, formatLabel("aperture_status"),
                    formatName(entry.status().getTranslationKey()));
        }

        @Override
        public ScreenRow visit(InfoModel.TalentRow entry) {
            return new ScreenRow(indent, formatLabel("talent"),
                    ModDisplayText.formatTalentLine(entry.aperture(), entry.awakened()));
        }

        @Override
        public ScreenRow visit(InfoModel.Essence entry) {
            return new ScreenRow(indent, formatLabel("essence"), Component.literal(
                    ModDisplayText.formatPool(entry.aperture().currentEssence(), entry.aperture().getMaxEssence())));
        }

        @Override
        public ScreenRow visit(InfoModel.Distilled entry) {
            return new ScreenRow(indent, formatLabel("distilled"), Component.literal(
                    ModDisplayText.formatPool(entry.aperture().distilledEssence(), entry.aperture().getMaxEssence())));
        }

        @Override
        public ScreenRow visit(InfoModel.Pressure entry) {
            return new ScreenRow(indent,
                    Component.translatable("guzhenren.screen.label.aperture_pressure"), Component.literal(
                    entry.aperture().pressure() + "%"));
        }

        @Override
        public ScreenRow visit(InfoModel.PathChoice entry) {
            return entry.primary()
                    ? new ScreenRow(indent, formatLabel("primary_path"), ModDisplayText.formatPath(entry.path()))
                    : new ScreenRow(indent, formatLabel("secondary_path"),
                    ModDisplayText.formatDetail(Component.translatable("guzhenren.screen.pick.hint"))
                            .append(entry.path() == null
                                    ? ModDisplayText.formatNone().withStyle(ChatFormatting.DARK_GRAY)
                                    : ModDisplayText.formatDetail(ModDisplayText.formatPath(entry.path()))),
                    new Click(entry.aperture()));
        }

        @Override
        public ScreenRow visit(InfoModel.PhysiqueRow entry) {
            return new ScreenRow(indent, formatLabel("physique"),
                    ModDisplayText.formatPhysiqueValue(entry.physique(), entry.extremePhysique()));
        }

        @Override
        public ScreenRow visit(InfoModel.RaceRow entry) {
            return new ScreenRow(indent, formatLabel("race"), formatName(entry.race().getTranslationKey()));
        }

        @Override
        public ScreenRow visit(InfoModel.TendonRow entry) {
            return new ScreenRow(indent, formatLabel("tendon"), formatName(entry.tendon().getTranslationKey()));
        }

        @Override
        public ScreenRow visit(InfoModel.BoneRow entry) {
            return new ScreenRow(indent, formatLabel("bone"), formatName(entry.bone().getTranslationKey()));
        }

        @Override
        public ScreenRow visit(InfoModel.SkinRow entry) {
            return new ScreenRow(indent, formatLabel("skin"), formatName(entry.skin().getTranslationKey()));
        }

        @Override
        public ScreenRow visit(InfoModel.FleshRow entry) {
            return new ScreenRow(indent, formatLabel("flesh"), formatName(entry.flesh().getTranslationKey()));
        }

        @Override
        public ScreenRow visit(InfoModel.Soul entry) {
            return new ScreenRow(indent, formatLabel("soul"),
                    Component.literal(ModDisplayText.formatPool(entry.soul().currentSoul(), entry.soul().maxSoul()))
                            .append(ModDisplayText.formatDetail(
                                    formatName(entry.soul().getTier().getTranslationKey()))));
        }

        @Override
        public ScreenRow visit(InfoModel.Lifespan entry) {
            return new ScreenRow(indent, formatLabel("lifespan"),
                    ModDisplayText.formatLifespan(entry.lifespan(), entry.age()));
        }

        @Override
        public ScreenRow visit(InfoModel.PathsHeader entry) {
            return new ScreenRow(indent, formatLabel("paths"), entry.empty() ? ModDisplayText.formatNone() : null);
        }

        @Override
        public ScreenRow visit(InfoModel.PathRow entry) {
            return new ScreenRow(indent,
                    ModDisplayText.formatPathLine(entry.path(), entry.attainment(), entry.marks()),
                    Component.empty());
        }

        @Override
        public ScreenRow visit(InfoModel.EffectCategoryHeader entry) {
            return new ScreenRow(indent, formatName(entry.category().getTranslationKey()), null);
        }

        @Override
        public ScreenRow visit(InfoModel.EffectCarrierRow entry) {
            return new ScreenRow(indent, formatName(entry.carrier().getTranslationKey()), null);
        }

        @Override
        public ScreenRow visit(InfoModel.PathEffectRow entry) {
            return new ScreenRow(indent, entry.effect().getName(),
                    ModDisplayText.formatEffectTime(entry.secondsLeft()));
        }

        @Override
        public ScreenRow visit(InfoModel.EffectMarkRow entry) {
            return new ScreenRow(indent, ModDisplayText.formatPath(entry.path()),
                    ModDisplayText.formatMarks(entry.marks()));
        }

        @Override
        public ScreenRow visit(InfoModel.BareMarksHeader entry) {
            return new ScreenRow(indent, formatLabel("bare_marks"), null);
        }

        @Override
        public ScreenRow visit(InfoModel.BareMarkRow entry) {
            return new ScreenRow(indent, ModDisplayText.formatPath(entry.path()),
                    ModDisplayText.formatMarks(entry.marks()));
        }

        @Override
        public ScreenRow visit(InfoModel.NoneRow entry) {
            return new ScreenRow(indent, ModDisplayText.formatNone(), null);
        }

        @Override
        public ScreenRow visit(InfoModel.QiPathAchieveHeader entry) {
            return new ScreenRow(indent, formatLabel("qi_path_achieve"), null);
        }

        @Override
        public ScreenRow visit(InfoModel.QiKindRow entry) {
            return new ScreenRow(indent, formatName(entry.kind().getTranslationKey()),
                    Component.literal(String.valueOf(entry.amount())));
        }

        @Override
        public ScreenRow visit(InfoModel.TimePathAchieveHeader entry) {
            return new ScreenRow(indent, formatLabel("time_path_achieve"), null);
        }

        @Override
        public ScreenRow visit(InfoModel.TimeRateUpRow entry) {
            return new ScreenRow(indent, formatLabel("time_rate_up"),
                    ModDisplayText.formatTimeRateUp(entry.rate()));
        }

        @Override
        public ScreenRow visit(InfoModel.StrengthPathAchieveHeader entry) {
            return new ScreenRow(indent, formatLabel("strength_path_achieve"), null);
        }

        @Override
        public ScreenRow visit(InfoModel.StrengthPathBranchRow entry) {
            return new ScreenRow(indent,
                    ModDisplayText.formatStrengthLabel(formatName(entry.branch().getTranslationKey()),
                            entry.totalJin()),
                    entry.getReading());
        }

        @Override
        public ScreenRow visit(InfoModel.CapacityRow entry) {
            return new ScreenRow(indent,
                    Component.translatable("guzhenren.screen.label.body_capacity"),
                    Component.translatable("guzhenren.screen.capacity", entry.usable(), entry.total()));
        }

        @Override
        public ScreenRow visit(InfoModel.AttackRow entry) {
            return new ScreenRow(indent, formatLabel("attack"),
                    Component.literal(ModDisplayText.formatAttackBonus(entry.bonus())));
        }

        @Override
        public ScreenRow visit(InfoModel.WisdomPathAchieveHeader entry) {
            return new ScreenRow(indent, formatLabel("wisdom_path_achieve"), null);
        }

        @Override
        public ScreenRow visit(InfoModel.ThoughtTagRow entry) {
            return new ScreenRow(indent, formatName(entry.tag().getTranslationKey()),
                    Component.literal(String.valueOf(entry.amount())));
        }

        @Override
        public ScreenRow visit(InfoModel.BrillianceRow entry) {
            return new ScreenRow(indent, formatLabel("brilliance"),
                    formatName(entry.brilliance().getTranslationKey()).append(ModDisplayText.formatDetail(
                            Component.translatable("guzhenren.display.brilliance_rate",
                                    entry.brilliance().getThoughtsPerSecond()))));
        }

        @Override
        public @Nullable ScreenRow visit(InfoModel.MindHeader entry) { return null; }

        @Override
        public ScreenRow visit(InfoModel.MindRow entry) {
            return new ScreenRow(indent, formatName(entry.type().getTranslationKey()),
                    Component.literal(ModDisplayText.formatPool(entry.pool().current(), entry.pool().max())));
        }
    }

    record ScreenRow(int indent, Component label, @Nullable Component value, @Nullable Click click) {

        ScreenRow(int indent, Component label, @Nullable Component value) { this(indent, label, value, null); }
    }

    record Click(int aperture) {}
}
