package net.alex.guzhenren.gameplay.aperture;

import java.util.List;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorageService;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.path.MarkTag;
import net.alex.guzhenren.gameplay.path.PathService;
import net.alex.guzhenren.gameplay.path.qi.PathQiService;
import net.alex.guzhenren.gameplay.path.qi.QiKind;
import net.alex.guzhenren.registry.attachment.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The only runtime writer of the Aperture [空窍] attachment: awakening [开窍], rank [转数], stage [阶段], talent
 * [资质] and paths [流派]. Static service; most writes route through {@code store}, which posts an
 * {@link ApertureChangedEvent} (the max-health modifier and Epic Fight's stamina refresh); pressure
 * writes, in {@link AperturePressureService}, skip it.
 *
 * <p>⚠ The body-physique/base-essence invariant is enforced here ({@code enforce}); the concrete
 * physique and talent grant live in {@code BodyService}. ⚠ {@code awaken} does NOT refuse an awakened
 * holder -- it appends; the caller gates. ⚠ {@code reconcileTalentPaths} (ten-extreme Dao marks plus
 * human qi) is one of the two cross-domain grants; a third would trigger extracting a coordinator.
 * {@link #onExtremePhysiqueChanged} runs it, through {@link ApertureEvents}, whenever the body stores a
 * different ten-extreme physique, then resets the first aperture's base essence to match -- the full
 * base for a physique, one below for none, which also empties the pressure.
 *
 * <p>{@link #getStatus(Player, int)} is the one derivation of {@link ApertureStatus}: Zombie,
 * Half-Zombie and petrified apertures are DEAD; every other aperture is NORMAL.
 *
 * <p>{@link #openSecond} is the only opener of a second aperture: Grade-A at 8/10, this rank's
 * first stage and a full pool. Works with NO aperture at all -- the lone second aperture then IS the
 * whole list until Hope Gu inserts the first one ahead of it. A higher-rank Second Aperture Gu
 * overwrites what is already there, back to the first stage, while the bound paths stay -- the Vital
 * Gu holding them is untouched in storage.
 *
 * @author Alex
 * @version 1.0.0
 * @see ApertureData
 * @see ApertureEssenceService
 * @since 1.0.0
 */

public final class ApertureService {

    private ApertureService() {}

    public static final long TALENT_MARK_TOTAL = 1000L;
    public static final long TALENT_HUMAN_QI = 100L;

    static long getTalentMarksPerPath(ExtremePhysique physique) {
        int paths = physique.getTalentPaths().size();
        return paths == 0 ? 0L : TALENT_MARK_TOTAL / paths;
    }

    public static void syncTalentMarks(@NotNull ServerPlayer player) {
        ExtremePhysique current = BodyService.getExtremePhysique(player);
        for (ExtremePhysique physique : ExtremePhysique.values()) {
            long expected = physique == current ? getTalentMarksPerPath(physique) : 0L;
            for (GuPath path : physique.getTalentPaths()) {
                if (PathService.getMark(player, path, MarkTag.EXTREME_PHYSIQUE) != expected) {
                    PathService.setMark(player, path, MarkTag.EXTREME_PHYSIQUE, expected);
                }
            }
        }
    }

    public static @NotNull ApertureData get(@NotNull Player player) { return player.getData(ModAttachments.APERTURE); }

    public static @NotNull Aperture getAperture(@NotNull Player player) { return get(player).primary(); }

    public static @NotNull Aperture getAperture(@NotNull Player player, int i) { return get(player).get(i); }

    public static boolean isAwakened(@NotNull Player player) { return get(player).isAwakened(); }

    public static boolean hasAperture(@NotNull Player player) { return get(player).hasAperture(); }

    public static @NotNull ApertureStatus getStatus(@NotNull Player player, int index) {
        Aperture aperture = getAperture(player, index);
        if (BodyService.isZombieOrHalfZombie(player) || aperture.petrified()) return ApertureStatus.DEAD;
        return ApertureStatus.NORMAL;
    }

    public static @NotNull ApertureStatus getStatus(@NotNull Player player) {
        return getStatus(player, ApertureData.PRIMARY);
    }

    public static @NotNull Talent getTalent(@NotNull Player player) { return getAperture(player).talent(); }

    public static @NotNull Rank getRank(@NotNull Player player) { return getAperture(player).rank(); }

    public static @NotNull Rank getHealthRank(@NotNull Player player) {
        return get(player).isAwakened() ? getAperture(player).rank() : Rank.NONE;
    }

    public static @NotNull Stage getStage(@NotNull Player player) { return getAperture(player).stage(); }

    public static void setRank(@NotNull ServerPlayer player, @NotNull Rank value) {
        setRank(player, ApertureData.PRIMARY, value);
    }

    public static void setRank(@NotNull ServerPlayer player, int index, @NotNull Rank value) {
        set(player, index, getAperture(player, index).withRank(value));
    }

    public static void setStage(@NotNull ServerPlayer player, @NotNull Stage value) {
        setStage(player, ApertureData.PRIMARY, value);
    }

    public static void setStage(@NotNull ServerPlayer player, int index, @NotNull Stage value) {
        set(player, index, getAperture(player, index).withStage(value));
    }

    public static void addBaseEssence(@NotNull ServerPlayer player, int index, int delta) {
        setBaseEssence(player, index, getAperture(player, index).baseEssence() + delta);
    }

    public static void setTalent(@NotNull ServerPlayer player, int index, @NotNull Talent value) {
        setBaseEssence(player, index, Talent.randomPercent(value));
    }

    public static void setPrimaryPath(@NotNull ServerPlayer player, int index, @Nullable GuPath value) {
        Aperture aperture = getAperture(player, index);
        if (aperture.primaryPath() == value) return;
        set(player, index, aperture.withPrimaryPath(value));
    }

    public static void setSecondaryPath(@NotNull ServerPlayer player, int index, @Nullable GuPath value) {
        Aperture aperture = getAperture(player, index);
        if (aperture.secondaryPath() == value) return;
        set(player, index, aperture.withSecondaryPath(value));
    }

    public static void shiftRank(@NotNull ServerPlayer player, int index, int delta) {
        setRank(player, index, getAperture(player, index).rank().shift(delta));
    }

    public static void shiftStage(@NotNull ServerPlayer player, int delta) {
        shiftStage(player, ApertureData.PRIMARY, delta);
    }

    public static void shiftStage(@NotNull ServerPlayer player, int index, int delta) {
        setStage(player, index, getAperture(player, index).stage().shift(delta));
    }

    public static void shiftTalent(@NotNull ServerPlayer player, int index, int delta) {
        setTalent(player, index, getAperture(player, index).talent().shift(delta));
    }

    public static void setBaseEssence(@NotNull ServerPlayer player, int value) {
        setBaseEssence(player, ApertureData.PRIMARY, value);
    }

    public static boolean setBaseEssence(@NotNull ServerPlayer player, int index, int value) {
        int clamped = Math.clamp(value, Aperture.MIN_BASE, Aperture.MAX_BASE);
        return set(player, index, getAperture(player, index).withBaseEssence(clamped));
    }

    public static void awaken(@NotNull ServerPlayer player) { open(player, Aperture.opened()); }

    public static void awaken(@NotNull ServerPlayer player, int baseEssence) {
        open(player, Aperture.openedAt(baseEssence));
    }

    public static void openSecond(@NotNull ServerPlayer player, @NotNull Rank rank) {
        ApertureData data = get(player);
        Aperture opened = Aperture.secondOpened(rank);
        int index = data.secondIndex();
        if (index < 0) {
            store(player, data.opened(opened));
            return;
        }
        Aperture old = data.get(index);
        set(player, index, opened.withPrimaryPath(old.primaryPath()).withSecondaryPath(old.secondaryPath()));
    }

    private static void open(ServerPlayer player, Aperture aperture) {
        ApertureData data = get(player);
        if (data.isFull()) return;
        if (data.count() == 1 && data.get(0).second()) {
            store(player, data.insertFirst(aperture));
            ApertureStorageService.shiftForFirstAperture(player);
            ApertureNourishService.shiftTargetForInsertedFirst(player);
        } else {
            store(player, data.opened(aperture));
        }
        if (aperture.talent() == Talent.TEN_EXTREMES) {
            BodyService.setExtremePhysique(player, ExtremePhysique.randomTenExtreme());
        }
    }

    public static boolean set(@NotNull ServerPlayer player, int index, @NotNull Aperture aperture) {
        if (index == get(player).firstIndex() && aperture.baseEssence() == Aperture.MAX_BASE
                && !BodyService.isExtreme(player)) return false;
        store(player, get(player).with(index, enforce(player, index, aperture)));
        return true;
    }

    private static void store(ServerPlayer player, ApertureData data) {
        player.setData(ModAttachments.APERTURE, data);
        NeoForge.EVENT_BUS.post(new ApertureChangedEvent(player));
    }

    private static Aperture enforce(@NotNull Player player, int index, @NotNull Aperture aperture) {
        if (index == get(player).firstIndex() && BodyService.isExtreme(player)) {
            return aperture.baseEssence() == Aperture.MAX_BASE ? aperture : aperture.withBaseEssence(Aperture.MAX_BASE);
        }
        return capBelowMaxBase(aperture);
    }

    private static Aperture capBelowMaxBase(Aperture aperture) {
        return aperture.baseEssence() == Aperture.MAX_BASE
                ? aperture.withBaseEssence(Aperture.MAX_BASE - 1).withPressure(0) : aperture;
    }

    public static void onExtremePhysiqueChanged(@NotNull ServerPlayer player, @NotNull ExtremePhysique before,
                                                @NotNull ExtremePhysique after) {
        reconcileTalentPaths(player, before, after);
        if (!isAwakened(player)) return;

        int base = after == ExtremePhysique.NONE ? Aperture.MAX_BASE - 1 : Aperture.MAX_BASE;
        Aperture updated = getAperture(player).withBaseEssence(base);
        if (after == ExtremePhysique.NONE) updated = updated.withPressure(0);
        set(player, ApertureData.PRIMARY, updated);
    }

    //    TODO(refactor): extract a coordinator once cross-domain grant rules reach 3; TWO exist today.
    private static void reconcileTalentPaths(ServerPlayer player, ExtremePhysique before, ExtremePhysique after) {
        if (before == after) return;
        grantTalentPaths(player, before, -1);
        grantTalentPaths(player, after, 1);
    }

    private static void grantTalentPaths(ServerPlayer player, ExtremePhysique physique, int sign) {
        List<GuPath> paths = physique.getTalentPaths();
        if (paths.isEmpty()) return;

        PathQiService.add(player, QiKind.HUMAN, sign * TALENT_HUMAN_QI);
        long marks = sign * getTalentMarksPerPath(physique);
        for (GuPath path : paths) PathService.addMark(player, path, MarkTag.EXTREME_PHYSIQUE, marks);
    }
}
