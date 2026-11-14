package net.alex.guzhenren.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.Talent;
import net.alex.guzhenren.gameplay.aperture.awakening.AwakeningRitual;
import net.alex.guzhenren.gameplay.aperture.awakening.AwakeningRitualService;
import net.alex.guzhenren.gameplay.aperture.awakening.AwakeningTimeline;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * 本地屏幕上的开窍[Awakening]仪式：像素菱形闪白、资质[Aptitude]牌匾与牌匾火星。
 *
 * <ul>
 *   <li>注册在全部其他层之上（{@code ClientEvents}），闪白盖住整个 HUD；闪白与牌匾不持状态，每帧按同步的仪式与整 tick
 *       画出，仪式中断（死亡、换维度）即停画。</li>
 *   <li>{@link #getBarBase} 给 {@link ChargeHud} 供资质条；读仪式，因为蛊点击时已耗掉；资质条是否显示按整 tick 判断，
 *       使 {@link ChargeHud#isShowing} 与所画的帧一致，不显示时为 -1。</li>
 *   <li>闪白是实心菱形，不抖动、不半透明：格子边长为牌匾整数倍 k，到屏幕中心格的曼哈顿距离不超过
 *       {@link AwakeningTimeline#getFlashReach} 的格子涂满；常态 {@link #FLASH_WHITE}，十绝天资开窍起转
 *       {@link #TEN_EXTREMES_FLASH} 淡鹅黄；HUD 隐藏仍覆盖。</li>
 *   <li>牌匾（{@link AwakeningPlaque}）随 HUD 隐藏，头顶那块也一样；只在第一人称显示，因脱离的镜头看得到头顶那块；按不
 *       超过 {@link #MAX_PLAQUE_SCALE} 的最大整数倍 k 画，高不超过屏高的 {@link #PLAQUE_SCREEN_FRACTION}；自中线展开、
 *       白色斜带扫过非底色像素、自下而上碎掉，全按整 tick 跳。</li>
 *   <li>火星由 {@code HopeGuEffectEvents} 在本人第一人称时加入，记牌匾像素坐标与出生 tick，同样按 k 画在牌匾上；
 *       {@link #SPARK_FRAMES} 帧各 {@link #SPARK_FRAME_TICKS} tick，仪式结束后仍画完；游戏时间早于出生 tick（进了时间更早的世界）即丢弃。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see AwakeningTimeline
 * @since 1.0.0
 */

public final class AwakeningHud {

    public static final AwakeningHud INSTANCE = new AwakeningHud();

    private AwakeningHud() {}

    private static final int FLASH_WHITE = 0xFFFFFFFE;
    private static final int TEN_EXTREMES_FLASH = 0xFFFFF4C2;
    private static final int SHEEN = 0xFFFFFFFF;
    private static final int MAX_PLAQUE_SCALE = 3;
    private static final float PLAQUE_SCREEN_FRACTION = 0.6F;
    private static final int SHEEN_PIXELS = 2;
    private static final ResourceLocation SPARK = Guzhenren.id("textures/particle/hope_spark.png");
    private static final int SPARK_PIXELS = 8;
    private static final int SPARK_FRAMES = 6;
    private static final int SPARK_FRAME_TICKS = 3;
    private final List<Spark> sparks = new ArrayList<>();

    int getBarBase(@NotNull Player player, float partialTick) {
        AwakeningRitual ritual = AwakeningRitualService.get(player);
        long gameTime = player.level().getGameTime();
        if (ritual == null || !AwakeningTimeline.showsBar(ritual.getElapsed(gameTime, 0.0F))) return -1;
        return AwakeningTimeline.getBarBase(ritual.getElapsed(gameTime, partialTick), ritual.base());
    }

    boolean isBarFull(@NotNull Player player, float partialTick) {
        AwakeningRitual ritual = AwakeningRitualService.get(player);
        return ritual != null
                && AwakeningTimeline.isBarFull(ritual.getElapsed(player.level().getGameTime(), partialTick));
    }

    public void addSpark(int x, int y, int plaqueHeight, long bornAt) {
        sparks.add(new Spark(x, y, plaqueHeight, bornAt));
    }

    public void render(@NotNull GuiGraphics graphics, @NotNull DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            sparks.clear();
            return;
        }
        long gameTime = player.level().getGameTime();
        boolean showsPlaque = !minecraft.options.hideGui && !minecraft.gameRenderer.getMainCamera().isDetached();
        sparks.removeIf(spark -> gameTime < spark.bornAt()
                || gameTime - spark.bornAt() >= SPARK_FRAMES * SPARK_FRAME_TICKS);

        AwakeningRitual ritual = AwakeningRitualService.get(player);
        int tick = ritual == null ? -1 : (int) (gameTime - ritual.startTick());
        boolean inRitual = tick >= 0 && !AwakeningTimeline.isOver(tick);
        if (!inRitual && (sparks.isEmpty() || !showsPlaque)) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        if (inRitual) {
            AwakeningPlaque plaque = AwakeningPlaque.of(ritual);
            int height = plaque == null ? AwakeningPlaque.TALENT_HEIGHT : plaque.height();
            int scale = getPlaqueScale(graphics.guiHeight(), height);
            renderFlash(graphics, ritual, tick, scale);
            if (showsPlaque && plaque != null) renderPlaque(minecraft, graphics, plaque, tick, scale);
        }
        if (showsPlaque) {
            for (Spark spark : sparks) renderSpark(graphics, spark, (int) (gameTime - spark.bornAt()));
        }
        RenderSystem.disableBlend();
    }

    private static void renderFlash(GuiGraphics graphics, AwakeningRitual ritual, int tick, int cell) {
        int step = AwakeningTimeline.getFlashStep(tick);
        if (step == 0) return;

        int color = tick >= AwakeningTimeline.AWAKEN_TICKS && Talent.fromPercent(ritual.base()) == Talent.TEN_EXTREMES
                ? TEN_EXTREMES_FLASH : FLASH_WHITE;
        int columns = Mth.positiveCeilDiv(graphics.guiWidth(), cell);
        int rows = Mth.positiveCeilDiv(graphics.guiHeight(), cell);
        int centerX = columns / 2;
        int centerY = rows / 2;
        int farthest = Math.max(centerX, columns - 1 - centerX) + Math.max(centerY, rows - 1 - centerY);
        int reach = AwakeningTimeline.getFlashReach(step, farthest);
        for (int row = Math.max(0, centerY - reach); row <= Math.min(rows - 1, centerY + reach); row++) {
            int span = reach - Math.abs(row - centerY);
            graphics.fill((centerX - span) * cell, row * cell, (centerX + span + 1) * cell, (row + 1) * cell, color);
        }
    }

    private static void renderPlaque(Minecraft minecraft, GuiGraphics graphics, AwakeningPlaque plaque, int tick,
                                     int scale) {
        int height = plaque.height();
        if (!AwakeningTimeline.showsPlaque(tick, height)) return;

        int rows = AwakeningTimeline.getUnfoldedRows(tick, height);
        int top = (height - rows) / 2;
        int bottom = Math.min(top + rows,
                height - AwakeningTimeline.getShatteredRows(tick) * AwakeningTimeline.SHATTER_BLOCK);
        if (bottom <= top) return;

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(getPlaqueLeft(graphics, scale), getPlaqueTop(graphics, height, scale), 0.0F);
        pose.scale(scale, scale, 1.0F);
        graphics.blitSprite(plaque.sprite(), AwakeningPlaque.WIDTH, height, 0, top, 0, top, AwakeningPlaque.WIDTH,
                bottom - top);
        int offset = AwakeningTimeline.getSheenOffset(tick, AwakeningPlaque.WIDTH + height - 1);
        if (offset >= 0) {
            boolean[] lit = AwakeningPlaque.getHighlight(minecraft.getGuiSprites().getSprite(plaque.sprite()));
            for (int y = top; y < bottom; y++) {
                int end = Math.min(AwakeningPlaque.WIDTH, offset + SHEEN_PIXELS - y);
                for (int x = Math.max(0, offset - y); x < end; x++) {
                    if (lit[y * AwakeningPlaque.WIDTH + x]) graphics.fill(x, y, x + 1, y + 1, SHEEN);
                }
            }
        }
        pose.popPose();
    }

    private static void renderSpark(GuiGraphics graphics, Spark spark, int age) {
        int scale = getPlaqueScale(graphics.guiHeight(), spark.plaqueHeight());
        int size = SPARK_PIXELS * scale;
        int x = getPlaqueLeft(graphics, scale) + spark.x() * scale - size / 2;
        int y = getPlaqueTop(graphics, spark.plaqueHeight(), scale) + spark.y() * scale - size / 2;
        graphics.blit(SPARK, x, y, size, size, (float) (age / SPARK_FRAME_TICKS * SPARK_PIXELS), 0.0F, SPARK_PIXELS,
                SPARK_PIXELS, SPARK_PIXELS * SPARK_FRAMES, SPARK_PIXELS);
    }

    private static int getPlaqueLeft(GuiGraphics graphics, int scale) {
        return (graphics.guiWidth() - AwakeningPlaque.WIDTH * scale) / 2;
    }

    private static int getPlaqueTop(GuiGraphics graphics, int height, int scale) {
        return (graphics.guiHeight() - height * scale) / 2;
    }

    static int getPlaqueScale(int screenHeight, int plaqueHeight) {
        return Mth.clamp((int) (screenHeight * PLAQUE_SCREEN_FRACTION / plaqueHeight), 1, MAX_PLAQUE_SCALE);
    }

    private record Spark(int x, int y, int plaqueHeight, long bornAt) {}
}
