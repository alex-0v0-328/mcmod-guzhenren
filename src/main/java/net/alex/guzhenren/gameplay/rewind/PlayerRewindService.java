package net.alex.guzhenren.gameplay.rewind;

import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.gameplay.mind.MindService;
import net.alex.guzhenren.gameplay.soul.SoulService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The body's memory for the Man As Before Gu [人如故]: {@link #record} a {@link PlayerSnapshot} when one is due,
 * {@link #pick} the one a day back, {@link #restore} the player to it.
 *
 * <p>A restore writes the vanilla body directly (health, food, air, fire, the effect list replaced whole) and the
 * mod's values through their services, so each derived state is refreshed the way its owner does it. The heartbeat
 * that follows re-projects the half-zombie [半僵] effect and re-runs the lethal checks on the restored values.
 *
 * <p>⚠ {@code record} is gated by {@link RewindClock#isDue} on the ring's last stamp, not by the caller's cadence,
 * so a relog or two carriers never double a snapshot.
 *
 * @author Alex
 * @version 1.0.0
 * @see PlayerSnapshot
 * @see PlayerRewindData
 * @since 1.0.0
 */

public final class PlayerRewindService {

    private PlayerRewindService() {}

    public static @NotNull PlayerRewindData get(@NotNull Player player) { return PlayerData.get(player).getRewind(); }

    public static boolean record(@NotNull ServerPlayer player, long now) {
        PlayerRewindData data = get(player);
        if (!RewindClock.isDue(data.getLastTakenAt(), now)) return false;

        PlayerData.get(player).setRewind(data.with(PlayerSnapshot.capture(player, now)));
        return true;
    }

    public static @Nullable PlayerSnapshot pick(@NotNull Player player, long now) { return get(player).pick(now); }

    public static void restore(@NotNull ServerPlayer player, @NotNull PlayerSnapshot snapshot) {
        player.setHealth(snapshot.health());
        player.getFoodData().setFoodLevel(snapshot.foodLevel());
        player.getFoodData().setSaturation(snapshot.saturation());
        player.setAirSupply(snapshot.airSupply());
        player.setRemainingFireTicks(snapshot.fireTicks());
        player.removeAllEffects();
        for (MobEffectInstance effect : snapshot.effects()) player.addEffect(new MobEffectInstance(effect));

        SoulService.setCurrent(player, snapshot.soul());
        MindService.restoreThoughts(player, snapshot.thoughts(), snapshot.taggedThoughts());
        BodyService.restoreHalfZombie(player, snapshot.halfZombieTicksLeft());
    }
}
