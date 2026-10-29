package net.alex.guzhenren.client.event;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.hud.SonicBoomHud;
import net.alex.guzhenren.client.sound.SecondStageWindSound;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.gameplay.flight.SecondStageCruise;
import net.alex.guzhenren.gameplay.flight.SecondStageService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import org.jetbrains.annotations.Nullable;

/**
 * The local player's second-stage flight [二段飞行] on its own end: the throttle that moves it, and the feedback that
 * reads it. {@code net.alex.guzhenren.mixin.PlayerMixin} moves the player; {@link SecondStageCamera} rolls and shakes
 * the view.
 *
 * <p>{@link #onClientTick} runs before the player ticks and after the tick's packets, and waits out a paused game,
 * when the player does not tick either. On the edge where the synced mark turns on, or turns up on a new local
 * player (a dimension change rebuilds it with the mark re-synced), it sets the throttle to
 * {@link SecondStageCruise#ENTRY_THROTTLE}, as the server did, starts the wind ({@link SecondStageWindSound}) and
 * plays the first-person sonic boom ({@link SonicBoomHud#onSecondStageStarted}), which keeps it only for an entry
 * this client asked for. Every later tick with the mark it steps the throttle
 * ({@link SecondStageService#stepThrottle}) from the local sprint, so the cruise answers the keys at once instead of a
 * round trip later; without the mark the throttle is empty.
 *
 * <p>{@link #onComputeFovModifier} widens the view with the throttle while the local player flies the second stage,
 * multiplying the vanilla modifier (already widened by flying and sprinting) by {@link #getFovFactor}: up to
 * {@link #FOV_WIDENING} more at full throttle, eased toward none by the vanilla FOV Effects slider the same way
 * NeoForge eases the vanilla modifier, so a player who turned FOV effects off sees no widening here either. Vanilla
 * eases the view toward each new modifier over a few ticks; Minecraft caps the total widening, sprint and flight
 * included.
 *
 * <p>{@link #onLoggingOut} clears {@link #wasInSecondStage}, the remembered player and the sonic boom, so a stage
 * that was running when the player left does not make the next world's first entry look like a continuation.
 *
 * @author Alex
 * @version 1.0.0
 * @see SecondStageService
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID, value = Dist.CLIENT)
public final class SecondStageEvents {

    private SecondStageEvents() {}

    private static final double FOV_WIDENING = 0.15D;
    private static boolean wasInSecondStage;
    private static @Nullable LocalPlayer lastPlayer;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.isPaused()) return;
        LocalPlayer player = minecraft.player;
        boolean active = player != null && SecondStageService.isActive(player);
        if (active && (!wasInSecondStage || player != lastPlayer)) {
            PlayerData.get(player).setSecondStageThrottle(SecondStageCruise.ENTRY_THROTTLE);
            minecraft.getSoundManager().play(new SecondStageWindSound(player));
            SonicBoomHud.INSTANCE.onSecondStageStarted(player.tickCount);
        } else if (active) {
            SecondStageService.stepThrottle(player);
        } else if (player != null) {
            PlayerData.get(player).setSecondStageThrottle(0.0D);
        }
        wasInSecondStage = active;
        lastPlayer = player;
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        wasInSecondStage = false;
        lastPlayer = null;
        SonicBoomHud.INSTANCE.reset();
    }

    @SubscribeEvent
    public static void onComputeFovModifier(ComputeFovModifierEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = event.getPlayer();
        if (player == minecraft.player && player.getAbilities().flying && SecondStageService.isActive(player)) {
            event.setNewFovModifier(event.getNewFovModifier() * getFovFactor(
                    minecraft.options.fovEffectScale().get(), PlayerData.get(player).getSecondStageThrottle()));
        }
    }

    public static float getFovFactor(double effectScale, double throttle) {
        return (float) (1.0D + FOV_WIDENING * throttle * effectScale);
    }
}
