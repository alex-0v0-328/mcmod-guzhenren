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
import net.alex.guzhenren.gameplay.path.time.PathTimeFlowService;
import net.alex.guzhenren.gameplay.soul.SoulData;
import net.alex.guzhenren.gameplay.soul.SoulService;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Every row the B panel and the info command share: which rows, in what order, decided once.
 *
 * <p>Deciding "draw or not" here, not in a surface, is what keeps one side from printing a section
 * header with [无] beside a real Dao-mark [道痕] figure. {@code Entry} is sealed on purpose so a new
 * row is a compile error in both switches. ⚠ A named section (Strength Attainment [力道造诣], Qi
 * Attainment [气道造诣], ...) is a label plus its rows, never a value; attainment [造诣] and Dao
 * marks [道痕] belong to the path-attainment list [流派造诣]. Do not widen {@code Entry}.
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

    public sealed interface Entry {}

    //region Aperture
    public record ApertureIndex(int number, int index) implements Entry {}

    public record Blank() implements Entry {}

    public record Realm(Aperture aperture) implements Entry {}

    public record Status(ApertureStatus status) implements Entry {}

    public record TalentRow(Aperture aperture, boolean awakened) implements Entry {}

    public record Essence(Aperture aperture) implements Entry {}

    public record Distilled(Aperture aperture) implements Entry {}

    public record Pressure(Aperture aperture) implements Entry {}

    public record PathChoice(boolean primary, int aperture, @Nullable GuPath path) implements Entry {}
    //endregion

    //region Body
    public record PhysiqueRow(@Nullable Physique physique, ExtremePhysique extremePhysique) implements Entry {}

    public record RaceRow(Race race) implements Entry {}

    public record Soul(SoulData soul) implements Entry {}

    public record Lifespan(double lifespan, double age) implements Entry {}

    public record PathsHeader(boolean empty) implements Entry {}

    public record PathRow(GuPath path, PathEntry entry) implements Entry {}

    public record QiPathAchieveHeader() implements Entry {}

    public record QiKindRow(QiKind kind, long amount) implements Entry {}

    public record StrengthPathAchieveHeader() implements Entry {}

    public record StrengthPathBranchRow(StrengthPathBranch branch, int totalJin,
                                        Component reading) implements Entry {}

    public record TimePathAchieveHeader() implements Entry {}

    public record TimeRateUpRow(int rate) implements Entry {}

    public record CapacityRow(int usable, int total) implements Entry {}

    public record AttackRow(double bonus) implements Entry {}

    public record WisdomPathAchieveHeader() implements Entry {}

    public record ThoughtTagRow(ThoughtTag tag, long amount) implements Entry {}
    //endregion

    //region Mind
    public record BrillianceRow(Brilliance brilliance) implements Entry {}

    public record MindHeader() implements Entry {}

    public record MindRow(MindPoolType type, MindPool pool) implements Entry {}
    //endregion

    public static List<Row> aperture(Player player) {
        ApertureData data = ApertureService.get(player);
        ApertureStatus status = ApertureService.getStatus(player);
        List<Row> rows = new ArrayList<>();

        if (data.count() <= 1) {
            boolean second = data.primary().second();
            rows.add(new Row(0, new ApertureIndex(second ? 2 : 1, 0)));
            apertureBlock(rows, data.primary(), data.hasAperture(), 0, ApertureData.PRIMARY, !second,
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
        rows.add(new Row(0, new Lifespan(body.lifespanYears(), body.ageYears())));
        if (!strength.isEmpty() && strength.hasPathBranch(StrengthPathBranch.HUMAN_JUN_STRENGTH)) {
            rows.add(new Row(0, new CapacityRow(BodyStrengthService.getUsableJin(player), strength.totalJin())));
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
        int rate = PathTimeFlowService.getRate(player);
        if (rate <= PathTimeFlowService.NORMAL_RATE) return;

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
            rows.add(new Row(INDENT,
                    new StrengthPathBranchRow(StrengthPathBranch.BEAST_STRENGTH_PHANTOM, 0,
                            ModDisplayText.beastStrengthLine(data))));
        }
        if (data.hasPathBranch(StrengthPathBranch.HUMAN_JUN_STRENGTH)) {
            rows.add(new Row(INDENT, new StrengthPathBranchRow(StrengthPathBranch.HUMAN_JUN_STRENGTH,
                    data.totalJin(), ModDisplayText.humanStrengthLine(data))));
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
            rows.add(new Row(0, new MindRow(type, mind.pool(type))));
        }
        return rows;
    }
}
