package net.alex.guzhenren.client.event;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.renderer.AwakeningGuRenderer;
import net.alex.guzhenren.client.renderer.AwakeningPlaqueRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * 开窍[Awakening]仪式的世界内绘制，排在实体之后：悬浮的希望蛊[Hope Gu]（{@link AwakeningGuRenderer}）
 * 与头顶资质[Aptitude]牌匾（{@link AwakeningPlaqueRenderer}）；屏幕部分在 {@code AwakeningHud}。
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
