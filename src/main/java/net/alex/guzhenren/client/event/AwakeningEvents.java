package net.alex.guzhenren.client.event;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.renderer.AwakeningGuRenderer;
import net.alex.guzhenren.client.renderer.AwakeningPlaqueRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * 开窍仪式的世界内渲染入口：排在实体渲染之后，负责绘制悬浮的希望蛊[Hope Gu]（{@link AwakeningGuRenderer}）与头顶资
 * 质[Aptitude]牌匾（{@link AwakeningPlaqueRenderer}）；屏幕叠层部分由 {@code AwakeningHud} 负责。
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
