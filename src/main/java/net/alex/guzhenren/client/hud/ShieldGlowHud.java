package net.alex.guzhenren.client.hud;

import net.alex.guzhenren.gameplay.body.ShieldData;
import net.alex.guzhenren.gameplay.body.ShieldLayer;
import net.alex.guzhenren.gameplay.body.ShieldService;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 护盾光晕：本机玩家有护盾时屏幕边缘的白光。
 * <ul>
 *   <li>无盾升盾时 {@link #FADE_IN_TICKS} 内淡入，后保持 {@link #PEAK_ALPHA} × 各层剩余点数和 / 升起点数和；
 *       受击削减某层而护盾未尽则加亮 {@link #FLASH_ALPHA}，{@link #FLASH_TICKS} 内回落；某层到时只降亮度；
 *       末层结束则从当前亮度经 {@link #FADE_OUT_TICKS} 淡出。</li>
 *   <li>{@link #tick} 每客户端 tick 读一次同步的 {@code ShieldData}，按游戏时间记时刻；本机玩家实体换新（重生、换维度）
 *       即重来，护盾同步后重新淡入；{@link #render} 以白色加法绘原版暗角贴图，边缘变亮，同 {@link SonicBoomHud}
 *       随「屏幕扭曲效果」滑条缩放；注册在原版镜头叠层上（{@code ClientEvents}）。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see ShieldService
 * @since 1.0.0
 */

public final class ShieldGlowHud {

    public static final ShieldGlowHud INSTANCE = new ShieldGlowHud();

    private ShieldGlowHud() {}

    private static final ResourceLocation VIGNETTE =
            ResourceLocation.withDefaultNamespace("textures/misc/vignette.png");
    static final int FADE_IN_TICKS = 10;
    static final int FLASH_TICKS = 5;
    static final int FADE_OUT_TICKS = 6;
    static final float PEAK_ALPHA = 0.35F;
    static final float FLASH_ALPHA = 0.3F;
    private @Nullable LocalPlayer owner;
    private ShieldData shield = ShieldData.DEFAULT;
    private float points;
    private float max;
    private long raisedAt = Long.MIN_VALUE;
    private long flashedAt = Long.MIN_VALUE;
    private long droppedAt = Long.MIN_VALUE;
    private float droppedFrom;

    public void tick(@NotNull LocalPlayer player) {
        if (player != owner) {
            reset();
            owner = player;
        }
        ShieldData next = ShieldService.get(player);
        long now = player.level().getGameTime();
        float nextPoints = next.getPoints(now);
        if (nextPoints > 0.0F && points <= 0.0F) {
            raisedAt = now;
        } else if (nextPoints > 0.0F && isSpent(shield, next)) {
            flashedAt = now;
        } else if (nextPoints <= 0.0F && points > 0.0F) {
            droppedFrom = getAlpha(now, 0.0F);
            droppedAt = now;
        }
        shield = next;
        points = nextPoints;
        max = next.getMax(now);
    }

    public void reset() {
        owner = null;
        shield = ShieldData.DEFAULT;
        points = 0.0F;
        max = 0.0F;
        raisedAt = Long.MIN_VALUE;
        flashedAt = Long.MIN_VALUE;
        droppedAt = Long.MIN_VALUE;
        droppedFrom = 0.0F;
    }

    public void render(@NotNull GuiGraphics graphics, @NotNull DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || player.isSpectator()) return;

        float alpha = getAlpha(player.level().getGameTime(), deltaTracker.getGameTimeDeltaPartialTick(false))
                * ScreenGlow.getEffectScale(minecraft);
        if (alpha <= 0.0F) return;

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        ScreenGlow.begin(graphics, alpha);
        graphics.blit(VIGNETTE, 0, 0, -90, 0.0F, 0.0F, width, height, width, height);
        ScreenGlow.end(graphics);
    }

    private float getAlpha(long now, float partialTick) {
        if (points <= 0.0F || max <= 0.0F) {
            float out = getAge(now, droppedAt, partialTick) / FADE_OUT_TICKS;
            return out < 1.0F ? droppedFrom * (1.0F - out) : 0.0F;
        }
        float in = Math.min(getAge(now, raisedAt, partialTick) / FADE_IN_TICKS, 1.0F);
        float flash = getAge(now, flashedAt, partialTick) / FLASH_TICKS;
        return PEAK_ALPHA * Math.min(points / max, 1.0F) * in + (flash < 1.0F ? FLASH_ALPHA * (1.0F - flash) : 0.0F);
    }

    /** 同一次升起（结束 tick 未变）且仍在的某层，点数是否比之前少。 */
    private static boolean isSpent(@NotNull ShieldData before, @NotNull ShieldData after) {
        for (ShieldLayer layer : before.layers()) {
            ShieldLayer next = after.find(layer.type());
            if (next != null && next.endTick() == layer.endTick() && next.points() < layer.points()) return true;
        }
        return false;
    }

    private static float getAge(long now, long at, float partialTick) {
        return at == Long.MIN_VALUE || now < at ? Float.MAX_VALUE : now - at + partialTick;
    }
}
