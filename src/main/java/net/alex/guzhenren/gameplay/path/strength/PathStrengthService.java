package net.alex.guzhenren.gameplay.path.strength;

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
 * {@link #getStrengthPoints} is the body's whole strength in points [力量点]: the permanent record plus every
 * running {@link BeastStrengthBuff}, read off the path effects, which are the buffs' truth. How much of it a body
 * can bring to bear is {@code gameplay.attribute.StrengthService}.
 *
 * <p>Mark Interference [道痕互斥] is enforced here, at the one write funnel: {@code grant} and a rising
 * {@code setHumanStrength} write nothing and return {@code false} while {@link #getInterference} names a held
 * branch, so neither a Gu nor a command can store both. A running {@link BeastStrengthBuff} counts as holding
 * Beast Strength Phantom, but granting one is not a write here: beast buffs are gated at the Gu. Removals always
 * go through.
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

    /** Permanent points plus the points of every running beast buff. */
    public static int getStrengthPoints(@NotNull Player player) {
        int total = get(player).getStrengthPoints();
        for (PathEffect<?> effect : PathEffectService.getEffects(player)) {
            BeastStrengthBuff buff = getBeastBuff(effect);
            if (buff != null) total += buff.getPoints();
        }
        return total;
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
