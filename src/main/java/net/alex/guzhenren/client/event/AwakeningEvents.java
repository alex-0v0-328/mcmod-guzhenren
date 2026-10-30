package net.alex.guzhenren.client.event;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.hud.AwakeningHud;
import net.alex.guzhenren.client.renderer.AwakeningGuRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * The client half of the awakening ritual [开窍仪式]: {@link AwakeningHud#update} every client tick, the hovering
 * Hope Gu [希望蛊] after the entities ({@link AwakeningGuRenderer}), and a cleared screen on logging out, so a ritual
 * running when the player left does not play on in the next world.
 *
 * @author Alex
 * @version 1.0.0
 * @see AwakeningHud
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID, value = Dist.CLIENT)
public final class AwakeningEvents {

    private AwakeningEvents() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) AwakeningHud.INSTANCE.update(player);
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        AwakeningGuRenderer.render(event.getPoseStack(), event.getCamera(),
                event.getPartialTick().getGameTimeDeltaPartialTick(false));
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) { AwakeningHud.INSTANCE.reset(); }
}
