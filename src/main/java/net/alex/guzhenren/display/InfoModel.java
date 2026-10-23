package net.alex.guzhenren.display;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.alex.guzhenren.gameplay.aperture.Aperture;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.ApertureStatus;
import net.alex.guzhenren.gameplay.attribute.AttackDamageService;
import net.alex.guzhenren.gameplay.body.BodyData;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.BodyStrengthService;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.alex.guzhenren.gameplay.body.Physique;
import net.alex.guzhenren.gameplay.body.Race;
import net.alex.guzhenren.gameplay.clock.TimeFlow;
import net.alex.guzhenren.gameplay.mind.Brilliance;
import net.alex.guzhenren.gameplay.mind.MindData;
import net.alex.guzhenren.gameplay.mind.MindPool;
import net.alex.guzhenren.gameplay.mind.MindPoolType;
import net.alex.guzhenren.gameplay.mind.MindService;
import net.alex.guzhenren.gameplay.mind.ThoughtTag;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.path.PathEntry;
import net.alex.guzhenren.gameplay.path.PathService;
import net.alex.guzhenren.gameplay.path.qi.PathQiService;
import net.alex.guzhenren.gameplay.path.qi.QiKind;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthData;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.gameplay.path.strength.StrengthPathBranch;
import net.alex.guzhenren.gameplay.soul.SoulData;
import net.alex.guzhenren.gameplay.soul.SoulService;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Every row the B panel and the info command share: which rows, in what order, decided once.
 *
 * <p>Deciding "draw or not" here, not in a surface, is what keeps one side from printing a section
 * header with [无] beside a real Dao-mark [道痕] figure. {@code Entry} is sealed and every row kind has its
 * {@link Visitor} method, so a new row is a compile error in both surfaces until each words it. ⚠ A named
 * section (Strength Attainment [力道造诣], Qi Attainment [气道造诣], ...) is a label plus its rows, never a value;
 * attainment [造诣] and Dao marks [道痕] belong to the path-attainment list [流派造诣]. Do not widen {@code Entry}.
 *
 * <p>Every entry compares by value, which is what lets the B panel keep the text it built for the last frame while
 * the rows are unchanged; {@link StrengthPathBranchRow} therefore carries the data and words its reading on demand
 * instead of holding a {@code Component}, which has no equality.
 *
 * <p>⚠ In {@link ApertureIndex}, {@code number} is the display ordinal (1 = first aperture, 2 =
 * second), while {@code index} is the real list position -- a lone second aperture shows number 2
 * but lives at index 0.
 *
 * @author Alex
 * @version 1.0.0
 * @see ModDisplayText
 * @since 1.0.0
 */

public final class InfoModel {

    private InfoModel() {}

    public static final int INDENT = 10;

    public record Row(int indent, Entry entry) {}

    public sealed interface Entry {

        <T> T accept(Visitor<T> visitor);
    }

    public interface Visitor<T> {

        T visit(ApertureIndex entry);

        T visit(Blank entry);

        T visit(Realm entry);

        T visit(Status entry);

        T visit(TalentRow entry);

        T visit(Essence entry);

        T visit(Distilled entry);

        T visit(Pressure entry);

        T visit(PathChoice entry);

        T visit(PhysiqueRow entry);

        T visit(RaceRow entry);

        T visit(Soul entry);

        T visit(Lifespan entry);

        T visit(PathsHeader entry);

        T visit(PathRow entry);

        T visit(QiPathAchieveHeader entry);

        T visit(QiKindRow entry);

        T visit(StrengthPathAchieveHeader entry);

        T visit(StrengthPathBranchRow entry);

        T visit(TimePathAchieveHeader entry);

        T visit(TimeRateUpRow entry);

        T visit(CapacityRow entry);

        T visit(AttackRow entry);

        T visit(WisdomPathAchieveHeader entry);

        T visit(ThoughtTagRow entry);

        T visit(BrillianceRow entry);

