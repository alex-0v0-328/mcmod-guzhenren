package net.alex.guzhenren.client.hud;

import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

/**
 * 二段飞行[Second-Stage Flight]进入者第一人称视角下的屏幕音爆环叠层。
 * <ul>
 *   <li>激波环从屏幕中心准星处展开并越过屏幕边缘，与此同时世界环
 *       （{@code net.alex.guzhenren.particle.SecondStageRingEmitter}）在镜头后方为其他观察者展开；本图层注册在原版镜头
 *       叠层之上（{@code ClientEvents}）。</li>
 *   <li>仅响应本机主动请求的进入动作：由 {@code net.alex.guzhenren.client.input.SecondStageInput} 发出的双击进入请求先
 *       通过 {@link #noteRequested} 登记，随后 {@link #REQUEST_WINDOW_TICKS} 内同步标记开启才由
 *       {@link #onSecondStageStarted} 触发播放（{@code net.alex.guzhenren.client.event.SecondStageEvents}），从而过滤
 *       换维度重建玩家等无请求同步；窗口两端均绑定本地玩家 {@code tickCount}，新实体自动重置。</li>
 *   <li>播放节奏与世界环一致：采用 Alex 绘制的五帧环贴图（{@link #RINGS}）每 tick 推进一帧，余下
 *       {@link #LIFETIME_TICKS} 减去帧数的窗口用于淡出；直径从屏高 {@link #START_HEIGHT_FRACTION} 缓出扩展至屏宽
 *       {@link #END_WIDTH_FRACTION}（中心留空且笔画在淡出前越过边缘，像素同步变粗以匹配 4 格世界环）；使用加法混合，
 *       不透明度随原版「屏幕扭曲效果」滑条缩放，关闭滑条时完全隐藏。</li>
 * </ul>
 */

public final class SonicBoomHud {

    public static final SonicBoomHud INSTANCE = new SonicBoomHud();

    private SonicBoomHud() {}

    private static final List<Integer> RING_CANVASES = List.of(3, 8, 13, 18, 23);
    private static final List<ResourceLocation> RINGS = RING_CANVASES.stream()
            .map(size -> Guzhenren.id("textures/particle/ring_" + size + "x" + size + ".png")).toList();
    static final int LIFETIME_TICKS = 8;
    static final float START_HEIGHT_FRACTION = 0.25F;
    static final float END_WIDTH_FRACTION = 1.7F;
    private static final int REQUEST_WINDOW_TICKS = 20;
    private int requestedAt = Integer.MIN_VALUE;
    private int startedAt = Integer.MIN_VALUE;

    public void noteRequested(int tickCount) { requestedAt = tickCount; }

    public void onSecondStageStarted(int tickCount) {
        long sinceRequest = (long) tickCount - requestedAt;
        if (sinceRequest < 0L || sinceRequest > REQUEST_WINDOW_TICKS) return;
        startedAt = tickCount;
        requestedAt = Integer.MIN_VALUE;
    }

    public void reset() {
        requestedAt = Integer.MIN_VALUE;
        startedAt = Integer.MIN_VALUE;
    }

    public void render(@NotNull GuiGraphics graphics, @NotNull DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !minecraft.options.getCameraType().isFirstPerson()) return;

        float age = getAge(player.tickCount, deltaTracker.getGameTimeDeltaPartialTick(false));
        if (age < 0.0F || age >= LIFETIME_TICKS) return;
        float alpha = getAlpha(age) * ScreenGlow.getEffectScale(minecraft);
        if (alpha <= 0.0F) return;

        int frame = getFrame(age);
        int canvas = RING_CANVASES.get(frame);
        int diameter = getDiameter(age, graphics.guiWidth(), graphics.guiHeight());
        ScreenGlow.begin(graphics, alpha);
        graphics.blit(RINGS.get(frame), (graphics.guiWidth() - diameter) / 2, (graphics.guiHeight() - diameter) / 2,
                diameter, diameter, 0.0F, 0.0F, canvas, canvas, canvas, canvas);
        ScreenGlow.end(graphics);
    }

    float getAge(int tickCount, float partialTick) {
        return (float) ((long) tickCount - startedAt) + partialTick;
    }

    static int getFrame(float age) {
        return Mth.clamp((int) age, 0, RING_CANVASES.size() - 1);
    }

    static float getAlpha(float age) {
        int frames = RING_CANVASES.size();
        if (age < frames) return 1.0F;
        return Mth.clamp(1.0F - (age - frames) / (LIFETIME_TICKS - frames), 0.0F, 1.0F);
    }

    static int getDiameter(float age, int screenWidth, int screenHeight) {
        float progress = Mth.clamp(age / LIFETIME_TICKS, 0.0F, 1.0F);
        float eased = 1.0F - (1.0F - progress) * (1.0F - progress);
        return Math.round(Mth.lerp(eased, START_HEIGHT_FRACTION * screenHeight, END_WIDTH_FRACTION * screenWidth));
    }
}
