package net.alex.guzhenren.client.slider;

import com.mojang.blaze3d.platform.Window;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.ModPalette;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.hud.HudBar;
import net.alex.guzhenren.client.radial.RadialMenu;
import net.alex.guzhenren.item.GuItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 长按滑条选择器：按住使用键时在准星下方弹出的横向单值选择器。
 * <ul>
 *   <li>为扇形选择器（{@link RadialMenu}）的单值简化版，目前由元老蛊[Primeval Elder Gu]调用；同一时间仅运行一个长按
 *       实例（{@link #hold}），不与扇形选择器的长按共存。</li>
 *   <li>按住满 {@link RadialMenu#OPEN_DELAY_TICKS} tick 后弹出（轻点不弹出），弹出初始停留在 {@code 1} 并读取一次上
 *       限。</li>
 *   <li>横向移动鼠标可在 {@code 0}（取消）到调用方上限之间选值，松开按键时结算：{@code 1} 及以上调用
 *       {@link Owner#pick}，{@code 0} 不执行任何动作；弹出前松开调用 {@link Owner#releaseUnopened}；若上限为
 *       {@code 0} 则立即调用一次 {@link Owner#empty} 并等待松手（不再二次回调，因已视为完成滑条操作）；打开界面或
 *       登出时直接丢弃长按而不触发任何回调。</li>
 *   <li>⚠ 与扇形选择器相同，{@link Owner#isHeld} 因其他原因变为 false（如切走物品）也会立刻结束长按并调用
 *       {@code pick}，因此会发包的调用方须自行确认物品仍在手。</li>
 *   <li>⚠ 弹出期间锁定视角旋转但保持移动按键正常：{@code MouseHandlerMixin} 将横向鼠标位移（按屏幕像素）转交给
 *       {@link #steer}，推动不可见光标沿 {@link #TRACK_WIDTH} 像素轨道按 {@code top + 1} 档均分移动；长按期间阻
 *       止交互键传入世界，且滚轮不滚动快捷栏也不操控滑条。</li>
 *   <li>⚠ 滑条轨道使用 {@code slider} {@link HudBar}、旋钮使用 {@code hud/slider_knob} 精灵图且均不做缩放；
 *       {@code HudBarTexturesTest} 会将它们锁定在 {@link #TRACK_WIDTH}、{@link #TRACK_HEIGHT} 与旋钮尺寸上。</li>
 * </ul>
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID, value = Dist.CLIENT)
public final class HoldSlider {

    public interface Owner {
        boolean isHeld();

        int top();

        Component label(int value);

        Component detail(int top);

        void pick(int value);

        default void empty() {}

        default void releaseUnopened() {}
    }

    public static final int TRACK_WIDTH = 120;
    public static final int TRACK_HEIGHT = 4;
    public static final int KNOB_WIDTH = 3;
    public static final int KNOB_HEIGHT = 10;
    private static final int TOP_GAP = 16;
    private static final int LINE_GAP = 3;
    private static final int END_GAP = 4;
    private static final HudBar TRACK = HudBar.of("slider", TRACK_WIDTH, TRACK_HEIGHT);
    private static final ResourceLocation KNOB = Guzhenren.id("hud/slider_knob");
    private static final Component CANCEL = Component.translatable("guzhenren.hud.slider.cancel");

    private static final class State {

        @Nullable Owner owner;
        int top = -1;
        int heldTicks;
        boolean spent;
        double cursorX;
    }

    private static final State STATE = new State();

    private HoldSlider() {}

    //region the hold [长按]
    public static void hold(@NotNull Owner next) {
        if (STATE.owner != null || RadialMenu.isHolding()) return;

        STATE.owner = next;
        STATE.top = -1;
        STATE.heldTicks = 0;
        STATE.spent = false;
    }

    public static boolean isHolding() { return STATE.owner != null; }

    public static boolean isOpen() { return STATE.top > 0; }

    public static void close() {
        STATE.owner = null;
        STATE.top = -1;
    }

    public static int getValue() {
        if (!isOpen()) return 0;
        return Math.min(STATE.top, (int) (STATE.cursorX / TRACK_WIDTH * (STATE.top + 1)));
    }

    public static void steer(double dx) {
        Window window = Minecraft.getInstance().getWindow();
        STATE.cursorX = Mth.clamp(STATE.cursorX + dx * window.getGuiScaledWidth() / window.getScreenWidth(), 0.0D,
                TRACK_WIDTH);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Owner current = STATE.owner;
        if (current == null) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            close();
            return;
        }
        if (!current.isHeld()) {
            release(current);
            return;
        }
        if (!STATE.spent && STATE.top < 0 && ++STATE.heldTicks >= RadialMenu.OPEN_DELAY_TICKS) open(current);
    }

    private static void open(Owner current) {
        int read = current.top();
        if (read <= 0) {
            STATE.spent = true;
            current.empty();
            return;
        }
        STATE.top = read;
        STATE.cursorX = 1.5D * TRACK_WIDTH / (read + 1);
    }

    private static void release(Owner current) {
        boolean opened = isOpen();
        int value = getValue();
        close();
        if (!opened) {
            if (!STATE.spent) current.releaseUnopened();
            return;
        }
        if (value > 0) current.pick(value);
    }

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!isHolding()) return;

        event.setCanceled(true);
        event.setSwingHand(false);
    }

    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        if (isHolding()) event.setCanceled(true);
    }
    //endregion

    //region drawing [绘制]
    public static void render(@NotNull GuiGraphics graphics) {
        Owner current = STATE.owner;
        if (current == null || !isOpen()) return;

        Font font = Minecraft.getInstance().font;
        int value = getValue();
        int left = (graphics.guiWidth() - TRACK_WIDTH) / 2;
        int labelY = graphics.guiHeight() / 2 + TOP_GAP;
        int trackY = labelY + font.lineHeight + LINE_GAP + (KNOB_HEIGHT - TRACK_HEIGHT) / 2;
        int detailY = trackY + TRACK_HEIGHT + (KNOB_HEIGHT - TRACK_HEIGHT) / 2 + LINE_GAP;

        Component label = value > 0 ? current.label(value) : CANCEL;
        graphics.drawCenteredString(font, label, graphics.guiWidth() / 2, labelY, ModPalette.TEXT);

        int knob = left + Math.round((float) value / STATE.top * (TRACK_WIDTH - KNOB_WIDTH));
        int knobY = trackY - (KNOB_HEIGHT - TRACK_HEIGHT) / 2;
        SpriteBatch.draw(graphics, sprites -> {
            TRACK.draw(sprites, left, trackY, knob - left, GuItem.CHARGE_COLOR_DEFAULT);
            sprites.blit(KNOB, knob, knobY, KNOB_WIDTH, KNOB_HEIGHT);
        });

        int endY = trackY + TRACK_HEIGHT / 2 - font.lineHeight / 2;
        graphics.drawString(font, CANCEL, left - END_GAP - font.width(CANCEL), endY,
                value == 0 ? ModPalette.TEXT : ModPalette.LEGEND_TEXT, true);
        graphics.drawString(font, String.valueOf(STATE.top), left + TRACK_WIDTH + END_GAP, endY,
                value == STATE.top ? ModPalette.TEXT : ModPalette.LEGEND_TEXT, true);
        graphics.drawCenteredString(font, current.detail(STATE.top), graphics.guiWidth() / 2, detailY,
                ModPalette.LEGEND_TEXT);
    }
    //endregion
}
