package net.alex.guzhenren.client.event;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.renderer.AwakeningGuRenderer;
import net.alex.guzhenren.client.renderer.AwakeningPlaqueRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * The awakening ritual [开窍仪式] in the world, after the entities: the hovering Hope Gu [希望蛊]
 * ({@link AwakeningGuRenderer}) and the aptitude plaque over the head ({@link AwakeningPlaqueRenderer}). The screen
 * half is {@code AwakeningHud}.
 *
 * @author Alex
 * @version 1.0.0
 * @see AwakeningGuRenderer
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID, value = Dist.CLIENT)
public final class AwakeningEvents {

    private AwakeningEvents() {}

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        AwakeningGuRenderer.render(event.getPoseStack(), event.getCamera(), partialTick);
        AwakeningPlaqueRenderer.render(event.getPoseStack(), event.getCamera(), partialTick);
    }
}
