package net.alex.guzhenren.gameplay.aperture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.alex.guzhenren.core.ModByteBufs;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 单个空窍的不可变数据记录，嵌于 {@link ApertureData}，决定真元容量并承载修为与状态。
 * <ul>
 *   <li>⚠ {@code baseEssence} 为正时夹取到 {@code [MIN_BASE, MAX_BASE]}（20 至 100）；{@code 0} 专属于
 *       {@link #NONE}，{@code 1} 至 {@code 19} 是无意义的空洞区间而非合法取值。</li>
 *   <li>⚠ 两个 {@link GuPath} 是仅有的可空字段（网络用 {@code writeNullableEnum}），{@link #encode} 与
 *       {@link #decode} 为手写实现；兼修流派与主修相同时在紧凑构造器中自动清空。</li>
 *   <li>⚠ {@code second} 标记该空窍由第二空窍蛊[Second Aperture Gu]开启（而非希望蛊[Hope Gu]）：列表下标仅反映当前
 *       排位，此标记才是区分第一、第二空窍的身份依据。</li>
 * </ul>
 */

public record Aperture(
        Rank rank,
        Stage stage,
        int baseEssence,
        long currentEssence,
        @Nullable GuPath primaryPath,
        @Nullable GuPath secondaryPath,
        long distilledEssence,
        int pressure,
        long pressureDeadlineTick,
        int nourishProgress,
        boolean petrified,
        boolean distilling,
        boolean second
) {

    public static final int MIN_BASE = 20;
    public static final int MAX_BASE = 100;
    public static final int MAX_PRESSURE = 100;
    public static final int PRESSURE_COUNTDOWN_START = MAX_PRESSURE - 1;
    public static final int SECOND_BASE = 80;
    public static final Aperture NONE = new Aperture(
            Rank.NONE, Stage.NONE, 0, 0L, null, null, 0L, 0, 0L, 0, false, false, false);
    public static final Codec<Aperture> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Rank.CODEC.optionalFieldOf("rank", Rank.NONE).forGetter(Aperture::rank),
            Stage.CODEC.optionalFieldOf("stage", Stage.NONE).forGetter(Aperture::stage),
            Codec.INT.optionalFieldOf("base_essence", 0).forGetter(Aperture::baseEssence),
            Codec.LONG.optionalFieldOf("current_essence", 0L).forGetter(Aperture::currentEssence),
            GuPath.CODEC.optionalFieldOf("primary_path")
                    .forGetter(aperture -> Optional.ofNullable(aperture.primaryPath())),
            GuPath.CODEC.optionalFieldOf("secondary_path")
                    .forGetter(aperture -> Optional.ofNullable(aperture.secondaryPath())),
            Codec.LONG.optionalFieldOf("distilled_essence", 0L).forGetter(Aperture::distilledEssence),
            Codec.INT.optionalFieldOf("pressure", 0).forGetter(Aperture::pressure),
            Codec.LONG.optionalFieldOf("pressure_deadline_tick", 0L).forGetter(Aperture::pressureDeadlineTick),
            Codec.INT.optionalFieldOf("nourish_progress", 0).forGetter(Aperture::nourishProgress),
            Codec.BOOL.optionalFieldOf("petrified", false).forGetter(Aperture::petrified),
            Codec.BOOL.optionalFieldOf("distilling", false).forGetter(Aperture::distilling),
            Codec.BOOL.optionalFieldOf("second", false).forGetter(Aperture::second)
    ).apply(instance, (rank, stage, base, essence, primary, secondary, distilled, pressure, deadline,
                       nourishProgress, petrified, distilling, second) ->
            new Aperture(rank, stage, base, essence, primary.orElse(null), secondary.orElse(null), distilled,
                    pressure, deadline, nourishProgress, petrified, distilling, second)));
    public static @NotNull Aperture decode(@NotNull FriendlyByteBuf buf) {
        return new Aperture(
                ModByteBufs.readEnum(buf, Rank.class),
                ModByteBufs.readEnum(buf, Stage.class),
                buf.readVarInt(),
                buf.readVarLong(),
                ModByteBufs.readNullableEnum(buf, GuPath.class),
                ModByteBufs.readNullableEnum(buf, GuPath.class),
                buf.readVarLong(),
                buf.readVarInt(),
                buf.readVarLong(),
                buf.readVarInt(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean());
    }

    public static void encode(@NotNull FriendlyByteBuf buf, @NotNull Aperture value) {
        ModByteBufs.writeEnum(buf, value.rank());
        ModByteBufs.writeEnum(buf, value.stage());
        buf.writeVarInt(value.baseEssence());
        buf.writeVarLong(value.currentEssence());
        ModByteBufs.writeNullableEnum(buf, value.primaryPath());
        ModByteBufs.writeNullableEnum(buf, value.secondaryPath());
        buf.writeVarLong(value.distilledEssence());
        buf.writeVarInt(value.pressure());
        buf.writeVarLong(value.pressureDeadlineTick());
        buf.writeVarInt(value.nourishProgress());
        buf.writeBoolean(value.petrified());
        buf.writeBoolean(value.distilling());
        buf.writeBoolean(value.second());
    }

    public Aperture {
        baseEssence = baseEssence <= 0 ? 0 : Mth.clamp(baseEssence, MIN_BASE, MAX_BASE);
        currentEssence = Math.clamp(currentEssence, 0L, computeMaxEssence(rank, stage, baseEssence));
        distilledEssence = Math.clamp(distilledEssence, 0L, computeMaxEssence(rank, stage, baseEssence));
        pressure = Mth.clamp(pressure, 0, MAX_PRESSURE);
        pressureDeadlineTick = pressure == PRESSURE_COUNTDOWN_START ? Math.max(0L, pressureDeadlineTick) : 0L;
        nourishProgress = Mth.clamp(nourishProgress, 0, ApertureNourishData.FULL);
        if (secondaryPath != null && secondaryPath == primaryPath) secondaryPath = null;
    }

    public static Aperture createOpened() { return createOpenedAt(Talent.randomPercent(Talent.randomTalent())); }

    public static Aperture createOpenedAt(int baseEssence) {
        long max = computeMaxEssence(Rank.ONE, Stage.INITIAL, baseEssence);
        return new Aperture(Rank.ONE, Stage.INITIAL, baseEssence, max, null, null, 0L, 0, 0L, 0, false, false, false);
    }

    public static Aperture createSecond(Rank rank) {
        long max = computeMaxEssence(rank, Stage.INITIAL, SECOND_BASE);
        return new Aperture(rank, Stage.INITIAL, SECOND_BASE, max, null, null, 0L, 0, 0L, 0, false, false, true);
    }

    public static long computeMaxEssence(Rank rank, Stage stage, int base) {
        return Math.max(0L, (long) base * stage.getEssenceMultiplier() * rank.getRankBase());
    }

    public long getMaxEssence() { return computeMaxEssence(rank, stage, baseEssence); }

    public Talent getTalent() { return Talent.fromPercent(baseEssence); }

    public Aperture withFullEssence() { return withCurrentEssence(getMaxEssence()); }

    public Aperture withRank(Rank value) {
        return new Aperture(value, stage, baseEssence, currentEssence, primaryPath, secondaryPath, distilledEssence,
                pressure, pressureDeadlineTick, nourishProgress, petrified, distilling, second);
    }

    public Aperture withStage(Stage value) {
        return new Aperture(rank, value, baseEssence, currentEssence, primaryPath, secondaryPath, distilledEssence,
                pressure, pressureDeadlineTick, nourishProgress, petrified, distilling, second);
    }

    public Aperture withBaseEssence(int value) {
        return new Aperture(rank, stage, value, currentEssence, primaryPath, secondaryPath, distilledEssence,
                pressure, pressureDeadlineTick, nourishProgress, petrified, distilling, second);
    }

    public Aperture withCurrentEssence(long value) {
        return new Aperture(rank, stage, baseEssence, value, primaryPath, secondaryPath, distilledEssence,
                pressure, pressureDeadlineTick, nourishProgress, petrified, distilling, second);
    }

    public Aperture withPrimaryPath(@Nullable GuPath value) {
        return new Aperture(rank, stage, baseEssence, currentEssence, value, secondaryPath, distilledEssence,
                pressure, pressureDeadlineTick, nourishProgress, petrified, distilling, second);
    }

    public Aperture withSecondaryPath(@Nullable GuPath value) {
        return new Aperture(rank, stage, baseEssence, currentEssence, primaryPath, value, distilledEssence,
                pressure, pressureDeadlineTick, nourishProgress, petrified, distilling, second);
    }

    public Aperture withDistilledEssence(long value) {
        return new Aperture(rank, stage, baseEssence, currentEssence, primaryPath, secondaryPath, value,
                pressure, pressureDeadlineTick, nourishProgress, petrified, distilling, second);
    }

    public Aperture withPressure(int value) {
        return new Aperture(rank, stage, baseEssence, currentEssence, primaryPath, secondaryPath, distilledEssence,
                value, 0L, nourishProgress, petrified, distilling, second);
    }

    public Aperture withPressureAndDeadline(int value, long deadline) {
        return new Aperture(rank, stage, baseEssence, currentEssence, primaryPath, secondaryPath, distilledEssence,
                value, deadline, nourishProgress, petrified, distilling, second);
    }

    public Aperture withNourishProgress(int value) {
        return new Aperture(rank, stage, baseEssence, currentEssence, primaryPath, secondaryPath, distilledEssence,
                pressure, pressureDeadlineTick, value, petrified, distilling, second);
    }

    public Aperture withPetrified(boolean value) {
        return new Aperture(rank, stage, baseEssence, currentEssence, primaryPath, secondaryPath, distilledEssence,
                pressure, pressureDeadlineTick, nourishProgress, value, distilling, second);
    }

    public Aperture withDistilling(boolean value) {
        return new Aperture(rank, stage, baseEssence, currentEssence, primaryPath, secondaryPath, distilledEssence,
                pressure, pressureDeadlineTick, nourishProgress, petrified, value, second);
    }

    public Aperture withSecond(boolean value) {
        return new Aperture(rank, stage, baseEssence, currentEssence, primaryPath, secondaryPath, distilledEssence,
                pressure, pressureDeadlineTick, nourishProgress, petrified, distilling, value);
    }
}