        T visit(MindHeader entry);

        T visit(MindRow entry);
    }

    //region Aperture
    public record ApertureIndex(int number, int index) implements Entry {

        @Override
        public <T> T accept(Visitor<T> visitor) { return visitor.visit(this); }
    }

    public record Blank() implements Entry {

        @Override
        public <T> T accept(Visitor<T> visitor) { return visitor.visit(this); }
    }

    public record Realm(Aperture aperture) implements Entry {

        @Override
        public <T> T accept(Visitor<T> visitor) { return visitor.visit(this); }
    }

    public record Status(ApertureStatus status) implements Entry {

        @Override
        public <T> T accept(Visitor<T> visitor) { return visitor.visit(this); }
    }

    public record TalentRow(Aperture aperture, boolean awakened) implements Entry {

        @Override
        public <T> T accept(Visitor<T> visitor) { return visitor.visit(this); }
    }

    public record Essence(Aperture aperture) implements Entry {

        @Override
        public <T> T accept(Visitor<T> visitor) { return visitor.visit(this); }
    }

    public record Distilled(Aperture aperture) implements Entry {

        @Override
        public <T> T accept(Visitor<T> visitor) { return visitor.visit(this); }
    }

    public record Pressure(Aperture aperture) implements Entry {

        @Override
        public <T> T accept(Visitor<T> visitor) { return visitor.visit(this); }
    }

    public record PathChoice(boolean primary, int aperture, @Nullable GuPath path) implements Entry {

        @Override
        public <T> T accept(Visitor<T> visitor) { return visitor.visit(this); }
    }
    //endregion

    //region Body
    public record PhysiqueRow(@Nullable Physique physique, ExtremePhysique extremePhysique) implements Entry {

        @Override
        public <T> T accept(Visitor<T> visitor) { return visitor.visit(this); }
    }

    public record RaceRow(Race race) implements Entry {

        @Override
        public <T> T accept(Visitor<T> visitor) { return visitor.visit(this); }
    }

    public record Soul(SoulData soul) implements Entry {

        @Override
        public <T> T accept(Visitor<T> visitor) { return visitor.visit(this); }
    }

    public record Lifespan(double lifespan, double age) implements Entry {

        @Override
        public <T> T accept(Visitor<T> visitor) { return visitor.visit(this); }
    }

    public record PathsHeader(boolean empty) implements Entry {

        @Override
        public <T> T accept(Visitor<T> visitor) { return visitor.visit(this); }
    }

    public record PathRow(GuPath path, PathEntry entry) implements Entry {

        @Override
        public <T> T accept(Visitor<T> visitor) { return visitor.visit(this); }
    }

    public record QiPathAchieveHeader() implements Entry {

        @Override
        public <T> T accept(Visitor<T> visitor) { return visitor.visit(this); }
    }

    public record QiKindRow(QiKind kind, long amount) implements Entry {

        @Override
        public <T> T accept(Visitor<T> visitor) { return visitor.visit(this); }
    }

    public record StrengthPathAchieveHeader() implements Entry {

        @Override
        public <T> T accept(Visitor<T> visitor) { return visitor.visit(this); }
    }

    public record StrengthPathBranchRow(StrengthPathBranch branch, int totalJin,
                                        PathStrengthData data) implements Entry {

        @Override
        public <T> T accept(Visitor<T> visitor) { return visitor.visit(this); }

        public Component reading() {
            return branch == StrengthPathBranch.BEAST_STRENGTH_PHANTOM
                    ? ModDisplayText.beastStrengthLine(data)
                    : ModDisplayText.humanStrengthLine(data);
        }
    }

    public record TimePathAchieveHeader() implements Entry {

        @Override
        public <T> T accept(Visitor<T> visitor) { return visitor.visit(this); }
    }

    public record TimeRateUpRow(int rate) implements Entry {

        @Override
        public <T> T accept(Visitor<T> visitor) { return visitor.visit(this); }
    }

