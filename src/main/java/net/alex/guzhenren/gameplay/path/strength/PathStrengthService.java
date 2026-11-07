package net.alex.guzhenren.gameplay.path.strength;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.core.ModEvents;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.gameplay.path.PathEffect;
import net.alex.guzhenren.gameplay.path.PathEffectService;
import net.alex.guzhenren.gameplay.path.timed.TimedBuff;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Strength [力道]: what has been accumulated -- beast strengths [兽力] and human strength counts. Static
 * service; every write posts a {@link StrengthChangedEvent}, which refreshes the attack modifier.
 * {@link #getBeastBuffs} reads the running timed beast buffs off the path effects, which are the buffs' truth, as
 * {@link StrengthAmplifier}s; the whole strength and how much of it a body can bring to bear are
 * {@code gameplay.attribute.StrengthService}, which also gates a new beast against the carrying capacity.
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
 * Normal. ⚠ The funnel keeps a body from holding both, but the codec does not: a record saved with both reads as
 * Beast Strength Phantom.
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
        return PlayerData.get(player).getStrength();
    }

    public static boolean has(@NotNull Player player, @NotNull BeastStrength beast) { return get(player).has(beast); }

    public static int getHumanStrength(@NotNull Player player, @NotNull HumanStrength kind) {
        return get(player).getHumanStrengthCount(kind);
    }

    public static boolean holdsBranch(@NotNull Player player, @NotNull StrengthPathBranch branch) {
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

        store(player, get(player).with(beast));
        return true;
    }

    public static void revoke(@NotNull ServerPlayer player, @NotNull BeastStrength beast) {
        store(player, get(player).without(beast));
    }

    public static void clear(@NotNull ServerPlayer player) { store(player, PathStrengthData.DEFAULT); }

    private static void store(ServerPlayer player, PathStrengthData data) {
        PlayerData.get(player).setStrength(data);
        ModEvents.post(new StrengthChangedEvent(player));
    }

    public static boolean setHumanStrength(@NotNull ServerPlayer player, @NotNull HumanStrength kind, int value) {
        if (value > getHumanStrength(player, kind)
                && getInterference(player, StrengthPathBranch.HUMAN_JUN_STRENGTH) != null) return false;

        store(player, get(player).withHumanStrength(kind, value));
        return true;
    }

    public static boolean addHumanStrength(@NotNull ServerPlayer player, @NotNull HumanStrength kind, int delta) {
        return setHumanStrength(player, kind, getHumanStrength(player, kind) + delta);
    }

    /** Every running timed beast buff as an amplifier, sourced to the Gu that granted it. */
    public static @NotNull List<StrengthAmplifier> getBeastBuffs(@NotNull Player player) {
        List<StrengthAmplifier> buffs = new ArrayList<>();
        for (PathEffect<?> effect : PathEffectService.getEffects(player)) {
            BeastStrengthBuff buff = getBeastBuff(effect);
            if (buff != null) buffs.add(StrengthAmplifier.beast(effect.source(), buff.getBeastFamily(), buff.getPoints()));
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
