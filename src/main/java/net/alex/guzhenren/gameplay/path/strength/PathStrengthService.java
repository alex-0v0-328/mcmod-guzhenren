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
 * 力道积累（兽力[Beast Strength]与人力层数）的静态服务。
 * <ul>
 *   <li>两个永久力道流派效果的唯一写入方，效果即唯一存储：兽力每物种一个，人力每种一个、层数同存量，均在肉体，
 *       携带点数的丝；{@link #get} 两端读回，效果被谁摘除即失去该力量。</li>
 *   <li>每次写入发 {@link StrengthChangedEvent} 刷新攻击；{@link #getBeastBuffs} 从流派效果读出运行中的限时兽力增益；
 *       总量、可发挥量与新兽力的承受力把关归 {@link StrengthService}。</li>
 *   <li>道痕[Path Marks]互斥在此唯一写入口执行：已持互斥分支时 {@code grant} 与上调的 {@code setHumanStrength} 不写、
 *       返回 {@code false}；运行中的 {@link BeastStrengthBuff} 算持兽力虚影流，其授予在蛊处把关；移除总放行。</li>
 *   <li>持有优先级（{@link #holdsBranch}）：兽力虚影流，其次人力钧力流，否则基础力道。</li>
 *   <li>⚠ 承受力门槛在蛊（{@code BeastStrengthBehavior}）不在此：指令可超常态承受力，肉体只发挥能承受的部分。</li>
 *   <li>⚠ 写入口阻止兼持，效果存储不阻止：兼持读作兽力虚影流。</li>
 *   <li>⚠ 降低人力按整层连丝移除，他途加的道痕保留。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see PathStrengthData
 * @see StrengthChangedEvent
 * @see StrengthPathBranch#interferesWith
 * @since 1.0.0
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

    /** 持有兽力[Beast Strength]给力道限时状态加的 tick（青牛 10 秒）。 */
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
                Map.of(GuPath.STRENGTH, PathMarks.ofStrands(PathStrengthData.getStrands(points))),
                PathEffect.PERMANENT, payload);
    }

    private static <P> PathEffect<P> withLayers(PathEffect<P> effect, Map<GuPath, PathMarks> layer, int layers) {
        PathEffect<P> next = effect;
        while (next.layers() < layers) next = next.addLayer(layer);
        while (next.layers() > layers) next = next.dropLayer(layer);
        return next;
    }

    /** 运行中的限时兽力[Beast Strength]增益，来源为授予它的蛊。 */
    public static @NotNull List<StrengthAmplifier> getBeastBuffs(@NotNull Player player) {
        List<StrengthAmplifier> buffs = new ArrayList<>();
        for (PathEffect<?> effect : PathEffectService.getEffects(player)) {
            BeastStrengthBuff buff = getBeastBuff(effect);
            if (buff != null) {
                buffs.add(StrengthAmplifier.beast(effect.source(), buff.getBeastFamily(),
                        PathStrengthData.getPoints(effect.getMarks(GuPath.STRENGTH).strands())));
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
