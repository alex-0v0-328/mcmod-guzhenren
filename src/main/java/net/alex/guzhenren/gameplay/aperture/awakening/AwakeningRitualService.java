package net.alex.guzhenren.gameplay.aperture.awakening;

import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.Talent;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.registry.advancement.ModCriteriaTriggers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The awakening ritual [开窍仪式]: the only writer of {@link AwakeningRitual}, and the one road from a Hope Gu
 * [希望蛊] click to an opened first aperture.
 *
 * <p>{@link #start} stores the ritual with the aptitude [资质] and a Ten-Extremes physique already rolled and the
 * hover point {@link #ANCHOR_DISTANCE} blocks before the eyes. {@link #tick} awakens at
 * {@link AwakeningTimeline#AWAKEN_TICKS}, reveals at {@link AwakeningTimeline#RELEASE_TICKS} (the whole-server
 * announcement and the player's own reveal sound) and ends the ritual at {@link AwakeningTimeline#END_TICKS}, so
 * every client draws the plaques off the synced ritual alone. {@link #holdsStill} is the stance both sides read: the
 * server refuses dashing and nourishing exactly as long as the client locks the input. {@link #finish} ends a ritual
 * at once, awakening and announcing whatever has not happened yet; every way out of a ritual calls it: the clock, a
 * death, a dimension change, a logout, and a login that finds one a crash left behind. The roll was fixed at the
 * click, so cutting the ritual short only cuts the animation: it opens the same aperture, and a relog can never
 * re-roll it. Awakening fires the Hope Gu criterion, since the ritual is what that advancement asks for.
 *
 * <p>{@link #isInDanger} is the gate before the click: a hit from another living entity within the last five seconds
 * (vanilla keeps the attacker that long, a player's own projectile aside), or the danger a bed refuses sleep for: a
 * monster that would stop the player resting within {@link #DANGER_REACH} blocks across and {@link #DANGER_HEIGHT}
 * up or down of the feet, the same box and test as {@code ServerPlayer.startSleepInBed}. Creative skips the gate, as
 * it skips the bed's.
 *
 * <p>⚠ {@code ApertureService.awaken} does not refuse an awakened player, so the ritual does: a command that
 * awakens a player mid-ritual leaves that aperture alone, and the Gu is spent. The announcement names the aptitude
 * the player holds then, while the plaques still draw the ritual's roll. A reset clears the ritual without awakening
 * ({@code PlayerDataService.resetAll}).
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
    public static final double DANGER_REACH = 8.0;
    public static final double DANGER_HEIGHT = 5.0;
    public static final String ANNOUNCEMENT = "guzhenren.chat.awakened";
    public static final String GRADE_PREFIX = "guzhenren.display.talent_grade.";
    private static final SoundEvent START_SOUND = SoundEvents.BEACON_ACTIVATE;
    private static final SoundEvent AWAKEN_SOUND = SoundEvents.ENCHANTMENT_TABLE_USE;
    private static final SoundEvent REVEAL_SOUND = SoundEvents.PLAYER_LEVELUP;
    private static final float START_PITCH = 1.2F;

    public static @Nullable AwakeningRitual get(@NotNull Player player) {
        return PlayerData.get(player).getAwakeningRitual();
    }

    public static boolean isInRitual(@NotNull Player player) { return get(player) != null; }

    public static boolean holdsStill(@NotNull Player player) {
        AwakeningRitual ritual = get(player);
        return ritual != null && AwakeningTimeline.holdsStill(ritual.getElapsed(player.level().getGameTime(), 0.0F));
    }

    public static boolean isInDanger(@NotNull Player player) {
        if (player.isCreative()) return false;
        LivingEntity attacker = player.getLastHurtByMob();
        if (attacker != null && attacker != player) return true;
        Vec3 feet = player.position();
        AABB area = new AABB(feet.x - DANGER_REACH, feet.y - DANGER_HEIGHT, feet.z - DANGER_REACH,
                feet.x + DANGER_REACH, feet.y + DANGER_HEIGHT, feet.z + DANGER_REACH);
        return !player.level().getEntitiesOfClass(Monster.class, area,
                monster -> monster.isPreventingPlayerRest(player)).isEmpty();
    }

    public static void start(@NotNull ServerPlayer player, int base) {
        ExtremePhysique physique = Talent.fromPercent(base) == Talent.TEN_EXTREMES
                ? ExtremePhysique.randomTenExtreme() : ExtremePhysique.NONE;
        Vec3 anchor = player.getEyePosition().add(player.getLookAngle().scale(ANCHOR_DISTANCE));
        PlayerData.get(player).setAwakeningRitual(
                new AwakeningRitual(player.level().getGameTime(), base, physique, anchor, false));
        playSound(player, START_SOUND, START_PITCH);
    }

    public static void tick(@NotNull ServerPlayer player) {
        AwakeningRitual ritual = get(player);
        if (ritual == null) return;
        float elapsed = ritual.getElapsed(player.level().getGameTime(), 0.0F);
        if (elapsed >= AwakeningTimeline.END_TICKS) {
            finish(player);
            return;
        }
        if (elapsed >= AwakeningTimeline.AWAKEN_TICKS) awaken(player, ritual);
        if (elapsed >= AwakeningTimeline.RELEASE_TICKS && !ritual.revealed()) {
            PlayerData.get(player).setAwakeningRitual(ritual.reveal());
            announce(player);
            player.playNotifySound(REVEAL_SOUND, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    public static void finish(@NotNull ServerPlayer player) {
        AwakeningRitual ritual = get(player);
        if (ritual == null) return;
        PlayerData.get(player).setAwakeningRitual(null);
        awaken(player, ritual);
        if (!ritual.revealed()) announce(player);
    }

    /** The announcement's aptitude: the grade, or the physique for Ten Extremes. */
    public static @NotNull Component getAptitudeName(@NotNull Player player) {
        Talent talent = ApertureService.getTalent(player);
        return talent == Talent.TEN_EXTREMES
                ? Component.translatable(BodyService.getExtremePhysique(player).getTranslationKey())
                : Component.translatable(GRADE_PREFIX + talent.getSerializedName());
    }

    private static void awaken(ServerPlayer player, AwakeningRitual ritual) {
        if (ApertureService.isAwakened(player)) return;

        ApertureService.awaken(player, ritual.base(), ritual.physique());
        ModCriteriaTriggers.USED_HOPE_GU.get().trigger(player);
        player.server.getCommands().sendCommands(player);
        playSound(player, AWAKEN_SOUND, 1.0F);
    }

    private static void announce(ServerPlayer player) {
        player.server.getPlayerList().broadcastSystemMessage(
                Component.translatable(ANNOUNCEMENT, player.getDisplayName(), getAptitudeName(player)), false);
    }

    private static void playSound(ServerPlayer player, SoundEvent sound, float pitch) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS,
                1.0F, pitch);
    }
}
