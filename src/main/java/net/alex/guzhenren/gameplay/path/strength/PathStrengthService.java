package net.alex.guzhenren.gameplay.path.strength;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.NamedEnum;
import net.alex.guzhenren.core.ModEvents;
import net.alex.guzhenren.gameplay.path.EffectCarrier;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.path.PathEffect;
import net.alex.guzhenren.gameplay.path.PathEffectService;
import net.alex.guzhenren.gameplay.path.PathEffectType;
import net.alex.guzhenren.gameplay.path.PathMarks;
import net.alex.guzhenren.gameplay.path.timed.TimedBuff;
import net.alex.guzhenren.registry.effect.ModPathEffectTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 永久力道积累（兽力[Beast Strength]与人力层数）的唯一写入与查询服务。
 * <ul>
 *   <li>作为永久兽力与永久人力两类肉身流派效果的唯一写入方，流派效果本身即唯一持久化载体（每种兽力一个效果、
 *       每档人力一个按层数叠加以携带对应道痕[Path Marks]点的效果）；双端通过 {@link #get} 实时从流派效果还原
 *       {@link PathStrengthData}，效果被摘除即失去对应力量。</li>
 *   <li>每次写入后发布 {@link StrengthChangedEvent} 触发攻击力刷新；{@link #getBeastBuffs} 从活动流派效果中提取限时兽力
 *       增益（{@link BeastStrengthBuff}），{@link #getDurationBonus} 汇总永久兽力对力道限时状态的时长加成；总力量点、结
 *       算力量点与准入归 {@link StrengthService} 计算。</li>
 *   <li>力道分支互斥在此处唯一把关：当玩家已持有互斥分支（{@link #getInterference}，运行中的限时兽力同样视为持有兽力虚
 *       影流）时，{@link #grant} 与上调层数的 {@link #setHumanStrength} 拒绝写入并返回 {@code false}；移除或下调始终放
 *       行。</li>
 *   <li>分支持有判定优先级（{@link #getHeldBranch}）：优先认兽力虚影流，其次人力钧力流，均无则为
 *       {@link StrengthPathBranch#NORMAL}。</li>
 *   <li>⚠ 永久兽力与人力的准入检查在蛊虫行为侧（{@code BeastStrengthBehavior}、{@code HumanStrengthBehavior}）执行而非
 *       本服务内，以便管理员指令可赋予超出常态承受力的永久力量（超出部分全额发挥并结算过载反噬）。</li>
 *   <li>⚠ 下调人力层数时通过 {@code dropLayer} 按整层扣除该档人力自带的道痕点，保留其他途径（如增幅蛊）追加的道
 *       痕。</li>
 * </ul>
 */

public final class PathStrengthService {

    private PathStrengthService() {}

    private static final List<StrengthPathBranch> HELD_ORDER =
            List.of(StrengthPathBranch.BEAST_STRENGTH_PHANTOM, StrengthPathBranch.HUMAN_JUN_STRENGTH);

    public static @NotNull PathStrengthData get(@NotNull Player player) {
        Set<BeastStrength> beasts = EnumSet.noneOf(BeastStrength.class);
        Map<HumanStrength, Integer> layers = new EnumMap<>(HumanStrength.class);
        for (PathEffect<?> effect : PathEffectService.getEffects(player)) {
            if (effect.payload() instanceof BeastStrength beast) {
                beasts.add(beast);
            } else if (effect.payload() instanceof HumanStrength kind) {
                layers.put(kind, effect.layers());
            }
        }
        return new PathStrengthData(beasts, layers);
    }

    public static boolean has(@NotNull Player player, @NotNull BeastStrength beast) { return get(player).has(beast); }

    public static int getDurationBonus(@NotNull Player player) { return get(player).getBeastDurationBonus(); }

    public static int getHumanStrength(@NotNull Player player, @NotNull HumanStrength kind) {
        return get(player).getHumanStrengthCount(kind);
    }

    private static boolean holdsBranch(@NotNull Player player, @NotNull StrengthPathBranch branch) {
        return get(player).hasPathBranch(branch)
                || (branch == StrengthPathBranch.BEAST_STRENGTH_PHANTOM && hasBeastBuff(player));
    }

    public static @NotNull StrengthPathBranch getHeldBranch(@NotNull Player player) {
        for (StrengthPathBranch branch : HELD_ORDER) {
            if (holdsBranch(player, branch)) return branch;
        }
        return StrengthPathBranch.NORMAL;
    }

    public static @Nullable StrengthPathBranch getInterference(@NotNull Player player,
                                                               @NotNull StrengthPathBranch branch) {
        for (StrengthPathBranch held : StrengthPathBranch.values()) {
            if (held.interferesWith(branch) && holdsBranch(player, held)) return held;
        }
        return null;
    }

    public static boolean grant(@NotNull ServerPlayer player, @NotNull BeastStrength beast) {
        if (getInterference(player, StrengthPathBranch.BEAST_STRENGTH_PHANTOM) != null) return false;

        PathStrengthData held = get(player);
        if (!held.has(beast)) {
            Set<BeastStrength> kept = held.with(beast).beasts();
            PathEffectService.remove(player,
                    effect -> effect.payload() instanceof BeastStrength other && !kept.contains(other));
            PathEffectService.apply(player, createEffect(ModPathEffectTypes.BEAST_STRENGTH, beast, beast.getPoints()));
        }
        ModEvents.post(new StrengthChangedEvent(player));
        return true;
    }

    public static void revoke(@NotNull ServerPlayer player, @NotNull BeastStrength beast) {
        PathEffectService.remove(player, effect -> effect.payload() == beast);
        ModEvents.post(new StrengthChangedEvent(player));
    }

    public static void clear(@NotNull ServerPlayer player) {
        PathEffectService.remove(player,
                effect -> effect.payload() instanceof BeastStrength || effect.payload() instanceof HumanStrength);
        ModEvents.post(new StrengthChangedEvent(player));
    }

    public static boolean setHumanStrength(@NotNull ServerPlayer player, @NotNull HumanStrength kind, int value) {
        int held = getHumanStrength(player, kind);
        if (value > held && getInterference(player, StrengthPathBranch.HUMAN_JUN_STRENGTH) != null) return false;

        int layers = Math.clamp(value, 0, kind.getMaxLayers());
        PathEffect<HumanStrength> one = createEffect(ModPathEffectTypes.HUMAN_STRENGTH, kind, kind.getJin());
        if (layers == 0) {
            PathEffectService.remove(player, effect -> effect.payload() == kind);
        } else if (held == 0) {
            PathEffectService.apply(player, withLayers(one, one.paths(), layers));
        } else if (layers != held) {
            PathEffectService.update(player, ModPathEffectTypes.HUMAN_STRENGTH, one.source(),
                    effect -> withLayers(effect, one.paths(), layers));
        }
        ModEvents.post(new StrengthChangedEvent(player));
        return true;
    }

    public static boolean addHumanStrength(@NotNull ServerPlayer player, @NotNull HumanStrength kind, int delta) {
        return setHumanStrength(player, kind, getHumanStrength(player, kind) + delta);
    }

    private static <P extends NamedEnum> PathEffect<P> createEffect(PathEffectType<P> type, P payload, int points) {
        ResourceLocation source = Guzhenren.id("strength/" + payload.getSerializedName());
        return PathEffect.of(type, EffectCarrier.BODY, source,
                Map.of(GuPath.STRENGTH, PathMarks.ofPoints(PathStrengthData.getMarkPoints(points))),
                PathEffect.PERMANENT, payload);
    }

    private static <P> PathEffect<P> withLayers(PathEffect<P> effect, Map<GuPath, PathMarks> layer, int layers) {
        PathEffect<P> next = effect;
        while (next.layers() < layers) next = next.addLayer(layer);
        while (next.layers() > layers) next = next.dropLayer(layer);
        return next;
    }

    public static @NotNull List<StrengthAmplifier> getBeastBuffs(@NotNull Player player) {
        List<StrengthAmplifier> buffs = new ArrayList<>();
        for (PathEffect<?> effect : PathEffectService.getEffects(player)) {
            BeastStrengthBuff buff = getBeastBuff(effect);
            if (buff != null) {
                buffs.add(StrengthAmplifier.beast(effect.source(), buff.getBeastFamily(),
                        PathStrengthData.getPoints(effect.getMarks(GuPath.STRENGTH).points())));
            }
        }
        return buffs;
    }

    private static boolean hasBeastBuff(Player player) {
        for (PathEffect<?> effect : PathEffectService.getEffects(player)) {
            if (getBeastBuff(effect) != null) return true;
        }
        return false;
    }

    private static @Nullable BeastStrengthBuff getBeastBuff(PathEffect<?> effect) {
        // Looked up without TimedBuff.getHolder: a saved buff whose effect is gone must not throw at a Gu gate.
        return effect.payload() instanceof TimedBuff buff && BuiltInRegistries.MOB_EFFECT.getOptional(buff.effect())
                .orElse(null) instanceof BeastStrengthBuff beast ? beast : null;
    }
}
