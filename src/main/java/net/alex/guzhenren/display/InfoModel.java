package net.alex.guzhenren.display;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.alex.guzhenren.gameplay.aperture.Aperture;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.ApertureStatus;
import net.alex.guzhenren.gameplay.attribute.AttackDamageService;
import net.alex.guzhenren.gameplay.body.BodyData;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.BodyStrengthService;
import net.alex.guzhenren.gameplay.body.Bone;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.alex.guzhenren.gameplay.body.Flesh;
import net.alex.guzhenren.gameplay.body.Physique;
import net.alex.guzhenren.gameplay.body.Race;
import net.alex.guzhenren.gameplay.body.Skin;
import net.alex.guzhenren.gameplay.body.Tendon;
import net.alex.guzhenren.gameplay.clock.TimeFlow;
import net.alex.guzhenren.gameplay.mind.Brilliance;
import net.alex.guzhenren.gameplay.mind.MindData;
import net.alex.guzhenren.gameplay.mind.MindPool;
import net.alex.guzhenren.gameplay.mind.MindPoolType;
import net.alex.guzhenren.gameplay.mind.MindService;
import net.alex.guzhenren.gameplay.mind.ThoughtTag;
import net.alex.guzhenren.gameplay.path.DaoMarks;
import net.alex.guzhenren.gameplay.path.EffectCarrier;
import net.alex.guzhenren.gameplay.path.GuAttainment;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.path.PathEffect;
import net.alex.guzhenren.gameplay.path.PathEffectCategory;
import net.alex.guzhenren.gameplay.path.PathEffectService;
import net.alex.guzhenren.gameplay.path.PathService;
import net.alex.guzhenren.gameplay.path.timed.TimedBuff;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthBuff;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import net.alex.guzhenren.gameplay.path.strength.HumanStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthData;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.gameplay.path.strength.StrengthPathBranch;
import net.alex.guzhenren.gameplay.soul.SoulData;
import net.alex.guzhenren.gameplay.soul.SoulService;
import net.alex.guzhenren.registry.effect.ModPathEffectTypes;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Every row the B panel draws: which rows, in what order, decided once, apart from how the panel words them.
 *
 * <p>Deciding "draw or not" here, not in the panel, is what keeps a section header with [无] from sitting beside
 * a real Dao-mark [道痕] figure. {@code Entry} is sealed and every row kind has its {@link Visitor} method, so a new
 * row is a compile error until the panel words it. ⚠ A named section (Strength Attainment [力道造诣], ...) is a
 * label plus its rows, never a value. The Paths tab [流派] ({@link #buildPathRows}) has two
 * parts: Dao marks [道痕], each path's attainment [造诣] and whole Dao-mark total with nothing per source, then
 * effects [效果], the named attainment sections followed by the path buffs [流派增益] and path debuffs [流派减益],
 * each grouped by carrier with every effect's time left. ⚠ A beast or human strength [兽力 / 人力] and a
 * {@link BeastStrengthBuff} read as their strength under Strength Attainment, up to {@link #READINGS_PER_ROW} to a
 * row, so they never also list as path buffs. Do not widen {@code Entry}.
 * Sealed without a {@code permits} list, {@code Entry} takes its row records from this file alone, so the class
 * stays whole past 300 lines: the records, their {@link Visitor} and the builders that order the rows are the one
 * table the panel reads.
 *
 * <p>Every entry compares by value, which is what lets the B panel keep the text it built for the last frame while
 * the rows are unchanged; {@link StrengthReadingRow} therefore carries readings and never a {@code Component},
 * which has no equality.
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
    public static final int READINGS_PER_ROW = 3;

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

        T visit(TendonRow entry);

        T visit(BoneRow entry);

        T visit(SkinRow entry);

        T visit(FleshRow entry);

        T visit(Soul entry);

        T visit(Lifespan entry);

        T visit(PathsHeader entry);

        T visit(EffectsHeader entry);

        T visit(PathRow entry);

        T visit(EffectCategoryHeader entry);

        T visit(EffectCarrierRow entry);

        T visit(PathEffectRow entry);

        T visit(StrengthPathAchieveHeader entry);

        T visit(StrengthPathBranchRow entry);

        T visit(StrengthReadingRow entry);

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

    public record TendonRow(Tendon tendon) implements Entry {

        @Override
        public <T> T accept(Visitor<T> visitor) { return visitor.visit(this); }
    }

    public record BoneRow(Bone bone) implements Entry {

        @Override
        public <T> T accept(Visitor<T> visitor) { return visitor.visit(this); }
    }

    public record SkinRow(Skin skin) implements Entry {

        @Override
        public <T> T accept(Visitor<T> visitor) { return visitor.visit(this); }
    }

    public record FleshRow(Flesh flesh) implements Entry {

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

    public record EffectsHeader(boolean empty) implements Entry {

        @Override
        public <T> T accept(Visitor<T> visitor) { return visitor.visit(this); }
    }

    public record PathRow(GuPath path, GuAttainment attainment, DaoMarks marks) implements Entry {

        @Override
        public <T> T accept(Visitor<T> visitor) { return visitor.visit(this); }
    }

    public record EffectCategoryHeader(PathEffectCategory category) implements Entry {

        @Override
        public <T> T accept(Visitor<T> visitor) { return visitor.visit(this); }
    }

    public record EffectCarrierRow(EffectCarrier carrier) implements Entry {

        @Override
        public <T> T accept(Visitor<T> visitor) { return visitor.visit(this); }
    }

    public record PathEffectRow(PathEffect<?> effect, long secondsLeft) implements Entry {

        @Override
        public <T> T accept(Visitor<T> visitor) { return visitor.visit(this); }
    }

    public record StrengthPathAchieveHeader() implements Entry {

        @Override
        public <T> T accept(Visitor<T> visitor) { return visitor.visit(this); }
    }

    public record StrengthPathBranchRow(StrengthPathBranch branch, int totalJin) implements Entry {

        @Override
        public <T> T accept(Visitor<T> visitor) { return visitor.visit(this); }
    }

    public record StrengthReadingRow(List<StrengthReading> readings) implements Entry {

        @Override
        public <T> T accept(Visitor<T> visitor) { return visitor.visit(this); }
    }

    /** One strength the body holds: {@code count} beasts of a family, or {@code count} of a human unit. */
    public sealed interface StrengthReading {}

    /** {@code secondsLeft} is {@code PathEffect.PERMANENT} for a refined beast strength. */
    public record BeastReading(BeastStrengthFamily family, int count, long secondsLeft) implements StrengthReading {}

    /** {@code unit} is {@link HumanStrength#JIN} or {@link HumanStrength#JUN}, the ten-fold kinds folded in. */
    public record HumanReading(HumanStrength unit, int count) implements StrengthReading {}

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

    public static List<Row> buildApertureRows(Player player) {
        ApertureData data = ApertureService.get(player);
        ApertureStatus status = ApertureService.getStatus(player);
        List<Row> rows = new ArrayList<>();

        if (data.count() <= 1) {
            boolean second = data.getPrimary().second();
            rows.add(new Row(0, new ApertureIndex(second ? 2 : 1, 0)));
            addApertureBlock(rows, data.getPrimary(), data.hasAperture(), 0, ApertureData.PRIMARY, !second,
                    BodyService.isExtreme(player), status);
            return rows;
        }
        for (int i = 0; i < data.count(); i++) {
            rows.add(new Row(0, new ApertureIndex(i + 1, i)));
            addApertureBlock(rows, data.get(i), true, INDENT, i, !data.get(i).second(),
                    BodyService.isExtreme(player), ApertureService.getStatus(player, i));
            if (i < data.count() - 1) rows.add(new Row(0, new Blank()));
        }
        return rows;
    }

    private static void addApertureBlock(List<Row> rows, Aperture aperture, boolean awakened, int indent,
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

    public static List<Row> buildBodyRows(Player player) {
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
        rows.add(new Row(0, new TendonRow(body.tendon())));
        rows.add(new Row(0, new BoneRow(body.bone())));
        rows.add(new Row(0, new SkinRow(body.skin())));
        rows.add(new Row(0, new FleshRow(body.flesh())));
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

    public static List<Row> buildSoulRows(Player player) {
        return List.of(new Row(0, new Soul(SoulService.get(player))));
    }

    public static List<Row> buildPathRows(Player player) {
        List<Row> rows = new ArrayList<>();
        addPaths(rows, player);

        List<Row> effects = new ArrayList<>();
        addStrengthPathAchieve(effects, player);
        addTimePathAchieve(effects, player);
        addWisdomPathAchieve(effects, player);
        for (PathEffectCategory category : PathEffectCategory.values()) addPathEffects(effects, player, category);
        rows.add(new Row(0, new EffectsHeader(effects.isEmpty())));
        effects.forEach(row -> rows.add(new Row(row.indent() + INDENT, row.entry())));
        return rows;
    }

    private static void addTimePathAchieve(List<Row> rows, Player player) {
        int rate = TimeFlow.getSelfRate(player);
        if (rate <= TimeFlow.NORMAL_RATE) return;

        rows.add(new Row(0, new TimePathAchieveHeader()));
        rows.add(new Row(INDENT, new TimeRateUpRow(rate)));
    }

    private static void addPaths(List<Row> rows, Player player) {
        Set<GuPath> paths = PathService.getShownPaths(player);

        rows.add(new Row(0, new PathsHeader(paths.isEmpty())));
        for (GuPath path : paths) {
            rows.add(new Row(INDENT, new PathRow(path, PathService.getAttainment(player, path),
                    PathService.getMarks(player, path))));
        }
    }

    private static void addPathEffects(List<Row> rows, Player player, PathEffectCategory category) {
        List<PathEffect<?>> effects = PathEffectService.getEffects(player).stream()
                .filter(effect -> effect.type().getCategory() == category && !isStrengthReading(effect)).toList();
        if (effects.isEmpty()) return;

        long now = PathEffectService.getGameTime(player);
        rows.add(new Row(0, new EffectCategoryHeader(category)));
        for (EffectCarrier carrier : EffectCarrier.values()) {
            List<PathEffect<?>> carried = effects.stream().filter(effect -> effect.carrier() == carrier).toList();
            if (carried.isEmpty()) continue;

            rows.add(new Row(INDENT, new EffectCarrierRow(carrier)));
            for (PathEffect<?> effect : carried) {
                rows.add(new Row(2 * INDENT, new PathEffectRow(effect, getSecondsLeft(effect, now))));
            }
        }
    }

    private static boolean isStrengthReading(PathEffect<?> effect) {
        return effect.type() == ModPathEffectTypes.BEAST_STRENGTH || effect.type() == ModPathEffectTypes.HUMAN_STRENGTH
                || getBeastBuff(effect) != null;
    }

    private static @Nullable BeastStrengthBuff getBeastBuff(PathEffect<?> effect) {
        return effect.payload() instanceof TimedBuff buff
                && buff.getHolder().value() instanceof BeastStrengthBuff beast ? beast : null;
    }

    static long getSecondsLeft(PathEffect<?> effect, long now) {
        return effect.isPermanent() ? PathEffect.PERMANENT
                : ModDisplayText.getCountdownSeconds(effect.getTicksLeft(now));
    }

    private static void addStrengthPathAchieve(List<Row> rows, Player player) {
        PathStrengthData data = PathStrengthService.get(player);
        List<StrengthReading> beasts = getBeastReadings(player, data);
        List<StrengthReading> humans = getHumanReadings(data);
        if (beasts.isEmpty() && humans.isEmpty()) return;

        rows.add(new Row(0, new StrengthPathAchieveHeader()));
        addStrengthBranch(rows, StrengthPathBranch.BEAST_STRENGTH_PHANTOM, 0, beasts);
        addStrengthBranch(rows, StrengthPathBranch.HUMAN_JUN_STRENGTH, data.getTotalJin(), humans);
    }

    private static void addStrengthBranch(List<Row> rows, StrengthPathBranch branch, int totalJin,
                                          List<StrengthReading> readings) {
        if (readings.isEmpty()) return;

        rows.add(new Row(INDENT, new StrengthPathBranchRow(branch, totalJin)));
        for (int i = 0; i < readings.size(); i += READINGS_PER_ROW) {
            List<StrengthReading> line = readings.subList(i, Math.min(readings.size(), i + READINGS_PER_ROW));
            rows.add(new Row(2 * INDENT, new StrengthReadingRow(List.copyOf(line))));
        }
    }

    private static List<StrengthReading> getBeastReadings(Player player, PathStrengthData data) {
        Map<BeastStrengthFamily, Integer> refined = data.getBeastReadings();
        List<PathEffect<?>> effects = PathEffectService.getEffects(player);
        long now = PathEffectService.getGameTime(player);
        List<StrengthReading> readings = new ArrayList<>();
        for (BeastStrengthFamily family : BeastStrengthFamily.values()) {
            int count = refined.getOrDefault(family, 0);
            if (count > 0) readings.add(new BeastReading(family, count, PathEffect.PERMANENT));
            for (PathEffect<?> effect : effects) {
                BeastStrengthBuff buff = getBeastBuff(effect);
                if (buff != null && buff.getBeastFamily() == family) {
                    readings.add(new BeastReading(family, 1, getSecondsLeft(effect, now)));
                }
            }
        }
        return readings;
    }

    private static List<StrengthReading> getHumanReadings(PathStrengthData data) {
        List<StrengthReading> readings = new ArrayList<>();
        if (data.getJinReading() > 0) readings.add(new HumanReading(HumanStrength.JIN, data.getJinReading()));
        if (data.getJunReading() > 0) readings.add(new HumanReading(HumanStrength.JUN, data.getJunReading()));
        return readings;
    }

    private static void addWisdomPathAchieve(List<Row> rows, Player player) {
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

    public static List<Row> buildMindRows(Player player) {
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