    public record CapacityRow(int usable, int total) implements Entry {

        @Override
        public <T> T accept(Visitor<T> visitor) { return visitor.visit(this); }
    }

    public record AttackRow(double bonus) implements Entry {

        @Override
        public <T> T accept(Visitor<T> visitor) { return visitor.visit(this); }
    }

    public record WisdomPathAchieveHeader() implements Entry {

        @Override
        public <T> T accept(Visitor<T> visitor) { return visitor.visit(this); }
    }

    public record ThoughtTagRow(ThoughtTag tag, long amount) implements Entry {

        @Override
        public <T> T accept(Visitor<T> visitor) { return visitor.visit(this); }
    }
    //endregion

    //region Mind
    public record BrillianceRow(Brilliance brilliance) implements Entry {

        @Override
        public <T> T accept(Visitor<T> visitor) { return visitor.visit(this); }
    }

    public record MindHeader() implements Entry {

        @Override
        public <T> T accept(Visitor<T> visitor) { return visitor.visit(this); }
    }

    public record MindRow(MindPoolType type, MindPool pool) implements Entry {

        @Override
        public <T> T accept(Visitor<T> visitor) { return visitor.visit(this); }
    }
    //endregion

    public static List<Row> aperture(Player player) {
        ApertureData data = ApertureService.get(player);
        ApertureStatus status = ApertureService.getStatus(player);
        List<Row> rows = new ArrayList<>();

        if (data.count() <= 1) {
            boolean second = data.getPrimary().second();
            rows.add(new Row(0, new ApertureIndex(second ? 2 : 1, 0)));
            apertureBlock(rows, data.getPrimary(), data.hasAperture(), 0, ApertureData.PRIMARY, !second,
                    BodyService.isExtreme(player), status);
            return rows;
        }
        for (int i = 0; i < data.count(); i++) {
            rows.add(new Row(0, new ApertureIndex(i + 1, i)));
            apertureBlock(rows, data.get(i), true, INDENT, i, !data.get(i).second(),
                    BodyService.isExtreme(player), ApertureService.getStatus(player, i));
            if (i < data.count() - 1) rows.add(new Row(0, new Blank()));
        }
        return rows;
    }

    private static void apertureBlock(List<Row> rows, Aperture aperture, boolean awakened, int indent,
                                      int index, boolean pressure, boolean extreme, ApertureStatus status) {
        rows.add(new Row(indent, new Realm(aperture)));
        rows.add(new Row(indent, new TalentRow(aperture, awakened)));
        if (awakened) {
            rows.add(new Row(indent, new Status(status)));
            rows.add(new Row(indent, new Essence(aperture)));
            if (aperture.distilledEssence() > 0L) rows.add(new Row(indent, new Distilled(aperture)));
            if (pressure && extreme) rows.add(new Row(indent, new Pressure(aperture)));
            rows.add(new Row(indent, new PathChoice(true, index, aperture.primaryPath())));
            rows.add(new Row(indent, new PathChoice(false, index, aperture.secondaryPath())));
        }
    }

    public static List<Row> body(Player player) {
        BodyData body = BodyService.get(player);
        PathStrengthData strength = PathStrengthService.get(player);
        List<Row> rows = new ArrayList<>();

        if (body.physiques().isEmpty()) {
            rows.add(new Row(0, new PhysiqueRow(null, ExtremePhysique.NONE)));
        } else {
            body.physiques().forEach(physique -> rows.add(new Row(0,
                    new PhysiqueRow(physique, body.extremePhysique()))));
        }
        rows.add(new Row(0, new RaceRow(body.race())));
        rows.add(new Row(0, new Lifespan(body.getLifespanYears(), body.ageYears())));
        if (!strength.isEmpty() && strength.hasPathBranch(StrengthPathBranch.HUMAN_JUN_STRENGTH)) {
            rows.add(new Row(0, new CapacityRow(BodyStrengthService.getUsableJin(player), strength.getTotalJin())));
        }
        double attackBonus = AttackDamageService.getBonus(player);
        if (shouldShowAttackRow(strength.isEmpty(), attackBonus)) {
            rows.add(new Row(0, new AttackRow(attackBonus)));
        }
        return rows;
    }

