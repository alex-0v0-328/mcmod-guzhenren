package net.alex.guzhenren.gameplay.path.strength;

import net.alex.guzhenren.registry.attachment.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.NotNull;

/**
 * Strength [力道]: what has been accumulated -- beast strengths [兽力] and human strength counts. Static
 * service; every write posts a {@link StrengthChangedEvent}, which refreshes the attack modifier. How
 * much of it a body can bring to bear is {@code BodyStrengthService}.
 *
 * @author Alex
 * @version 1.0.0
 * @see PathStrengthData
 * @see StrengthChangedEvent
 * @since 1.0.0
 */

public final class PathStrengthService {

    private PathStrengthService() {}

    public static @NotNull PathStrengthData get(@NotNull Player player) {
        return player.getData(ModAttachments.PATH_STRENGTH);
    }

    public static boolean has(@NotNull Player player, @NotNull BeastStrength beast) { return get(player).has(beast); }

    public static int getHumanStrength(@NotNull Player player, @NotNull HumanStrength kind) {
        return get(player).humanStrengthCount(kind);
    }

    public static void grant(@NotNull ServerPlayer player, @NotNull BeastStrength beast) {
        store(player, get(player).with(beast));
    }

    public static void revoke(@NotNull ServerPlayer player, @NotNull BeastStrength beast) {
        store(player, get(player).without(beast));
    }

    public static void clear(@NotNull ServerPlayer player) { store(player, PathStrengthData.DEFAULT); }

    private static void store(ServerPlayer player, PathStrengthData data) {
        player.setData(ModAttachments.PATH_STRENGTH, data);
        NeoForge.EVENT_BUS.post(new StrengthChangedEvent(player));
    }

    public static void setHumanStrength(@NotNull ServerPlayer player, @NotNull HumanStrength kind, int value) {
        store(player, get(player).withHumanStrength(kind, value));
    }

    public static void addHumanStrength(@NotNull ServerPlayer player, @NotNull HumanStrength kind, int delta) {
        setHumanStrength(player, kind, getHumanStrength(player, kind) + delta);
    }
}
