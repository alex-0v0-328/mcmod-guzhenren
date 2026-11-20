package net.alex.guzhenren.client.screen;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.display.InfoModel;
import net.alex.guzhenren.display.ModDisplayText;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

/**
 * B 面板信息行渲染器：将 {@link InfoModel} 条目转化为左标签、右可选值的双列文本布局并绘制。
 * <ul>
 *   <li>⚠ 数据模型仅定义行结构与排序而不决定界面文案，面板统一按左标签、右可选值两列排版。</li>
 *   <li>点击辅修流派行可打开 {@link PathPicker}；标题行无右侧值且使用当前页签强调色，其中脑海页签标题为 {@code null}
 *       （页签名称已写明）。</li>
 *   <li>{@link #getRows} 把两条道痕[Path Marks]行合并为一行的 {@code paired} 布局，并将力道读数行转换为
 *       {@code cells} 网格；{@link #getApertureGroups} 将空窍页签的模型按空窍拆组并去除间隔空行，供空窍页上下分段排
 *       版。</li>
 *   <li>{@link #getBlock} 将整页或单段空窍行量算为 {@link RowBlock}：右侧值列紧接在最宽「缩进 + 标签」之后一列起步，
 *       并排次条道痕紧接在最宽首条之后起步，读数格取最宽列宽，各列之间增加 {@code VALUE_GAP}（16 px）；列宽仅由当前
 *       行集合决定，使右侧值紧贴标签列而不浪费横向空间。</li>
 *   <li>在窄栏条件下（如英文界面或 GUI 6 缩放下仅 122 px 宽的空窍文字栏），{@link #getValueLeft} 先向左压缩值列直
 *       至贴近标签后 {@code VALUE_MIN_GAP}（绝不压盖标签），仍溢出时由 {@link #clip} 截断为「...」并由面板在悬停时显
 *       示完整文本。</li>
 *   <li>{@link #draw} 对 sealed 行条目执行无 {@code default} 分支的 {@code switch} 穷举匹配，确保新增行类型若漏写渲染
 *       逻辑会直接触发编译失败。</li>
 * </ul>
 */

final class InfoRowRenderer {

    private static final int VALUE_GAP = 16;
    private static final int VALUE_MIN_GAP = 6;

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

    static int getValueLeft(Font font, RowBlock block, ScreenRow row, Component value, int x0, int x1) {
        int aligned = x0 + (row.paired() ? block.pairLeft() : block.valueLeft());
        int afterLabel = x0 + row.indent() + font.width(row.label()) + VALUE_MIN_GAP;
        return Math.max(afterLabel, Math.min(aligned, x1 - font.width(value)));
    }

    static boolean isValueClipped(Font font, RowBlock block, ScreenRow row, int x0, int x1) {
        Component value = row.value();
        return value != null && getValueLeft(font, block, row, value, x0, x1) + font.width(value) > x1;
    }

    static FormattedCharSequence clip(Font font, FormattedText text, int width) {
        if (font.width(text) <= width) return Language.getInstance().getVisualOrder(text);

        FormattedText head = font.substrByWidth(text, width - font.width(CommonComponents.ELLIPSIS));
        return Language.getInstance().getVisualOrder(FormattedText.composite(head, CommonComponents.ELLIPSIS));
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
            case InfoModel.AppendageRow e -> new ScreenRow(indent, formatLabel("appendages"),
                    ComponentUtils.formatList(e.appendages(), appendage -> formatName(appendage.getTranslationKey())));
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
            case InfoModel.CritRow e -> new ScreenRow(indent, formatLabel("crit_chance"),
                    Component.literal(ModDisplayText.formatCritChance(e.chance())));
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
        MutableComponent text = Component.translatable("guzhenren.screen.capacity", row.total(), row.capacity());
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