    static boolean shouldShowAttackRow(boolean strengthEmpty, double attackBonus) {
        return !strengthEmpty || attackBonus != 0.0D;
    }

    public static List<Row> soul(Player player) {
        return List.of(new Row(0, new Soul(SoulService.get(player))));
    }

    public static List<Row> pathAchieve(Player player) {
        List<Row> rows = new ArrayList<>();
        strengthPathAchieve(rows, player);
        timePathAchieve(rows, player);
        qiPathAchieve(rows, player);
        wisdomPathAchieve(rows, player);
        paths(rows, player);
        return rows;
    }

    private static void timePathAchieve(List<Row> rows, Player player) {
        int rate = TimeFlow.getSelfRate(player);
        if (rate <= TimeFlow.NORMAL_RATE) return;

        rows.add(new Row(0, new TimePathAchieveHeader()));
        rows.add(new Row(INDENT, new TimeRateUpRow(rate)));
    }

    private static void paths(List<Row> rows, Player player) {
        Map<GuPath, PathEntry> paths = PathService.getVisibleEntries(player);

        rows.add(new Row(0, new PathsHeader(paths.isEmpty())));
        paths.forEach((path, entry) -> rows.add(new Row(INDENT, new PathRow(path, entry))));
    }

    private static void qiPathAchieve(List<Row> rows, Player player) {
        List<Row> held = new ArrayList<>();
        for (QiKind kind : QiKind.values()) {
            long amount = PathQiService.getCurrent(player, kind);
            if (amount > 0L) held.add(new Row(INDENT, new QiKindRow(kind, amount)));
        }
        if (held.isEmpty()) return;

        rows.add(new Row(0, new QiPathAchieveHeader()));
        rows.addAll(held);
    }

    private static void strengthPathAchieve(List<Row> rows, Player player) {
        PathStrengthData data = PathStrengthService.get(player);
        if (data.isEmpty()) return;

        rows.add(new Row(0, new StrengthPathAchieveHeader()));
        if (data.hasPathBranch(StrengthPathBranch.BEAST_STRENGTH_PHANTOM)) {
            rows.add(new Row(INDENT, new StrengthPathBranchRow(StrengthPathBranch.BEAST_STRENGTH_PHANTOM, 0, data)));
        }
        if (data.hasPathBranch(StrengthPathBranch.HUMAN_JUN_STRENGTH)) {
            rows.add(new Row(INDENT, new StrengthPathBranchRow(StrengthPathBranch.HUMAN_JUN_STRENGTH,
                    data.getTotalJin(), data)));
        }
    }

    private static void wisdomPathAchieve(List<Row> rows, Player player) {
        List<Row> held = new ArrayList<>();
        long natural = MindService.getNaturalThoughts(player);
        if (natural > 0L) held.add(new Row(INDENT, new ThoughtTagRow(ThoughtTag.NATURAL, natural)));
        MindService.getTaggedThoughts(player).forEach((tag, amount) -> {
            if (amount > 0L) held.add(new Row(INDENT, new ThoughtTagRow(tag, amount)));
        });
        if (held.isEmpty()) return;

        rows.add(new Row(0, new WisdomPathAchieveHeader()));
        rows.addAll(held);
    }

    public static List<Row> mind(Player player) {
        MindData mind = MindService.get(player);
        List<Row> rows = new ArrayList<>();

        rows.add(new Row(0, new BrillianceRow(mind.brilliance())));
        rows.add(new Row(0, new MindHeader()));
        for (MindPoolType type : MindPoolType.values()) {
            rows.add(new Row(0, new MindRow(type, mind.getPool(type))));
        }
        return rows;
    }
}
