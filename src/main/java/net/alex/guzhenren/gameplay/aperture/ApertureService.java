package net.alex.guzhenren.gameplay.aperture;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.ModEvents;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.gameplay.path.PathMarks;
import net.alex.guzhenren.gameplay.path.EffectCarrier;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.path.PathEffect;
import net.alex.guzhenren.gameplay.path.PathEffectService;
import net.alex.guzhenren.registry.effect.ModPathEffectTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 空窍[Aperture]附件唯一运行时写入方：开窍[Awakening]、转数、阶段、资质[Aptitude]、道[Path]。
 *
 * <ul>
 *   <li>静态服务；多数写入经 {@code store} 发布 {@link ApertureChangedEvent}（最大生命值修饰符、Epic Fight 耐力刷
 *       新），{@link AperturePressureService} 的压力写入绕过它。</li>
 *   <li>⚠ 体质与基础真元[Essence]的不变量在此强制（{@code enforce}），具体体质与资质授予在 {@code BodyService}。</li>
 *   <li>⚠ {@code awaken} 不拒已开窍者而追加，调用方把关；基础为 0（希望蛊[Hope Gu]未掷）重掷资质，免开零基础窍。</li>
 *   <li>{@link #getStatus(Player, int)} 是 {@link ApertureStatus} 唯一推导：僵尸、临时僵尸化与石化空窍为
 *       {@code DEAD}，其余 {@code NORMAL}。</li>
 *   <li>⚠ {@code reconcileTalentPaths}（十绝天赋效果）是两条跨领域授予之一，出现第三条即抽出协调者；天赋是
 *       {@link #TALENT_SOURCE} 给的永久 {@code EXTREME_PHYSIQUE} 空窍效果，整 1000 条道痕[Path Marks]均分到体质各天赋
 *       道，登录时缺失或体质不符由 {@link #syncTalentEffect} 补回。</li>
 *   <li>肉身存入不同十绝体时，{@link #onExtremePhysiqueChanged} 经 {@link ApertureEvents} 执行它，再对齐第一空窍基础真
 *       元：有体质为满值，无则满值减一并清空压力。</li>
 *   <li>第一空窍资质设为十绝时像开窍一样掷体质（满基础需要体质）；设得更低先摘体质，免得重掷基础被强制回满。</li>
 *   <li>{@link #openSecond} 是第二空窍唯一开启方：甲等八成、本转初阶、满真元池；无空窍也能开，孤立第二空窍即整个列
 *       表，直到希望蛊在其前插入第一空窍。</li>
 *   <li>更高转第二空窍蛊[Second Aperture Gu]覆盖已有第二空窍并回到初阶，绑定的道保留，持有它们的本命蛊在存储里不动；希
 *       望蛊插入的第一空窍存下后发布 {@link FirstApertureInsertedEvent}，按空窍下标的存储随之移位。</li>
 * </ul>
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
    public static final ResourceLocation TALENT_SOURCE = Guzhenren.id("innate/extreme_physique");

    static long getTalentMarksPerPath(ExtremePhysique physique) {
        int paths = physique.getTalentPaths().size();
        return paths == 0 ? 0L : TALENT_MARK_TOTAL / paths;
    }

    public static @NotNull PathEffect<ExtremePhysique> getTalentEffect(@NotNull ExtremePhysique physique) {
        Map<GuPath, PathMarks> paths = new EnumMap<>(GuPath.class);
        PathMarks each = PathMarks.ofMarks(getTalentMarksPerPath(physique));
        for (GuPath path : physique.getTalentPaths()) paths.put(path, each);
        return PathEffect.of(ModPathEffectTypes.EXTREME_PHYSIQUE, EffectCarrier.APERTURE, TALENT_SOURCE, paths,
                PathEffect.PERMANENT, physique);
    }

    public static void syncTalentEffect(@NotNull ServerPlayer player) {
        ExtremePhysique current = BodyService.getExtremePhysique(player);
        PathEffectService.sync(player, ModPathEffectTypes.EXTREME_PHYSIQUE,
                current.getTalentPaths().isEmpty() ? null : getTalentEffect(current));
    }

    public static @NotNull ApertureData get(@NotNull Player player) { return PlayerData.get(player).getAperture(); }

    public static @NotNull Aperture getAperture(@NotNull Player player) { return get(player).getPrimary(); }

    public static @NotNull Aperture getAperture(@NotNull Player player, int i) { return get(player).get(i); }

    public static boolean isAwakened(@NotNull Player player) { return get(player).isAwakened(); }

    public static boolean hasAperture(@NotNull Player player) { return get(player).hasAperture(); }

    public static @NotNull ApertureStatus getStatus(@NotNull Player player, int index) {
        Aperture aperture = getAperture(player, index);
        if (BodyService.isUndead(player) || aperture.petrified()) return ApertureStatus.DEAD;
        return ApertureStatus.NORMAL;
    }

    public static @NotNull ApertureStatus getStatus(@NotNull Player player) {
        return getStatus(player, ApertureData.PRIMARY);
    }

    public static @NotNull Talent getTalent(@NotNull Player player) { return getAperture(player).getTalent(); }

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

    public static void setTalent(@NotNull ServerPlayer player, int index, @NotNull Talent value) {
        if (value == Talent.TEN_EXTREMES && index == get(player).getFirstIndex()) {
            if (!BodyService.isExtreme(player)) {
                BodyService.setExtremePhysique(player, ExtremePhysique.randomTenExtreme());
            }
            return;
        }
        if (index == get(player).getFirstIndex() && BodyService.isExtreme(player)) {
            BodyService.setExtremePhysique(player, ExtremePhysique.NONE);
        }
        setBaseEssence(player, index, Talent.randomPercent(value));
    }

    public static void setPrimaryPath(@NotNull ServerPlayer player, int index, @Nullable GuPath value) {
        Aperture aperture = getAperture(player, index);
        if (aperture.primaryPath() == value) return;
        set(player, index, aperture.withPrimaryPath(value));
    }

    public static int inheritVital(@NotNull ServerPlayer player, @NotNull List<Integer> eatenKinds,
                                   @Nullable GuPath path) {
        ApertureData data = get(player);
        int heir = -1;
        for (int kind : eatenKinds) {
            int index = data.indexOf(kind);
            if (heir < 0) heir = index;
            else if (index >= 0 && index != heir) setPrimaryPath(player, index, null);
        }
        if (heir >= 0) setPrimaryPath(player, heir, path);
        return heir;
    }

    public static void setSecondaryPath(@NotNull ServerPlayer player, int index, @Nullable GuPath value) {
        Aperture aperture = getAperture(player, index);
        if (aperture.secondaryPath() == value) return;
        set(player, index, aperture.withSecondaryPath(value));
    }

    public static void shiftStage(@NotNull ServerPlayer player, int delta) {
        shiftStage(player, ApertureData.PRIMARY, delta);
    }

    public static void shiftStage(@NotNull ServerPlayer player, int index, int delta) {
        setStage(player, index, getAperture(player, index).stage().shift(delta));
    }

    public static void setBaseEssence(@NotNull ServerPlayer player, int value) {
        setBaseEssence(player, ApertureData.PRIMARY, value);
    }

    public static boolean setBaseEssence(@NotNull ServerPlayer player, int index, int value) {
        int clamped = Mth.clamp(value, Aperture.MIN_BASE, Aperture.MAX_BASE);
        return set(player, index, getAperture(player, index).withBaseEssence(clamped));
    }

    public static void awaken(@NotNull ServerPlayer player) {
        open(player, Aperture.createOpened(), ExtremePhysique.NONE);
    }

    public static void awaken(@NotNull ServerPlayer player, int baseEssence) {
        awaken(player, baseEssence, ExtremePhysique.NONE);
    }

    /** {@code tenExtreme} 是十绝天资空窍[Aperture]带来的体质；为 {@code NONE} 时随机掷一个。 */
    public static void awaken(@NotNull ServerPlayer player, int baseEssence, @NotNull ExtremePhysique tenExtreme) {
        open(player, baseEssence > 0 ? Aperture.createOpenedAt(baseEssence) : Aperture.createOpened(), tenExtreme);
    }

    public static void openSecond(@NotNull ServerPlayer player, @NotNull Rank rank) {
        ApertureData data = get(player);
        Aperture opened = Aperture.createSecond(rank);
        int index = data.getSecondIndex();
        if (index < 0) {
            store(player, data.withOpened(opened));
            return;
        }
        Aperture old = data.get(index);
        set(player, index, opened.withPrimaryPath(old.primaryPath()).withSecondaryPath(old.secondaryPath()));
    }

    private static void open(ServerPlayer player, Aperture aperture, ExtremePhysique tenExtreme) {
        ApertureData data = get(player);
        if (data.isFull()) return;
        if (data.count() == 1 && data.get(0).second()) {
            store(player, data.insertFirst(aperture));
            ModEvents.post(new FirstApertureInsertedEvent(player));
            ApertureNourishService.shiftTargetForInsertedFirst(player);
        } else {
            store(player, data.withOpened(aperture));
        }
        if (aperture.getTalent() == Talent.TEN_EXTREMES) {
            BodyService.setExtremePhysique(player,
                    tenExtreme != ExtremePhysique.NONE ? tenExtreme : ExtremePhysique.randomTenExtreme());
        }
    }

    public static boolean set(@NotNull ServerPlayer player, int index, @NotNull Aperture aperture) {
        if (index == get(player).getFirstIndex() && aperture.baseEssence() == Aperture.MAX_BASE
                && !BodyService.isExtreme(player)) return false;
        store(player, get(player).with(index, enforce(player, index, aperture)));
        return true;
    }

    private static void store(ServerPlayer player, ApertureData data) {
        PlayerData.get(player).setAperture(data);
        ModEvents.post(new ApertureChangedEvent(player));
    }

    private static Aperture enforce(@NotNull Player player, int index, @NotNull Aperture aperture) {
        if (index == get(player).getFirstIndex() && BodyService.isExtreme(player)) {
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
        if (!before.getTalentPaths().isEmpty()) {
            PathEffectService.removeType(player, ModPathEffectTypes.EXTREME_PHYSIQUE);
        }
        if (!after.getTalentPaths().isEmpty()) PathEffectService.apply(player, getTalentEffect(after));
    }
}
