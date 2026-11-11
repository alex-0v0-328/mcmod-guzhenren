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
 * Strength [力道]: what has been accumulated -- beast strengths [兽力] and human strength counts. Static
 * service and the only writer of the two permanent strength path effects, which are the strengths' one store: a
 * beast strength is one {@code BEAST_STRENGTH} effect per species, a human strength one {@code HUMAN_STRENGTH}
 * effect per kind with a layer per stored layer, each on the body, sourced {@code strength/<name>} and carrying
 * {@link PathStrengthData#getStrands} of its points. {@link #get} reads them back as a {@link PathStrengthData}, on
 * either side, so an effect removed by any hand is a strength gone. Every write posts a
 * {@link StrengthChangedEvent}, which refreshes the attack modifier.
 * {@link #getBeastBuffs} reads the running timed beast buffs off the path effects, which are the buffs' truth, as
 * {@link StrengthAmplifier}s; the whole strength and how much of it a body can bring to bear are
 * {@link StrengthService}, which also gates a new beast against the carrying capacity.
 *
 * <p>Mark Interference [道痕互斥] is enforced here, at the one write funnel: {@code grant} and a rising
 * {@code setHumanStrength} write nothing and return {@code false} while {@link #getInterference} names a held
 * branch, so neither a Gu nor a command can store both. A running {@link BeastStrengthBuff} counts as holding
 * Beast Strength Phantom, but granting one is not a write here: beast buffs are gated at the Gu. Removals always
 * go through. ⚠ The capacity gate is the Gu's ({@code BeastStrengthBehavior}), not this funnel's: a command grant
 * may seat a beast past the normal capacity, and the body then uses what it can carry.
 *
 * <p>{@link #holdsBranch} is the one holding rule, read by {@link #getInterference} for the gates and by
 * {@link #getHeldBranch} for the effects that differ by branch: Beast Strength Phantom, else Human Jun Strength, else
 * Normal. ⚠ The funnel keeps a body from holding both, but the effect store does not: a body carrying both reads as
 * Beast Strength Phantom. ⚠ Lowering human strength drops whole layers with their strands, so marks added to the
 * effect by other means stay.
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

    /** The ticks the held beasts add to a strength path timed state as it starts (the green bull's ten seconds). */
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

    /** Every running timed beast buff as an amplifier, sourced to the Gu that granted it. */
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
