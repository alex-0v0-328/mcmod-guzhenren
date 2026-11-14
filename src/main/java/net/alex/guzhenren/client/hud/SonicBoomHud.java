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
 * 二段飞行[Second-Stage Flight]进入者自己第一人称屏幕上的音爆环。
 * <ul>
 *   <li>激波环从准星展开、越过屏幕边缘；世界环（{@code net.alex.guzhenren.particle.SecondStageRingEmitter}）
 *       同时在镜头后为他人展开；注册在原版镜头叠层之上（{@code ClientEvents}）。</li>
 *   <li>只播本机请求的进入：{@code net.alex.guzhenren.client.input.SecondStageInput} 发出的进入双击经
 *       {@link #noteRequested} 记下，其后 {@link #REQUEST_WINDOW_TICKS} 内同步标记打开才由
 *       {@link #onSecondStageStarted} 播放（{@code net.alex.guzhenren.client.event.SecondStageEvents}）；
 *       换维度后附件同步等无请求的标记不播；两处均为本机玩家 {@code tickCount}，新实体重置，窗口两端有界。</li>
 *   <li>绽放同世界环：Alex 五帧环（{@link #RINGS}）每 tick 一帧，余下 {@link #LIFETIME_TICKS} 减帧数淡出；直径由屏高
 *       {@link #START_HEIGHT_FRACTION} 缓出至屏宽 {@link #END_WIDTH_FRACTION}，中心留空，笔画淡出前越过边缘；
 *       像素随之变粗，同 4 格世界环；加法混合，不透明度随「屏幕扭曲效果」滑条，关掉即无。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.client.particle.RingParticle
 * @since 1.0.0
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
