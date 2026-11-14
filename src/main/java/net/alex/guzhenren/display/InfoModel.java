package net.alex.guzhenren.display;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.alex.guzhenren.gameplay.aperture.Aperture;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.ApertureStatus;
import net.alex.guzhenren.gameplay.body.BodyData;
import net.alex.guzhenren.gameplay.body.BodyService;
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
import net.alex.guzhenren.gameplay.path.EffectCarrier;
import net.alex.guzhenren.gameplay.path.GuAttainment;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.path.PathEffect;
import net.alex.guzhenren.gameplay.path.PathEffectCategory;
import net.alex.guzhenren.gameplay.path.PathEffectService;
import net.alex.guzhenren.gameplay.path.PathMarks;
import net.alex.guzhenren.gameplay.path.PathService;
import net.alex.guzhenren.gameplay.path.strength.AttackDamageService;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthBuff;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import net.alex.guzhenren.gameplay.path.strength.HumanStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthData;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.gameplay.path.strength.StrengthPathBranch;
import net.alex.guzhenren.gameplay.path.strength.StrengthService;
import net.alex.guzhenren.gameplay.path.strength.StrengthSnapshot;
import net.alex.guzhenren.gameplay.path.timed.TimedBuff;
import net.alex.guzhenren.gameplay.soul.SoulData;
import net.alex.guzhenren.gameplay.soul.SoulService;
import net.alex.guzhenren.registry.effect.ModPathEffectTypes;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * B 面板全部行的目录，与面板措辞分开。
 *
 * <ul>
 *   <li>画哪些行、何序、画不画一处决定：带 [无] 的分区标题才不挨着真实道痕[Path Marks]数字。</li>
 *   <li>{@code Entry} sealed、不写 {@code permits}，行记录只来自本文件：记录与排行构建方法即面板读的唯一一张表；面板对
 *       它 switch 不写 default，新行写出措辞前编译不过；不要放宽 {@code Entry}。</li>
 *   <li>条目按值比较，行不变时 B 面板才沿用上一帧文字；故 {@link StrengthReadingRow} 只带读数，不带无相等性的
 *       {@code Component}。</li>
 *   <li>{@link #buildPathRows} 分两段：道痕，列每条道[Path]的造诣与整条道痕总数，不按来源分；效果，先具名造诣分区，再流
 *       派增益、减益，各按载体分组并带剩余时间。</li>
 *   <li>⚠ 具名分区（力道造诣等）是标签加其下各行，从不是数值；兽力[Beast Strength]、人力与 {@link BeastStrengthBuff} 在
 *       力道造诣下按力量读数显示，每行至多 {@link #READINGS_PER_ROW} 个，不再列为流派增益。</li>
 *   <li>⚠ {@link ApertureIndex} 的 {@code number} 是显示序号（1 = 第一空窍，2 = 第二空窍），{@code index} 是真实列表位
 *       置：单独的第二空窍显示 2，却在 index 0。</li>
 *   <li>超 300 行有意：面板行 sealed 目录，每行一条记录连同构建方法；增改一种行只在 {@code Entry} 旁加记录。</li>
 * </ul>
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

    public sealed interface Entry {}

    //region Aperture [空窍]
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

    //region Body [肉身]
    public record PhysiqueRow(@Nullable Physique physique, ExtremePhysique extremePhysique) implements Entry {}

    public record RaceRow(Race race) implements Entry {}

    public record TendonRow(Tendon tendon) implements Entry {}

    public record BoneRow(Bone bone) implements Entry {}

    public record SkinRow(Skin skin) implements Entry {}

    public record FleshRow(Flesh flesh) implements Entry {}

    public record Soul(SoulData soul) implements Entry {}

    public record Lifespan(double lifespan, double age) implements Entry {}

    public record PathsHeader(boolean empty) implements Entry {}

    public record EffectsHeader(boolean empty) implements Entry {}

    public record PathRow(GuPath path, GuAttainment attainment, PathMarks marks) implements Entry {}

    public record EffectCategoryHeader(PathEffectCategory category) implements Entry {}

    public record EffectCarrierRow(EffectCarrier carrier) implements Entry {}

    public record PathEffectRow(PathEffect<?> effect, long secondsLeft) implements Entry {}

    public record StrengthPathAchieveHeader() implements Entry {}

    public record StrengthPathBranchRow(StrengthPathBranch branch, int totalJin) implements Entry {}

    public record StrengthReadingRow(List<StrengthReading> readings) implements Entry {}

    /** 身体持有的一种力量：某兽族 {@code count} 头，或某人力单位 {@code count} 个。 */
    public sealed interface StrengthReading {}

    /** 已炼化的兽力[Beast Strength]，{@code secondsLeft} 为 {@code PathEffect.PERMANENT}。 */
    public record BeastReading(BeastStrengthFamily family, int count, @Nullable BeastStrength innate,
                               long secondsLeft) implements StrengthReading {}

    /** {@code unit} 只取 {@link HumanStrength#JIN} 或 {@link HumanStrength#JUN}，十倍、百倍种类并入其中。 */
    public record HumanReading(HumanStrength unit, int count) implements StrengthReading {}

    public record TimePathAchieveHeader() implements Entry {}

    public record TimeRateUpRow(int rate) implements Entry {}

    /**
     * 可发挥力量点[Strength Points] / 持有力量点（兽力[Beast Strength]与人力合计）；旁列当前承受力、其所基的常态承受
     * 力、超出的肉体过载（无则 0）。
     */
    public record CapacityRow(int usable, int total, int capacity, int normalCapacity, int overload)
            implements Entry {}

    public record AttackRow(double bonus) implements Entry {}

    public record WisdomPathAchieveHeader() implements Entry {}

    public record ThoughtTagRow(ThoughtTag tag, long amount) implements Entry {}
    //endregion

    //region Mind [脑海]
    public record BrillianceRow(Brilliance brilliance) implements Entry {}

    public record MindHeader() implements Entry {}

    public record MindRow(MindPoolType type, MindPool pool) implements Entry {}
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
            for (Physique physique : body.physiques()) {
                rows.add(new Row(0, new PhysiqueRow(physique, body.extremePhysique())));
            }
        }
        rows.add(new Row(0, new RaceRow(body.race())));
        rows.add(new Row(0, new TendonRow(body.tendon())));
        rows.add(new Row(0, new BoneRow(body.bone())));
        rows.add(new Row(0, new SkinRow(body.skin())));
        rows.add(new Row(0, new FleshRow(body.flesh())));
        rows.add(new Row(0, new Lifespan(body.getLifespanYears(), body.ageYears())));
        StrengthSnapshot settlement = StrengthService.snapshot(player);
        if (settlement.totalPoints() > 0) {
            rows.add(new Row(0, new CapacityRow(StrengthService.getUsablePoints(player, settlement),
                    settlement.totalPoints(), settlement.capacity(), settlement.normalCapacity(),
                    settlement.overloadPoints())));
        }
        double attackBonus = AttackDamageService.getFistBonus(player);
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
        for (Row row : effects) rows.add(new Row(row.indent() + INDENT, row.entry()));
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
        List<PathEffect<?>> effects = new ArrayList<>();
        for (PathEffect<?> effect : PathEffectService.getEffects(player)) {
            if (effect.type().getCategory() == category && !isStrengthReading(effect)) effects.add(effect);
        }
        if (effects.isEmpty()) return;

        long now = player.level().getGameTime();
        rows.add(new Row(0, new EffectCategoryHeader(category)));
        for (EffectCarrier carrier : EffectCarrier.values()) {
            boolean headed = false;
            for (PathEffect<?> effect : effects) {
                if (effect.carrier() != carrier) continue;
                if (!headed) rows.add(new Row(INDENT, new EffectCarrierRow(carrier)));
                headed = true;
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

    private static long getSecondsLeft(PathEffect<?> effect, long now) {
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
        long now = player.level().getGameTime();
        List<StrengthReading> readings = new ArrayList<>();
        for (BeastStrengthFamily family : BeastStrengthFamily.values()) {
            int count = refined.getOrDefault(family, 0);
            if (count > 0) readings.add(new BeastReading(family, count, data.getInnate(family), PathEffect.PERMANENT));
            for (PathEffect<?> effect : effects) {
                BeastStrengthBuff buff = getBeastBuff(effect);
                if (buff != null && buff.getBeastFamily() == family) {
                    readings.add(new BeastReading(family, 1, null, getSecondsLeft(effect, now)));
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
        for (Map.Entry<ThoughtTag, Long> tagged : MindService.getTaggedThoughts(player).entrySet()) {
            long amount = tagged.getValue();
            if (amount > 0L) held.add(new Row(INDENT, new ThoughtTagRow(tagged.getKey(), amount)));
        }
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
