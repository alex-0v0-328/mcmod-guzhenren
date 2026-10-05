package net.alex.guzhenren.gameplay.aperture;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.alex.guzhenren.core.ModStreamCodecs;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One Aperture [空窍]: the vessel a cultivator awakens, and the thing that decides how much essence
 * fits. Leaf record nested inside {@link ApertureData}; immutable; thirteen active components hold rank,
 * stage, essence, paths, distilled essence, pressure, cultivation latches and the second-aperture flag.
 *
 * <p>⚠ {@code baseEssence} clamps to {@code [MIN_BASE, MAX_BASE]} (20..100) when positive; {@code 0} is
 * reserved for {@code NONE} and {@code 1..19} is a hole, not a value. ⚠ The two {@link GuPath} fields
 * are the ONLY active nullables in the data model ({@code writeNullableEnum}); the legacy physique carrier
 * is decode-only, never synced nor streamed. ⚠ {@code second} marks the aperture opened by a Second
 * Aperture Gu, not by Hope Gu -- the LIST position is the awakening order, the flag is the identity.
 * ⚠ The stream codec is handwritten (composite caps at 6).
 *
 * @author Alex
 * @version 1.0.0
 * @see ApertureData
 * @see ApertureService
 * @since 1.0.0
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
        @Nullable ExtremePhysique legacyExtremePhysique,
        boolean second
) {

    public static final int MIN_BASE = 20;
    public static final int MAX_BASE = 100;
    public static final int MAX_PRESSURE = 100;
    public static final int PRESSURE_COUNTDOWN_START = MAX_PRESSURE - 1;
    public static final int SECOND_BASE = 80;
    public static final Aperture NONE = new Aperture(
            Rank.NONE, Stage.NONE, 0, 0L, null, null, 0L, 0, 0L, 0, false, false, null, false);

    public Aperture(Rank rank, Stage stage, int baseEssence, long currentEssence,
                    @Nullable GuPath primaryPath, @Nullable GuPath secondaryPath, long distilledEssence,
                    int pressure, long pressureDeadlineTick, int nourishProgress, boolean petrified,
                    boolean distilling) {
        this(rank, stage, baseEssence, currentEssence, primaryPath, secondaryPath, distilledEssence, pressure,
                pressureDeadlineTick, nourishProgress, petrified, distilling, null, false);
    }

    private static final Codec<Aperture> CURRENT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
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
                    pressure, deadline, nourishProgress, petrified, distilling, null, second)));
    private static final Codec<Aperture> LEGACY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Rank.CODEC.optionalFieldOf("rank", Rank.NONE).forGetter(Aperture::rank),
            Stage.CODEC.optionalFieldOf("stage", Stage.NONE).forGetter(Aperture::stage),
            Codec.INT.optionalFieldOf("base_essence", 0).forGetter(Aperture::baseEssence),
            ExtremePhysique.CODEC.optionalFieldOf("extreme_physique", ExtremePhysique.NONE)
                    .forGetter(aperture -> aperture.legacyExtremePhysique() == null
                            ? ExtremePhysique.NONE : aperture.legacyExtremePhysique()),
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
            Codec.BOOL.optionalFieldOf("zombie_opened", false).forGetter(aperture -> false),
            Codec.BOOL.optionalFieldOf("second", false).forGetter(Aperture::second)
    ).apply(instance, (rank, stage, base, legacyPhysique, essence, primary, secondary, distilled, pressure, deadline,
                       nourishProgress, petrified, distilling, ignoredZombieOpened, second) ->
            new Aperture(rank, stage, base, essence, primary.orElse(null), secondary.orElse(null), distilled,
                    pressure, deadline, nourishProgress, petrified, distilling,
                    legacyPhysique == ExtremePhysique.NONE ? null : legacyPhysique, second)));
    private static final Decoder<Aperture> DECODER = new Decoder<>() {
        @Override
        public <T> DataResult<Pair<Aperture, T>> decode(DynamicOps<T> ops, T input) {
            return ops.getMap(input).flatMap(map -> {
                boolean legacy = map.get("extreme_physique") != null || map.get("zombie_opened") != null;
                return (legacy ? LEGACY_CODEC : CURRENT_CODEC).decode(ops, input);
            });
        }
    };
    public static final Codec<Aperture> CODEC = Codec.of(CURRENT_CODEC, DECODER);
    private static final StreamCodec<ByteBuf, Rank> RANK = ModStreamCodecs.ofEnum(Rank.class);
    private static final StreamCodec<ByteBuf, Stage> STAGE = ModStreamCodecs.ofEnum(Stage.class);
    public static final StreamCodec<ByteBuf, Aperture> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public @NotNull Aperture decode(@NotNull ByteBuf buf) {
            return new Aperture(
                    RANK.decode(buf),
                    STAGE.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf),
                    ModStreamCodecs.readNullableEnum(buf, GuPath.class),
                    ModStreamCodecs.readNullableEnum(buf, GuPath.class),
                    ByteBufCodecs.VAR_LONG.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf),
                    null,
                    ByteBufCodecs.BOOL.decode(buf));
        }

        @Override
        public void encode(@NotNull ByteBuf buf, @NotNull Aperture value) {
            RANK.encode(buf, value.rank());
            STAGE.encode(buf, value.stage());
            ByteBufCodecs.VAR_INT.encode(buf, value.baseEssence());
            ByteBufCodecs.VAR_LONG.encode(buf, value.currentEssence());
            ModStreamCodecs.writeNullableEnum(buf, value.primaryPath());
            ModStreamCodecs.writeNullableEnum(buf, value.secondaryPath());
            ByteBufCodecs.VAR_LONG.encode(buf, value.distilledEssence());
            ByteBufCodecs.VAR_INT.encode(buf, value.pressure());
            ByteBufCodecs.VAR_LONG.encode(buf, value.pressureDeadlineTick());
            ByteBufCodecs.VAR_INT.encode(buf, value.nourishProgress());
            ByteBufCodecs.BOOL.encode(buf, value.petrified());
            ByteBufCodecs.BOOL.encode(buf, value.distilling());
            ByteBufCodecs.BOOL.encode(buf, value.second());
        }
    };

    public Aperture {
        baseEssence = baseEssence <= 0 ? 0 : Math.clamp(baseEssence, MIN_BASE, MAX_BASE);
        currentEssence = Math.clamp(currentEssence, 0L, maxEssence(rank, stage, baseEssence));
        distilledEssence = Math.clamp(distilledEssence, 0L, maxEssence(rank, stage, baseEssence));
        pressure = Math.clamp(pressure, 0, MAX_PRESSURE);
        pressureDeadlineTick = pressure == PRESSURE_COUNTDOWN_START ? Math.max(0L, pressureDeadlineTick) : 0L;
        nourishProgress = Math.clamp(nourishProgress, 0, ApertureNourishData.FULL);
        if (secondaryPath != null && secondaryPath == primaryPath) secondaryPath = null;
    }

    public static Aperture opened() { return openedAt(Talent.randomPercent(Talent.randomTalent())); }

    public static Aperture openedAt(int baseEssence) {
        long max = maxEssence(Rank.ONE, Stage.INIT, baseEssence);
        return new Aperture(Rank.ONE, Stage.INIT, baseEssence, max, null, null, 0L, 0, 0L, 0, false, false);
    }

    public static Aperture secondOpened(Rank rank) {
        long max = maxEssence(rank, Stage.INIT, SECOND_BASE);
        return new Aperture(rank, Stage.INIT, SECOND_BASE, max, null, null, 0L, 0, 0L, 0, false, false,
                null, true);
    }

    public static long maxEssence(Rank rank, Stage stage, int base) {
        return Math.max(0L, (long) base * stage.getEssenceMultiplier() * rank.getRankBase());
    }

    public long maxEssence() { return maxEssence(rank, stage, baseEssence); }

    public Talent talent() { return Talent.fromPercent(baseEssence); }

    public Aperture refilled() { return withCurrentEssence(maxEssence()); }

    public Aperture withRank(Rank value) {
        return new Aperture(value, stage, baseEssence, currentEssence, primaryPath, secondaryPath, distilledEssence,
                pressure, pressureDeadlineTick, nourishProgress, petrified, distilling, legacyExtremePhysique, second);
    }

    public Aperture withStage(Stage value) {
        return new Aperture(rank, value, baseEssence, currentEssence, primaryPath, secondaryPath, distilledEssence,
                pressure, pressureDeadlineTick, nourishProgress, petrified, distilling, legacyExtremePhysique, second);
    }

    public Aperture withBaseEssence(int value) {
        return new Aperture(rank, stage, value, currentEssence, primaryPath, secondaryPath, distilledEssence,
                pressure, pressureDeadlineTick, nourishProgress, petrified, distilling, legacyExtremePhysique, second);
    }

    public Aperture withCurrentEssence(long value) {
        return new Aperture(rank, stage, baseEssence, value, primaryPath, secondaryPath, distilledEssence,
                pressure, pressureDeadlineTick, nourishProgress, petrified, distilling, legacyExtremePhysique, second);
    }

    public Aperture withPrimaryPath(@Nullable GuPath value) {
        return new Aperture(rank, stage, baseEssence, currentEssence, value, secondaryPath, distilledEssence,
                pressure, pressureDeadlineTick, nourishProgress, petrified, distilling, legacyExtremePhysique, second);
    }

    public Aperture withSecondaryPath(@Nullable GuPath value) {
        return new Aperture(rank, stage, baseEssence, currentEssence, primaryPath, value, distilledEssence,
                pressure, pressureDeadlineTick, nourishProgress, petrified, distilling, legacyExtremePhysique, second);
    }

    public Aperture withDistilledEssence(long value) {
        return new Aperture(rank, stage, baseEssence, currentEssence, primaryPath, secondaryPath, value,
                pressure, pressureDeadlineTick, nourishProgress, petrified, distilling, legacyExtremePhysique, second);
    }

    public Aperture withPressure(int value) {
        return new Aperture(rank, stage, baseEssence, currentEssence, primaryPath, secondaryPath, distilledEssence,
                value, 0L, nourishProgress, petrified, distilling, legacyExtremePhysique, second);
    }

    public Aperture withPressureAndDeadline(int value, long deadline) {
        return new Aperture(rank, stage, baseEssence, currentEssence, primaryPath, secondaryPath, distilledEssence,
                value, deadline, nourishProgress, petrified, distilling, legacyExtremePhysique, second);
    }

    public Aperture withNourishProgress(int value) {
        return new Aperture(rank, stage, baseEssence, currentEssence, primaryPath, secondaryPath, distilledEssence,
                pressure, pressureDeadlineTick, value, petrified, distilling, legacyExtremePhysique, second);
    }

    public Aperture withPetrified(boolean value) {
        return new Aperture(rank, stage, baseEssence, currentEssence, primaryPath, secondaryPath, distilledEssence,
                pressure, pressureDeadlineTick, nourishProgress, value, distilling, legacyExtremePhysique, second);
    }

    public Aperture withDistilling(boolean value) {
        return new Aperture(rank, stage, baseEssence, currentEssence, primaryPath, secondaryPath, distilledEssence,
                pressure, pressureDeadlineTick, nourishProgress, petrified, value, legacyExtremePhysique, second);
    }

    public Aperture withSecond(boolean value) {
        return new Aperture(rank, stage, baseEssence, currentEssence, primaryPath, secondaryPath, distilledEssence,
                pressure, pressureDeadlineTick, nourishProgress, petrified, distilling, legacyExtremePhysique, value);
    }

    public Aperture clearLegacyExtremePhysique() {
        return new Aperture(rank, stage, baseEssence, currentEssence, primaryPath, secondaryPath, distilledEssence,
                pressure, pressureDeadlineTick, nourishProgress, petrified, distilling, null, second);
    }
}
