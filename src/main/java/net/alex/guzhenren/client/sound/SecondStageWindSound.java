package net.alex.guzhenren.client.sound;

import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.gameplay.flight.SecondStageService;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

/**
 * The wind of the local player's second-stage flight [二段飞行]: vanilla's elytra loop, following the player and the
 * throttle; client only, and only the flier hears it.
 *
 * <p>{@code SecondStageEvents} starts one on each entry. Silent up to {@link #QUIET_THROTTLE}, it swells to full
 * volume at full throttle while the pitch climbs from {@link #BASE_PITCH} by {@link #PITCH_PER_THROTTLE}; both ease
 * toward the target, {@link #RISE} a tick going up and {@link #FALL} coming down. It stops when the mark goes.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public class SecondStageWindSound extends AbstractTickableSoundInstance {

    static final float QUIET_THROTTLE = 0.2F;
    static final float BASE_PITCH = 1.1F;
    static final float PITCH_PER_THROTTLE = 0.7F;
    private static final float RISE = 0.1F;
    private static final float FALL = 0.15F;
    private static final float SILENT = 0.001F;
    private final LocalPlayer player;

    public SecondStageWindSound(@NotNull LocalPlayer player) {
        super(SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.player = player;
        this.looping = true;
        this.delay = 0;
        this.volume = SILENT;
        this.pitch = BASE_PITCH;
    }

    @Override
    public void tick() {
        if (player.isRemoved() || !SecondStageService.isActive(player)) {
            stop();
            return;
        }
        x = player.getX();
        y = player.getY();
        z = player.getZ();
        float throttle = (float) PlayerData.get(player).getSecondStageThrottle();
        float targetVolume = getVolume(throttle);
        float ease = targetVolume < volume ? FALL : RISE;
        volume = Mth.lerp(ease, volume, targetVolume);
        pitch = Mth.lerp(ease, pitch, BASE_PITCH + throttle * PITCH_PER_THROTTLE);
    }

    static float getVolume(float throttle) {
        return Mth.clamp((throttle - QUIET_THROTTLE) / (1.0F - QUIET_THROTTLE), SILENT, 1.0F);
    }
}
