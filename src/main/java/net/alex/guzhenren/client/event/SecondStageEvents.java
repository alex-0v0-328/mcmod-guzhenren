package net.alex.guzhenren.client.event;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.hud.SonicBoomHud;
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

/**
 * The local player's second-stage flight [二段飞行] beyond its speed, which
 * {@code net.alex.guzhenren.mixin.PlayerMixin} and {@code LocalPlayerMixin} set.
 *
 * <p>{@link #onClientTick} watches the synced mark turn on, before the player ticks and after the tick's packets,
 * and plays the first-person sonic boom on that edge ({@link SonicBoomHud#onSecondStageStarted}), which keeps it
 * only for an entry this client asked for.
 *
 * <p>{@link #onComputeFovModifier} widens the view while the local player flies the second stage, multiplying the
 * vanilla modifier (already widened by flying and sprinting) by {@link #getFovFactor}: {@link #FOV_WIDENING} more,
 * eased toward none by the vanilla FOV Effects slider the same way NeoForge eases the vanilla modifier, so a player
 * who turned FOV effects off sees no widening here either. Vanilla eases the view toward each new modifier over a
 * few ticks, so it widens and narrows smoothly; Minecraft caps the total widening, sprint and flight included.
 *
 * <p>{@link #onLoggingOut} clears {@link #wasInSecondStage} and the sonic boom, so a stage that was running when the
 * player left does not make the next world's first entry look like a continuation.
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

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        LocalPlayer player = Minecraft.getInstance().player;
        boolean active = player != null && SecondStageService.isActive(player);
        if (active && !wasInSecondStage) SonicBoomHud.INSTANCE.onSecondStageStarted(player.tickCount);
        wasInSecondStage = active;
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        wasInSecondStage = false;
        SonicBoomHud.INSTANCE.reset();
    }

    @SubscribeEvent
    public static void onComputeFovModifier(ComputeFovModifierEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = event.getPlayer();
        if (player == minecraft.player && player.getAbilities().flying && SecondStageService.isActive(player)) {
            event.setNewFovModifier(event.getNewFovModifier()
                    * getFovFactor(minecraft.options.fovEffectScale().get()));
        }
    }

    public static float getFovFactor(double effectScale) {
        return (float) (1.0D + FOV_WIDENING * effectScale);
    }
}
