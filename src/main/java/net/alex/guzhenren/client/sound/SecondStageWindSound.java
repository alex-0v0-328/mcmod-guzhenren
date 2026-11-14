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
 * 本地玩家二段飞行[Second-Stage Flight]的风声，仅本人客户端可闻。
 * <ul>
 *   <li>原版鞘翅循环音，随玩家位置和油门；{@code SecondStageEvents} 每次进入二段飞行时启动，标记消失时停止。</li>
 *   <li>油门到 {@link #QUIET_THROTTLE} 前静音、满油门满音量；音高从 {@link #BASE_PITCH} 起随油门升
 *       {@link #PITCH_PER_THROTTLE}；音量和音高向目标缓动，每 tick 升 {@link #RISE}、降 {@link #FALL}。</li>
 * </ul>
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

    private static float getVolume(float throttle) {
        return Mth.clamp((throttle - QUIET_THROTTLE) / (1.0F - QUIET_THROTTLE), SILENT, 1.0F);
    }
}
