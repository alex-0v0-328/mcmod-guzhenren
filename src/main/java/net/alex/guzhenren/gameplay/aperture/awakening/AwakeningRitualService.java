package net.alex.guzhenren.gameplay.aperture.awakening;

import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.registry.advancement.ModCriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The awakening ritual [开窍仪式]: the only writer of {@link AwakeningRitual}, and the one road from a Hope Gu
 * [希望蛊] click to an opened first aperture.
 *
 * <p>{@link #start} stores the ritual with the aptitude [资质] already rolled and the hover point
 * {@link #ANCHOR_DISTANCE} blocks before the eyes; {@link #tick} awakens at {@link AwakeningTimeline#AWAKEN_TICKS}
 * and ends the ritual at {@link AwakeningTimeline#RELEASE_TICKS}, when the client lifts the stance, so the server
 * refuses dashing and nourishing exactly as long as the player is held still. {@link #finish} ends a ritual and
 * awakens if that has not happened yet; every way out of a ritual calls it: the clock, a death, a dimension change, a
 * logout, and a login that finds one a crash left behind. The roll was fixed at the click, so cutting the
 * ritual short opens the same aperture and a relog can never re-roll it. Finishing fires the Hope Gu criterion, since
 * the ritual is what that advancement asks for.
 *
 * <p>⚠ {@code ApertureService.awaken} does not refuse an awakened player, so the ritual does: a command that
 * awakens a player mid-ritual leaves that aperture alone, and the Gu is spent. A reset clears the ritual without
 * awakening ({@code PlayerDataService.resetAll}).
 *
 * @author Alex
 * @version 1.0.0
 * @see AwakeningTimeline
 * @see AwakeningRitualEvents
 * @since 1.0.0
 */

public final class AwakeningRitualService {

    private AwakeningRitualService() {}

    public static final double ANCHOR_DISTANCE = 1.5;
    private static final SoundEvent START_SOUND = SoundEvents.BEACON_ACTIVATE;
    private static final SoundEvent AWAKEN_SOUND = SoundEvents.ENCHANTMENT_TABLE_USE;
    private static final float START_PITCH = 1.2F;

    public static @Nullable AwakeningRitual get(@NotNull Player player) {
        return PlayerData.get(player).getAwakeningRitual();
    }

    public static boolean isInRitual(@NotNull Player player) { return get(player) != null; }

    public static void start(@NotNull ServerPlayer player, int base) {
        Vec3 anchor = player.getEyePosition().add(player.getLookAngle().scale(ANCHOR_DISTANCE));
        PlayerData.get(player).setAwakeningRitual(new AwakeningRitual(player.level().getGameTime(), base, anchor));
        playSound(player, START_SOUND, START_PITCH);
    }

    public static void tick(@NotNull ServerPlayer player) {
        AwakeningRitual ritual = get(player);
        if (ritual == null) return;
        float elapsed = ritual.getElapsed(player.level().getGameTime(), 0.0F);
        if (elapsed >= AwakeningTimeline.RELEASE_TICKS) {
            finish(player);
        } else if (elapsed >= AwakeningTimeline.AWAKEN_TICKS) {
            awaken(player, ritual);
        }
    }

    public static void finish(@NotNull ServerPlayer player) {
        AwakeningRitual ritual = get(player);
        if (ritual == null) return;
        PlayerData.get(player).setAwakeningRitual(null);
        awaken(player, ritual);
    }

    private static void awaken(ServerPlayer player, AwakeningRitual ritual) {
        if (ApertureService.isAwakened(player)) return;

        ApertureService.awaken(player, ritual.base());
        ModCriteriaTriggers.USED_HOPE_GU.get().trigger(player);
        player.server.getCommands().sendCommands(player);
        playSound(player, AWAKEN_SOUND, 1.0F);
    }

    private static void playSound(ServerPlayer player, SoundEvent sound, float pitch) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS,
                1.0F, pitch);
    }
}
