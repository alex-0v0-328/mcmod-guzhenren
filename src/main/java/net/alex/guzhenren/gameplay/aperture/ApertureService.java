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
 * 玩家空窍数据（{@link ApertureData}）的唯一运行时写入入口：管理开窍[Awakening]、第二空窍、转数、阶段、资质与流派。
 * <ul>
 *   <li>除 {@link AperturePressureService} 直接写压力外，其余变更均经 {@code store} 落盘并发布
 *       {@link ApertureChangedEvent}（触发最大生命值修饰符与 Epic Fight 耐力刷新）。</li>
 *   <li>{@link #getStatus(Player, int)} 是推导 {@link ApertureStatus} 的唯一入口：死灵体质
 *       （{@code BodyService.isUndead}，含僵尸与临时僵尸化）或已石化的空窍返回 {@code DEAD}，其余返回
 *       {@code NORMAL}。</li>
 *   <li>⚠ 第一空窍的满值基础真元（{@code Aperture.MAX_BASE}，即十绝天资）与身怀十绝体[Ten Extreme Physiques]双向绑定：
 *       {@link #set} 拒绝为非十绝体写入满基础真元，{@code enforce} 则将十绝体玩家的第一空窍强制对齐为满值、非十绝体压至
 *       {@code MAX_BASE - 1} 并清空压力。</li>
 *   <li>{@link #setTalent} 将第一空窍设为十绝天资时，若尚无十绝体则由 {@code BodyService} 随机赋予一种；设为更低档时先
 *       摘除十绝体再重掷基础真元，防止重掷结果被 {@code enforce} 强制拉回满值。</li>
 *   <li>⚠ 肉身十绝体变更时，{@link ApertureEvents} 调 {@link #onExtremePhysiqueChanged} 对齐第一空窍基础真元（有体质设
 *       为满值，无则降为满值减一并清空压力），并经 {@code reconcileTalentPaths} 更新 {@link #TALENT_SOURCE} 的永久
 *       {@code EXTREME_PHYSIQUE} 空窍流派效果（将 1000 条道痕[Path Marks]均分至该体质各天赋流派，登录缺失或体质不符时由
 *       {@link #syncTalentEffect} 补齐；此为现有两处跨领域授予之一，满三处时抽出协调器）。</li>
 *   <li>⚠ {@link #awaken} 不校验玩家是否已开窍（由调用方把关，仅在空窍槽已满时忽略）：{@code baseEssence <= 0}（
 *       希望蛊[Hope Gu]未预掷）时重掷资质以免开出零基础空窍；开出十绝天资时按 {@code tenExtreme} 赋予十绝体（
 *       {@code NONE} 则随机掷取）。</li>
 *   <li>{@link #openSecond} 是开启或覆盖第二空窍的唯一入口（固定甲等八成、本转初阶、满真元池）：未开第一空窍时也可单独
 *       开启并暂居下标 {@code 0}；后续希望蛊开启第一空窍时会插入到下标 {@code 0}，发布
 *       {@link FirstApertureInsertedEvent} 并后移温养目标下标。更高转第二空窍蛊[Second Aperture Gu]覆盖已有第二空窍时重
 *       置为初阶，保留已绑定的主修与兼修流派（存储中的本命蛊保持原位）。</li>
 * </ul>
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
